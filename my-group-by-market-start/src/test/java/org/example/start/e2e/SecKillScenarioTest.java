package org.example.start.e2e;

import lombok.extern.slf4j.Slf4j;
import org.example.application.service.trade.TradeOrderService;
import org.example.application.service.trade.cmd.LockOrderCmd;
import org.example.application.service.trade.result.TradeOrderResult;
import org.example.common.exception.BizException;
import org.example.start.base.ConcurrentTestSupport;
import org.example.start.base.IntegrationTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.example.infrastructure.cache.IRedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.time.LocalDateTime;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E Test 1: 秒杀场景 - 500用户抢5人团
 *
 * <p>
 * 测试目的：验证在极端高并发场景下，系统的资源控制机制能否正确防止超卖
 * <p>
 * 场景：500个不同用户同时抢购一个5人团的拼团活动
 * <p>
 * 验证点：
 * <ul>
 * <li>只有5个TradeOrder创建成功</li>
 * <li>Order.lockCount = 5（不是500）</li>
 * <li>Redis槽位 available = 0（5个名额全部占用）</li>
 * <li>冻结库存 frozen_stock = 5（不超卖）</li>
 * <li>成功用户的Account.participationCount正确扣减</li>
 * <li>失败用户的资源正确回滚（无泄漏）</li>
 * </ul>
 * <p>
 * 关键业务不变量：
 * <ul>
 * <li>lockCount ≤ targetCount（防止超卖）</li>
 * <li>Redis槽位 + DB记录 = targetCount（资源一致性）</li>
 * <li>frozen_stock = 成功订单数（库存一致性）</li>
 * </ul>
 * <p>
 * 可能发现的Bug：
 * <ul>
 * <li>Redis DECR 非原子操作 → 超卖</li>
 * <li>Order.lockCount 更新无WHERE条件 → 超过targetCount</li>
 * <li>失败请求资源未回滚 → 槽位/库存泄漏</li>
 * <li>Account乐观锁冲突未处理 → 参团次数不一致</li>
 * </ul>
 *
 */
@Slf4j
@DisplayName("E2E Test 1: 秒杀场景 - 500用户抢5人团")
public class SecKillScenarioTest extends IntegrationTestBase {

        @Autowired
        private TradeOrderService tradeOrderService;

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
        void setUpSecKillTestData() {
                // 准备测试数据
                testActivityId = "ACT_SECKILL_" + System.currentTimeMillis();
                testOrderId = "ORD_SECKILL_" + System.currentTimeMillis();
                testSkuId = "SKU_SECKILL_" + System.currentTimeMillis();
                testSpuId = "SPU_SECKILL_" + System.currentTimeMillis();
                testTeamId = "TEAM_SECKILL_" + System.currentTimeMillis();

                // 1. 创建测试活动（5人团，每人最多参与1次，防止重复参与）
                jdbcTemplate.update(
                                "INSERT INTO activity (activity_id, activity_name, activity_desc, discount_id, tag_id, tag_scope, "
                                                +
                                                "group_type, target, valid_time, participation_limit, start_time, end_time, status) "
                                                +
                                                "VALUES (?, '秒杀测试活动', '500人抢5人团', 'DIS001', NULL, 'OPEN', 0, 5, 1800, 1, "
                                                +
                                                "'2026-01-01 00:00:00', '2026-12-31 23:59:59', 'ACTIVE')",
                                testActivityId);

                // 2. 创建测试SPU
                jdbcTemplate.update(
                                "INSERT INTO spu (spu_id, spu_name, category_id, brand, description, status) " +
                                                "VALUES (?, '秒杀商品', 'CAT001', 'TestBrand', '秒杀测试', 'ON_SALE')",
                                testSpuId);

                // 3. 创建测试SKU（库存充足，1000件）
                jdbcTemplate.update(
                                "INSERT INTO sku (sku_id, spu_id, goods_name, stock, frozen_stock, original_price, status) "
                                                +
                                                "VALUES (?, ?, '秒杀商品SKU', 1000, 0, 999.00, 'ON_SALE')",
                                testSkuId, testSpuId);

                // 4. 创建活动商品关联
                jdbcTemplate.update(
                                "INSERT INTO activity_goods (activity_id, spu_id, source, channel) " +
                                                "VALUES (?, ?, 's01', 'c01')",
                                testActivityId, testSpuId);

                // 5. 创建Order（5人团，初始lockCount=0）
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime deadline = now.plusMinutes(30);
                jdbcTemplate.update(
                                "INSERT INTO `order` (order_id, activity_id, spu_id, leader_user_id, status, target_count, "
                                                +
                                                "lock_count, complete_count, original_price, deduction_price, pay_amount, "
                                                +
                                                "start_time, deadline_time, create_time, update_time, team_id) " +
                                                "VALUES (?, ?, ?, 'LEADER_USER', 'PENDING', 5, 0, 0, 999.00, 200.00, 0.00, "
                                                +
                                                "?, ?, ?, ?, ?)",
                                testOrderId, testActivityId, testSpuId, now, deadline, now, now, testTeamId);

                String deadlineTime = jdbcTemplate.queryForObject(
                                "select deadline_time from `order` where order_id = ?", String.class, testOrderId);
                log.debug("deadlineTime: {}", deadlineTime);

                // 注意：不需要手动初始化Redis槽位，decrWithInit()的Lua脚本会原子性地初始化
                // 如果手动初始化，会导致Lua脚本跳过初始化步骤，直接DECR，造成竞态条件

                // 7. 为500个用户创建Account记录（participationCount=0, limit=1）
                for (int i = 0; i < 500; i++) {
                        String userId = "USER_SECKILL_" + i;
                        String accountId = "ACC_SECKILL_" + i;
                        jdbcTemplate.update(
                                        "INSERT INTO account (account_id, user_id, activity_id, participation_count, version) "
                                                        +
                                                        "VALUES (?, ?, ?, 0, 1)",
                                        accountId, userId, testActivityId);
                }

                log.info("【测试准备】秒杀场景测试数据创建完成");
                log.info("【测试准备】activityId: {}, orderId: {}, skuId: {}", testActivityId, testOrderId, testSkuId);
                log.info("【测试准备】目标人数: 5, 初始lockCount: 0, Redis槽位: 5");
                log.info("【测试准备】已创建500个测试用户账户");
        }

        @Test
        @DisplayName("500并发抢5人团 - 应只有5个成功，资源无泄漏")
        void testSecKillScenario_500UsersFor5Slots() throws InterruptedException {
                // Given: 准备并发测试数据
                int totalUsers = 500;
                int targetSlots = 5;

                AtomicInteger successCount = new AtomicInteger(0);
                AtomicInteger failureCount = new AtomicInteger(0);
                List<String> successfulTradeOrderIds = new CopyOnWriteArrayList<>();
                List<String> successfulUserIds = new CopyOnWriteArrayList<>();
                List<Exception> exceptions = new CopyOnWriteArrayList<>();

                // 记录初始状态
                String slotKey = "team_slot:" + testOrderId + ":available";
                Long initialSlot = redisService.getAtomicLong(slotKey);
                Integer initialFrozenStock = jdbcTemplate.queryForObject(
                                "SELECT frozen_stock FROM sku WHERE sku_id = ?",
                                Integer.class,
                                testSkuId);
                Integer initialLockCount = jdbcTemplate.queryForObject(
                                "SELECT lock_count FROM `order` WHERE order_id = ?",
                                Integer.class,
                                testOrderId);

                log.info("【测试前】初始状态 - Redis槽位: {}, frozen_stock: {}, lockCount: {}",
                                initialSlot, initialFrozenStock, initialLockCount);

                AtomicInteger threadIndexCounter = new AtomicInteger(0);

                // When: 500个用户并发抢购
                ConcurrentTestSupport.executeConcurrently(totalUsers, () -> {
                        int userIndex = threadIndexCounter.getAndIncrement();
                        String userId = "USER_SECKILL_" + userIndex;
                        String outTradeNo = "OUT_SECKILL_" + System.currentTimeMillis() + "_" + userIndex;

                        LockOrderCmd cmd = LockOrderCmd.builder()
                                        .userId(userId)
                                        .activityId(testActivityId)
                                        .skuId(testSkuId)
                                        .outTradeNo(outTradeNo)
                                        .orderId(testOrderId) // 加入已有拼团
                                        .source("APP")
                                        .channel("iOS")
                                        .originalPrice(new BigDecimal("999.00"))
                                        .deductionPrice(new BigDecimal("200.00"))
                                        .payPrice(new BigDecimal("799.00"))
                                        .build();

                        try {
                                TradeOrderResult result = tradeOrderService.lockOrder(cmd);
                                successCount.incrementAndGet();
                                successfulTradeOrderIds.add(result.getTradeOrderId());
                                successfulUserIds.add(userId);
                                log.debug("【秒杀成功】用户 {} 抢购成功，tradeOrderId: {}", userId, result.getTradeOrderId());
                        } catch (BizException e) {
                                failureCount.incrementAndGet();
                                exceptions.add(e);
                                log.debug("【秒杀失败】用户 {} 抢购失败: {}", userId, e.getMessage());
                        } catch (Exception e) {
                                failureCount.incrementAndGet();
                                exceptions.add(e);
                                log.error("【秒杀异常】用户 {} 发生异常", userId, e);
                        }
                }, 120); // 120秒超时

                // Then: 验证结果
                log.info("【秒杀结束】总请求: {}, 成功: {}, 失败: {}", totalUsers, successCount.get(), failureCount.get());

                // ========== 核心验证1: 只有5个用户成功 ==========
                assertThat(successCount.get())
                                .as("【核心不变量】只有5个用户应该抢购成功（防止超卖）")
                                .isEqualTo(targetSlots);

                assertThat(failureCount.get())
                                .as("应该有495个用户抢购失败")
                                .isEqualTo(totalUsers - targetSlots);

                // ========== 核心验证2: Order.lockCount = 5 ==========
                Integer finalLockCount = jdbcTemplate.queryForObject(
                                "SELECT lock_count FROM `order` WHERE order_id = ?",
                                Integer.class,
                                testOrderId);

                assertThat(finalLockCount)
                                .as("【核心不变量】Order.lockCount应该等于5（原子更新正确）")
                                .isEqualTo(targetSlots);

                // ========== 核心验证3: Redis槽位 = 0 ==========
                Long finalSlot = redisService.getAtomicLong(slotKey);
                assertThat(finalSlot)
                                .as("【核心不变量】Redis槽位应该为0（5个名额全部占用）")
                                .isEqualTo(0);

                // ========== 核心验证4: 数据库TradeOrder记录 = 5 ==========
                Integer dbTradeOrderCount = jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM trade_order WHERE order_id = ?",
                                Integer.class,
                                testOrderId);

                assertThat(dbTradeOrderCount)
                                .as("【核心不变量】数据库应该只有5条TradeOrder记录")
                                .isEqualTo(targetSlots);

                // ========== 核心验证5: 冻结库存 = 5 ==========
                Integer finalFrozenStock = jdbcTemplate.queryForObject(
                                "SELECT frozen_stock FROM sku WHERE sku_id = ?",
                                Integer.class,
                                testSkuId);

                assertThat(finalFrozenStock)
                                .as("【核心不变量】冻结库存应该等于5（库存一致性）")
                                .isEqualTo(targetSlots);

                // ========== 核心验证6: 成功用户的participationCount = 1 ==========
                for (String userId : successfulUserIds) {
                        Integer participationCount = jdbcTemplate.queryForObject(
                                        "SELECT participation_count FROM account WHERE user_id = ? AND activity_id = ?",
                                        Integer.class,
                                        userId, testActivityId);

                        assertThat(participationCount)
                                        .as("成功用户 %s 的participationCount应该为1", userId)
                                        .isEqualTo(1);
                }

                // ========== 核心验证7: 失败用户的participationCount = 0（资源回滚） ==========
                Integer failedUsersWithZeroCount = jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM account WHERE activity_id = ? AND participation_count = 0",
                                Integer.class,
                                testActivityId);

                assertThat(failedUsersWithZeroCount)
                                .as("【资源回滚验证】应该有495个用户的participationCount保持为0（失败请求资源正确回滚）")
                                .isEqualTo(totalUsers - targetSlots);

                // ========== 数据一致性验证 ==========
                // 验证：Redis槽位消耗 + 数据库记录 = targetCount
                // 如果initialSlot为null，说明是首次初始化，逻辑初始值为targetSlots
                Long initialSlotValue = initialSlot != null ? initialSlot : (long) targetSlots;
                Long finalSlotValue = java.util.Objects.requireNonNull(finalSlot, "最终Redis槽位不应为null");

                long redisConsumed = initialSlotValue - finalSlotValue;
                assertThat(redisConsumed)
                                .as("【数据一致性】Redis槽位消耗应该等于数据库记录数")
                                .isEqualTo(dbTradeOrderCount.longValue());

                // ========== 输出测试报告 ==========
                log.info("========== 秒杀场景测试报告 ==========");
                log.info("【并发规模】总请求: {}, 成功: {}, 失败: {}", totalUsers, successCount.get(), failureCount.get());
                log.info("【资源状态】Redis槽位: {} → {}", initialSlotValue, finalSlot);
                log.info("【资源状态】Order.lockCount: {} → {}", initialLockCount, finalLockCount);
                log.info("【资源状态】frozen_stock: {} → {}", initialFrozenStock, finalFrozenStock);
                log.info("【数据库记录】TradeOrder记录数: {}", dbTradeOrderCount);
                log.info("【资源回滚】失败用户中participationCount=0的数量: {}", failedUsersWithZeroCount);
                log.info("【核心不变量】 lockCount ≤ targetCount: {} ≤ 5", finalLockCount);
                log.info("【核心不变量】 Redis槽位消耗 = DB记录: {} = {}", redisConsumed, dbTradeOrderCount);
                log.info("【核心不变量】 frozen_stock = 成功订单数: {} = {}", finalFrozenStock, successCount.get());
                log.info("【测试结论】 秒杀场景测试通过 - 系统在极端并发下正确防止超卖，资源无泄漏");
                log.info("=====================================");

                // ========== 异常分析（可选） ==========
                if (!exceptions.isEmpty()) {
                        log.info("【异常分析】失败请求异常类型统计:");
                        exceptions.stream()
                                        .map(e -> e.getClass().getSimpleName() + ": " + e.getMessage())
                                        .distinct()
                                        .forEach(msg -> log.info("  - {}", msg));
                }
        }

        @Test
        @DisplayName("秒杀场景 - 验证失败原因分布")
        void testSecKillScenario_FailureReasonDistribution() throws InterruptedException {
                // Given: 准备并发测试数据
                int totalUsers = 500;

                AtomicInteger slotExhaustedCount = new AtomicInteger(0); // Redis槽位耗尽
                AtomicInteger inventoryExhaustedCount = new AtomicInteger(0); // 库存不足
                AtomicInteger participationLimitCount = new AtomicInteger(0); // 参团次数超限
                AtomicInteger optimisticLockCount = new AtomicInteger(0); // 乐观锁冲突
                AtomicInteger otherErrorCount = new AtomicInteger(0); // 其他错误
                AtomicInteger successCount = new AtomicInteger(0);

                AtomicInteger threadIndexCounter = new AtomicInteger(0);

                // When: 500个用户并发抢购
                ConcurrentTestSupport.executeConcurrently(totalUsers, () -> {
                        int userIndex = threadIndexCounter.getAndIncrement();
                        String userId = "USER_SECKILL_" + userIndex;
                        String outTradeNo = "OUT_SECKILL_REASON_" + System.currentTimeMillis() + "_" + userIndex;

                        LockOrderCmd cmd = LockOrderCmd.builder()
                                        .userId(userId)
                                        .activityId(testActivityId)
                                        .skuId(testSkuId)
                                        .outTradeNo(outTradeNo)
                                        .orderId(testOrderId)
                                        .source("APP")
                                        .channel("iOS")
                                        .originalPrice(new BigDecimal("999.00"))
                                        .deductionPrice(new BigDecimal("200.00"))
                                        .payPrice(new BigDecimal("799.00"))
                                        .build();

                        try {
                                tradeOrderService.lockOrder(cmd);
                                successCount.incrementAndGet();
                        } catch (BizException e) {
                                String message = e.getMessage();
                                if (message.contains("名额已满") || message.contains("槽位")) {
                                        slotExhaustedCount.incrementAndGet();
                                } else if (message.contains("库存不足")) {
                                        inventoryExhaustedCount.incrementAndGet();
                                } else if (message.contains("参团次数") || message.contains("超过限制")) {
                                        participationLimitCount.incrementAndGet();
                                } else {
                                        otherErrorCount.incrementAndGet();
                                }
                        } catch (org.springframework.dao.OptimisticLockingFailureException e) {
                                optimisticLockCount.incrementAndGet();
                        } catch (Exception e) {
                                otherErrorCount.incrementAndGet();
                                log.error("【未预期异常】{}: {}", e.getClass().getSimpleName(), e.getMessage());
                        }
                }, 120);

                // Then: 输出失败原因分布
                log.info("========== 秒杀失败原因分布 ==========");
                log.info("【成功】: {} 个用户", successCount.get());
                log.info("【失败 - Redis槽位耗尽】: {} 个用户", slotExhaustedCount.get());
                log.info("【失败 - 库存不足】: {} 个用户", inventoryExhaustedCount.get());
                log.info("【失败 - 参团次数超限】: {} 个用户", participationLimitCount.get());
                log.info("【失败 - 乐观锁冲突】: {} 个用户", optimisticLockCount.get());
                log.info("【失败 - 其他错误】: {} 个用户", otherErrorCount.get());
                log.info("【总计】: {} 个用户", totalUsers);
                log.info("=====================================");

                // 验证：成功数量应该是5
                assertThat(successCount.get())
                                .as("应该有5个用户成功")
                                .isEqualTo(5);

                // 验证：失败总数应该是495
                int totalFailures = slotExhaustedCount.get() + inventoryExhaustedCount.get()
                                + participationLimitCount.get() + optimisticLockCount.get() + otherErrorCount.get();
                assertThat(totalFailures)
                                .as("应该有495个用户失败")
                                .isEqualTo(495);
        }

        @AfterEach
        void tearDownSecKillTestData() {
                if (testOrderId != null) {
                        try {
                                // 1. 清理数据库数据 (按依赖反向清理)
                                jdbcTemplate.update("DELETE FROM trade_order WHERE order_id = ?", testOrderId);
                                jdbcTemplate.update("DELETE FROM account WHERE activity_id = ?", testActivityId);
                                jdbcTemplate.update("DELETE FROM `order` WHERE order_id = ?", testOrderId);
                                jdbcTemplate.update("DELETE FROM activity_goods WHERE activity_id = ?", testActivityId);
                                jdbcTemplate.update("DELETE FROM sku WHERE spu_id = ?", testSpuId);
                                jdbcTemplate.update("DELETE FROM spu WHERE spu_id = ?", testSpuId);
                                jdbcTemplate.update("DELETE FROM activity WHERE activity_id = ?", testActivityId);

                                // 2. 清理Redis缓存
                                String slotKey = "team_slot:" + testOrderId + ":available";
                                redisService.delete(slotKey);

                                log.info("【测试清理】已清理测试数据: orderId={}, activityId={}", testOrderId, testActivityId);
                        } catch (Exception e) {
                                log.error("【测试清理】清理测试数据失败", e);
                        }
                }
        }

}
