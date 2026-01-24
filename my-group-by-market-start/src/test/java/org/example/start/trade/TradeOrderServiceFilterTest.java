package org.example.start.trade;

import lombok.extern.slf4j.Slf4j;
import org.example.application.service.trade.TradeOrderService;
import org.example.application.service.trade.cmd.LockOrderCmd;
import org.example.common.exception.BizException;
import org.example.start.base.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test 4: Filter链部分失败的回滚测试
 *
 * <p>
 * 测试场景：InventoryOccupyHandler失败时TeamSlotOccupyHandler的回滚
 * <p>
 * 设置：
 * - 创建一个已有的Order（确保TeamSlotOccupyHandler会执行）
 * - 创建库存耗尽的SKU（frozen_stock = stock，确保InventoryOccupyHandler失败）
 * <p>
 * 场景：
 * <ol>
 * <li>用户加入已有拼团（orderId不为空）</li>
 * <li>TeamSlotOccupyHandler 成功占用Redis槽位（DECR）</li>
 * <li>InventoryOccupyHandler 尝试冻结库存 → 返回0（库存不足） → 返回reject</li>
 * <li>TradeOrderService 应该调用rollbackResources()</li>
 * </ol>
 * <p>
 * 验证：
 * <ul>
 * <li>Redis team_slot:{orderId}:available 回到初始值（未减少）</li>
 * <li>抛出 BizException，错误信息包含 "商品库存不足"</li>
 * <li>数据库 frozen_stock 保持不变</li>
 * <li>没有新的 TradeOrder 记录被创建</li>
 * </ul>
 * <p>
 * 可能发现的 Bug：
 * <ul>
 * <li>context.recoveryTeamSlotKey 未设置 → 回滚跳过</li>
 * <li>rollbackResources() 未调用 → 槽位泄漏</li>
 * <li>Filter reject响应未被正确处理 → 继续执行锁单逻辑</li>
 * </ul>
 *
 */
@Slf4j
@DisplayName("Test 4: Filter链部分失败的回滚测试")
public class TradeOrderServiceFilterTest extends IntegrationTestBase {

        @Autowired
        private TradeOrderService tradeOrderService;

        @Autowired
        private JdbcTemplate jdbcTemplate;

        @Autowired
        private RedisTemplate<String, Object> redisTemplate;

        private String testUserId;
        private String testAccountId;
        private String testOrderId;
        private String testSkuId;

        @BeforeEach
        void setUpTestData() {
                // 准备测试用户
                testUserId = "USER_FILTER_" + System.currentTimeMillis();
                testAccountId = "ACC_FILTER_" + System.currentTimeMillis();
                testOrderId = "ORD_FILTER_" + System.currentTimeMillis();
                testSkuId = "SKU_FILTER_" + System.currentTimeMillis();

                // 1. 创建测试账户（participationCount=0, limit=10）
                jdbcTemplate.update(
                                "INSERT INTO account (account_id, user_id, activity_id, participation_count, version) "
                                                +
                                                "VALUES (?, ?, 'ACT001', 0, 1)",
                                testAccountId, testUserId);

                // 2. 创建库存耗尽的SKU（available_stock = 0）
                jdbcTemplate.update(
                                "INSERT INTO sku (sku_id, spu_id, goods_name, stock, frozen_stock, original_price, status) "
                                                +
                                                "VALUES (?, 'SPU001', 'Test SKU - Out of Stock', 100, 100, 999.00, 'ON_SALE')",
                                testSkuId);

                // 3. 创建一个已存在的Order（确保orderId不为空，触发TeamSlotOccupyHandler）
                jdbcTemplate.update(
                                "INSERT INTO `order` (order_id, activity_id, spu_id, leader_user_id, status, target_count, "
                                                +
                                                "lock_count, complete_count, original_price, deduction_price, pay_amount, "
                                                +
                                                "start_time, deadline_time, create_time, update_time) " +
                                                "VALUES (?, 'ACT001', 'SPU001', ?, 'PENDING', 5, 0, 0, 999.00, 200.00, 0.00, "
                                                +
                                                "NOW(), DATE_ADD(NOW(), INTERVAL 30 MINUTE), NOW(), NOW())",
                                testOrderId, testUserId);

                log.info("【测试准备】创建测试数据 - userId: {}, skuId: {}, orderId: {}, frozen_stock: 100/100",
                                testUserId, testSkuId, testOrderId);
        }

        @Test
        @DisplayName("库存不足导致锁单失败 - 应正确回滚Redis槽位")
        void testInventoryOccupyFailure_ShouldRollbackTeamSlot() {
                // Given: 准备锁单命令（加入已有拼团）
                String outTradeNo = "OUT_FILTER_" + System.currentTimeMillis();
                LockOrderCmd cmd = LockOrderCmd.builder()
                                .userId(testUserId)
                                .activityId("ACT001")
                                .skuId(testSkuId)
                                .outTradeNo(outTradeNo)
                                .orderId(testOrderId) // 加入已有拼团（触发TeamSlotOccupyHandler）
                                .source("APP")
                                .channel("iOS")
                                .originalPrice(new BigDecimal("999.00"))
                                .deductionPrice(new BigDecimal("200.00"))
                                .payPrice(new BigDecimal("799.00"))
                                .build();

                // 记录初始状态
                String slotKey = "team_slot:" + testOrderId + ":available";

                // 初始化Redis槽位（模拟第一个用户加入时的状态）
                // 5人团，已有0人，可用5个名额
                redisTemplate.opsForValue().set(slotKey, 5);

                Object initialAvailable = redisTemplate.opsForValue().get(slotKey);
                log.info("【测试前】Redis槽位 - key: {}, available: {}", slotKey, initialAvailable);

                Integer initialFrozenStock = jdbcTemplate.queryForObject(
                                "SELECT frozen_stock FROM sku WHERE sku_id = ?",
                                Integer.class,
                                testSkuId);
                log.info("【测试前】库存状态 - skuId: {}, frozen_stock: {}", testSkuId, initialFrozenStock);

                // When: 执行锁单（预期失败：库存不足）
                // Then: 验证抛出异常，错误信息包含 "商品库存不足"
                assertThatThrownBy(() -> tradeOrderService.lockOrder(cmd))
                                .isInstanceOf(BizException.class)
                                .hasMessageContaining("商品库存不足");

                log.info("【测试通过】锁单失败，抛出预期异常：商品库存不足");

                // 验证1: Redis槽位应该回到初始值（回滚成功）
                Object finalAvailable = redisTemplate.opsForValue().get(slotKey);
                log.info("【测试后】Redis槽位 - key: {}, available: {}", slotKey, finalAvailable);

                assertThat(finalAvailable)
                                .as("Redis槽位应该回滚到初始值（TeamSlotOccupyHandler的占用应该被释放）")
                                .isEqualTo(initialAvailable);

                // 验证2: 库存应该保持不变（因为库存冻结从未成功）
                Integer finalFrozenStock = jdbcTemplate.queryForObject(
                                "SELECT frozen_stock FROM sku WHERE sku_id = ?",
                                Integer.class,
                                testSkuId);

                assertThat(finalFrozenStock)
                                .as("frozen_stock应该保持不变（InventoryOccupyHandler未成功冻结）")
                                .isEqualTo(initialFrozenStock);

                // 验证3: 没有新的TradeOrder记录被创建
                Integer tradeOrderCount = jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM trade_order WHERE user_id = ? AND out_trade_no = ?",
                                Integer.class,
                                testUserId,
                                outTradeNo);

                assertThat(tradeOrderCount)
                                .as("不应该创建新的TradeOrder记录（锁单失败）")
                                .isEqualTo(0);

                // 验证4: Account.participationCount 应该保持不变（未扣减）
                Integer participationCount = jdbcTemplate.queryForObject(
                                "SELECT participation_count FROM account WHERE account_id = ?",
                                Integer.class,
                                testAccountId);

                assertThat(participationCount)
                                .as("Account参团次数应该保持不变（锁单失败）")
                                .isEqualTo(0);

                log.info("【测试通过】Filter链回滚验证成功");
                log.info("【验证结果】Redis槽位: {} → {} (回滚成功)", initialAvailable, finalAvailable);
                log.info("【验证结果】frozen_stock: {} → {} (保持不变)", initialFrozenStock, finalFrozenStock);
                log.info("【验证结果】TradeOrder记录数: 0 (未创建)");
                log.info("【验证结果】participationCount: 0 (未扣减)");
        }
}
