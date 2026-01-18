package org.example.start.e2e;

import lombok.extern.slf4j.Slf4j;
import org.example.application.service.payment.PaymentCallbackApplicationService;
import org.example.application.service.trade.TradeOrderService;
import org.example.application.service.trade.cmd.LockOrderCmd;
import org.example.application.service.trade.result.TradeOrderResult;
import org.example.common.exception.BizException;
import org.example.infrastructure.cache.IRedisService;
import org.example.start.base.ConcurrentTestSupport;
import org.example.start.base.IntegrationTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E Test 2: 并发支付结算测试
 *
 * <p>
 * 测试目的：验证全链路（锁单 -> 支付 -> 结算）在并发场景下的正确性
 * <p>
 * 场景：20个用户同时抢购一个5人团，抢到后立即支付
 * <p>
 * 验证点：
 * <ul>
 * <li>Lock阶段：只有5个用户锁单成功</li>
 * <li>Pay阶段：5个成功用户全部支付成功</li>
 * <li>Settle阶段：Order状态变为SUCCESS，completeCount=5</li>
 * <li>TradeOrder状态：5个SETTLED，15个(或失败数)无非法状态</li>
 * <li>数据一致性：Redis槽位、DB记录、库存完全一致</li>
 * </ul>
 */
@Slf4j
@DisplayName("E2E Test 2: 并发支付结算测试")
public class ConcurrentPaymentSettlementTest extends IntegrationTestBase {

    @Autowired
    private TradeOrderService tradeOrderService;

    @Autowired
    private PaymentCallbackApplicationService paymentCallbackApplicationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private IRedisService redisService;

    private String testOrderId;
    private String testActivityId;
    private String testSkuId;
    private String testSpuId;
    private String testTeamId;

    @BeforeEach
    void setUpTestData() {
        // 准备测试数据
        testActivityId = "ACT_E2E2_" + System.currentTimeMillis();
        testOrderId = "ORD_E2E2_" + System.currentTimeMillis();
        testSkuId = "SKU_E2E2_" + System.currentTimeMillis();
        testSpuId = "SPU_E2E2_" + System.currentTimeMillis();
        testTeamId = "TEAM_E2E2_" + System.currentTimeMillis();

        // 0. 创建测试折扣 (满100减20)
        String discountId = "DIS_E2E2_" + System.currentTimeMillis();
        jdbcTemplate.update(
                "INSERT INTO discount (discount_id, discount_name, discount_desc, discount_amount, discount_type, " +
                        "market_plan, market_expr, tag_id, create_time, update_time) " +
                        "VALUES (?, 'E2E2折扣', '满100减20', 20.00, 'DIRECT', 'ZJ', '20', NULL, NOW(), NOW())",
                discountId);

        // 1. 创建测试活动（5人团） - 使用新创建的折扣
        jdbcTemplate.update(
                "INSERT INTO activity (activity_id, activity_name, activity_desc, discount_id, tag_id, tag_scope, " +
                        "group_type, target, valid_time, participation_limit, start_time, end_time, status) " +
                        "VALUES (?, 'E2E2测试活动', '20人抢5人团', ?, NULL, 'OPEN', 0, 5, 1800, 1, " +
                        "'2026-01-01 00:00:00', '2026-12-31 23:59:59', 'ACTIVE')",
                testActivityId, discountId);

        // 2. 创建测试SPU
        jdbcTemplate.update(
                "INSERT INTO spu (spu_id, spu_name, category_id, brand, description, status) " +
                        "VALUES (?, 'E2E2商品', 'CAT001', 'TestBrand', 'E2E2测试', 'ON_SALE')",
                testSpuId);

        // 3. 创建测试SKU（库存充足，100件）
        jdbcTemplate.update(
                "INSERT INTO sku (sku_id, spu_id, goods_name, stock, frozen_stock, original_price, status) " +
                        "VALUES (?, ?, 'E2E2商品SKU', 100, 0, 100.00, 'ON_SALE')",
                testSkuId, testSpuId);

        // 4. 创建活动商品关联
        jdbcTemplate.update(
                "INSERT INTO activity_goods (activity_id, spu_id, source, channel) " +
                        "VALUES (?, ?, 's01', 'c01')",
                testActivityId, testSpuId);

        // 5. 创建Order（5人团，初始状态）
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = now.plusMinutes(30);
        jdbcTemplate.update(
                "INSERT INTO `order` (order_id, activity_id, spu_id, leader_user_id, status, target_count, " +
                        "lock_count, complete_count, original_price, deduction_price, pay_amount, " +
                        "start_time, deadline_time, create_time, update_time, team_id) " +
                        "VALUES (?, ?, ?, 'LEADER_USER', 'PENDING', 5, 0, 0, 100.00, 20.00, 0.00, " +
                        "?, ?, ?, ?, ?)",
                testOrderId, testActivityId, testSpuId, now, deadline, now, now, testTeamId);

        // 6. 为20个用户创建Account记录
        for (int i = 0; i < 20; i++) {
            String userId = "USER_E2E2_" + i;
            String accountId = "ACC_E2E2_" + i;
            jdbcTemplate.update(
                    "INSERT INTO account (account_id, user_id, activity_id, participation_count, version) " +
                            "VALUES (?, ?, ?, 0, 1)",
                    accountId, userId, testActivityId);
        }

        log.info("【测试准备】E2E2测试数据创建完成");
    }

    @Test
    @DisplayName("全链路并发测试：Lock -> Pay -> Settle")
    void testConcurrentLockAndPay() throws InterruptedException {
        // Given: 20个并发用户，5个名额
        int totalUsers = 20;
        int targetSlots = 5;
        BigDecimal payAmount = new BigDecimal("80.00");

        AtomicInteger lockSuccessCount = new AtomicInteger(0);
        AtomicInteger paySuccessCount = new AtomicInteger(0);
        List<Exception> exceptions = new CopyOnWriteArrayList<>();
        AtomicInteger threadIndexCounter = new AtomicInteger(0);

        // When: 并发执行 Lock -> Pay
        ConcurrentTestSupport.executeConcurrently(totalUsers, () -> {
            int userIndex = threadIndexCounter.getAndIncrement();
            String userId = "USER_E2E2_" + userIndex;
            String outTradeNo = "OUT_E2E2_" + System.currentTimeMillis() + "_" + userIndex;

            try {
                // Step 1: 锁单
                LockOrderCmd cmd = LockOrderCmd.builder()
                        .userId(userId)
                        .activityId(testActivityId)
                        .skuId(testSkuId)
                        .outTradeNo(outTradeNo)
                        .orderId(testOrderId)
                        .source("APP")
                        .channel("iOS")
                        .originalPrice(new BigDecimal("100.00"))
                        .deductionPrice(new BigDecimal("20.00"))
                        .payPrice(payAmount)
                        .build();

                TradeOrderResult result = tradeOrderService.lockOrder(cmd);
                lockSuccessCount.incrementAndGet();
                log.info("【锁单成功】用户: {}, TradeOrderId: {}", userId, result.getTradeOrderId());

                // Step 2: 立即支付
                // 模拟一点点处理延迟，增加随机性
                Thread.sleep((long) (Math.random() * 50));

                paymentCallbackApplicationService.handlePaymentSuccess(outTradeNo, payAmount);
                paySuccessCount.incrementAndGet();
                log.info("【支付成功】用户: {}, outTradeNo: {}", userId, outTradeNo);

            } catch (BizException e) {
                // 预期内的业务异常（如名额已满）
                log.info("【业务拦截】用户: {}, 原因: {}", userId, e.getMessage());
            } catch (Exception e) {
                exceptions.add(e);
                log.error("【系统异常】用户: {}", userId, e);
            }
        }, 60);

        // 等待异步Settlement完成 (Event Listener)
        Thread.sleep(2000);

        // Then: 验证结果
        log.info("========== E2E2 测试结果验证 ==========");
        log.info("Lock成功数: {}, Pay成功数: {}", lockSuccessCount.get(), paySuccessCount.get());

        // 验证 1: 锁单数量
        assertThat(lockSuccessCount.get())
                .as("只有5个用户应该锁单成功")
                .isEqualTo(targetSlots);

        // 验证 2: 支付数量
        assertThat(paySuccessCount.get())
                .as("所有锁单成功的用户都应该支付成功")
                .isEqualTo(targetSlots);

        // 验证 3: Order状态
        Map<String, Object> orderMap = jdbcTemplate.queryForMap(
                "SELECT status, complete_count, lock_count FROM `order` WHERE order_id = ?",
                testOrderId);

        assertThat(orderMap.get("status"))
                .as("Order最终状态应为SUCCESS")
                .isEqualTo("SUCCESS"); // 数据库存储的是枚举字符串

        assertThat(((Number) orderMap.get("complete_count")).intValue())
                .as("Order完成人数应为5")
                .isEqualTo(targetSlots);

        // 验证 4: TradeOrder状态
        List<Map<String, Object>> tradeOrders = jdbcTemplate.queryForList(
                "SELECT status FROM trade_order WHERE order_id = ?",
                testOrderId);

        long settledCount = tradeOrders.stream()
                .filter(row -> "SETTLED".equals(row.get("status")))
                .count();

        assertThat(settledCount)
                .as("应该有5个SETTLED状态的TradeOrder")
                .isEqualTo(targetSlots);

        assertThat(tradeOrders.size())
                .as("数据库中应只有5条TradeOrder记录")
                .isEqualTo(targetSlots);

        // 验证 5: 库存和Redis
        String slotKey = "team_slot:" + testOrderId + ":available";
        Long finalSlot = redisService.getAtomicLong(slotKey);
        assertThat(finalSlot).as("Redis槽位应归零").isEqualTo(0);

        Integer frozenStock = jdbcTemplate.queryForObject(
                "SELECT frozen_stock FROM sku WHERE sku_id = ?", Integer.class, testSkuId);
        // settlement逻辑只是更改状态，frozen_stock应为5，发货阶段才扣减库存
        assertThat(frozenStock).as("冻结库存应保留为5").isEqualTo(5);

        log.info("Test Passed! All verifications successful.");
    }

    @AfterEach
    void tearDown() {
        if (testOrderId != null) {
            jdbcTemplate.update("DELETE FROM trade_order WHERE order_id = ?", testOrderId);
            jdbcTemplate.update("DELETE FROM account WHERE activity_id = ?", testActivityId);
            jdbcTemplate.update("DELETE FROM `order` WHERE order_id = ?", testOrderId);
            jdbcTemplate.update("DELETE FROM activity_goods WHERE activity_id = ?", testActivityId);
            jdbcTemplate.update("DELETE FROM sku WHERE spu_id = ?", testSpuId);
            jdbcTemplate.update("DELETE FROM spu WHERE spu_id = ?", testSpuId);
            jdbcTemplate.update("DELETE FROM activity WHERE activity_id = ?", testActivityId);

            String slotKey = "team_slot:" + testOrderId + ":available";
            redisService.delete(slotKey);
        }
    }
}
