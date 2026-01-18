package org.example.start.e2e;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.example.application.service.trade.TradeOrderService;
import org.example.application.service.trade.cmd.LockOrderCmd;
import org.example.common.exception.BizException;
import org.example.domain.model.account.Account;
import org.example.domain.model.account.repository.AccountRepository;
import org.example.infrastructure.cache.IRedisService;
import org.example.start.base.ConcurrentTestSupport;
import org.example.start.base.IntegrationTestBase;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import org.example.common.cache.RedisKeyManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 端到端测试：混沌工程与系统弹性验证
 *
 * <p>
 * 测试目标：
 * 验证系统在随机基础设施故障（如数据库短暂不可用）下的数据一致性和自我恢复能力。
 *
 * <p>
 * 测试场景：
 * <ol>
 * <li>50 个并发用户争抢 5 个活动名额。</li>
 * <li>启用混沌故障注入（Chaos Monkey），以 10% 的概率随机抛出数据库异常。</li>
 * <li>验证核心不变式：Redis 剩余名额 + 数据库成功订单数 == 总名额。</li>
 * <li>验证资源无泄漏，即使在事务回滚发生时。</li>
 * </ol>
 *
 * @author 开发团队
 * @since 2026-01-18
 */
@Slf4j
@Import(ChaosEngineeringTest.ChaosConfig.class)
public class ChaosEngineeringTest extends IntegrationTestBase {

    @Autowired
    private TradeOrderService tradeOrderService;

    @Autowired
    private IRedisService redisService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AccountRepository accountRepository;

    private String testActivityId;
    private String testSkuId;
    private String testSpuId;
    private String commonOrderId;
    private int targetSlots = 5;

    @BeforeEach
    public void setUp() {
        // 清理数据
        jdbcTemplate.execute("DELETE FROM trade_order");
        jdbcTemplate.execute("DELETE FROM `order`");
        jdbcTemplate.execute("DELETE FROM account");
        jdbcTemplate.execute("DELETE FROM activity");
        jdbcTemplate.execute("DELETE FROM sku");
        jdbcTemplate.execute("DELETE FROM spu");
        jdbcTemplate.execute("DELETE FROM discount");

        testActivityId = "ACT_CHAOS_" + System.currentTimeMillis();
        testSkuId = "SKU_CHAOS_" + System.currentTimeMillis();

        // 0. 创建折扣配置 (ZJ)
        String discountId = "DIS_CHAOS_" + System.currentTimeMillis();
        jdbcTemplate.update(
                "INSERT INTO discount (discount_id, discount_name, discount_desc, discount_amount, discount_type, " +
                        "market_plan, market_expr, tag_id, create_time, update_time) " +
                        "VALUES (?, 'ChaosDiscount', 'Direct -20', 20.00, 'DIRECT', 'ZJ', '20', NULL, NOW(), NOW())",
                discountId);

        // 1. 创建活动 (目标人数=5)
        jdbcTemplate.update(
                "INSERT INTO activity (activity_id, activity_name, activity_desc, discount_id, tag_id, tag_scope, " +
                        "group_type, target, valid_time, participation_limit, start_time, end_time, status) " +
                        "VALUES (?, 'Chaos Activity', 'Chaos Test', ?, NULL, 'OPEN', 0, ?, 1800, 1, " +
                        "'2026-01-01 00:00:00', '2026-12-31 23:59:59', 'ACTIVE')",
                testActivityId, discountId, targetSlots);

        // 2. 创建 SPU 和 SKU
        testSpuId = "SPU_CHAOS_" + System.currentTimeMillis();

        jdbcTemplate.update("INSERT INTO spu (spu_id, spu_name, category_id, brand, description, status) " +
                "VALUES (?, 'Chaos SPU', 'CAT_TEST', 'Brand', 'Desc', 'ON_SALE')", testSpuId);

        jdbcTemplate.update(
                "INSERT INTO sku (sku_id, spu_id, goods_name, original_price, stock, frozen_stock, create_time, update_time, status) "
                        +
                        "VALUES (?, ?, 'Chaos Goods', 100.00, 100, 0, NOW(), NOW(), 'ON_SALE')",
                testSkuId, testSpuId);
    }

    @AfterEach
    public void tearDown() {
        ChaosContext.disable(); // 确保关闭混沌模式
        jdbcTemplate.execute("DELETE FROM trade_order");
        jdbcTemplate.execute("DELETE FROM `order`");
        jdbcTemplate.execute("DELETE FROM account");
        jdbcTemplate.execute("DELETE FROM activity");
        jdbcTemplate.execute("DELETE FROM sku");
        jdbcTemplate.execute("DELETE FROM spu");
        jdbcTemplate.execute("DELETE FROM discount");
        if (commonOrderId != null) {
            redisService.delete(RedisKeyManager.teamSlotAvailableKey(commonOrderId));
            redisService.delete(RedisKeyManager.teamSlotLockedKey(commonOrderId));

            // 清理分布式锁
            redisService.delete(RedisKeyManager.lockKey("resource-release", commonOrderId));
            for (int i = 0; i < 50; i++) {
                String userId = "USER_CHAOS_" + i;
                redisService.delete(RedisKeyManager.lockKey("resource-release", commonOrderId + ":" + userId));
            }
        }
    }

    @Test
    public void testConcurrentLockWithChaos() throws InterruptedException {
        // 1. 先创建一个公共的 Order，确保所有用户加入同一个团 (目标=5)
        commonOrderId = "ORD_CHAOS_COMMON_" + System.currentTimeMillis();
        String teamId = "TEAM_" + System.currentTimeMillis();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = now.plusHours(1); // 使用 Java 时间匹配应用逻辑（避免时区不匹配）
        jdbcTemplate.update(
                "INSERT INTO `order` (order_id, team_id, activity_id, spu_id, leader_user_id, target_count, complete_count, lock_count, "
                        +
                        "status, original_price, deduction_price, pay_amount, start_time, deadline_time, create_time, update_time) "
                        +
                        "VALUES (?, ?, ?, ?, 'LEADER', ?, 0, 0, 'PENDING', 100.00, 20.00, 0.00, ?, ?, NOW(), NOW())",
                commonOrderId, teamId, testActivityId, testSpuId, targetSlots, now, deadline);

        // 创建 50 个用户账户
        int threadCount = 50;
        for (int i = 0; i < threadCount; i++) {
            String userId = "USER_CHAOS_" + i;
            String accountId = "ACC_CHAOS_" + i;
            jdbcTemplate.update(
                    "INSERT INTO account (account_id, user_id, activity_id, participation_count, version) VALUES (?, ?, ?, 0, 1)",
                    accountId, userId, testActivityId);
        }

        // Init Redis Slot // 4. Redis Key (正确使用RedisKeyManager生成Key)
        // 注意：TradeOrderRepositoryImpl 中使用 RedisKeyManager.teamSlotAvailableKey(orderId)
        // 即 team_slot:{orderId}:available
        String slotAvailableKey = RedisKeyManager.teamSlotAvailableKey(commonOrderId);
        // 使用 setNx 避免覆盖（虽然 keys 是新的）
        redisService.setNx(slotAvailableKey, targetSlots, 3600, TimeUnit.SECONDS);

        // 初始化 locked key 为 0
        String slotLockedKey = RedisKeyManager.teamSlotLockedKey(commonOrderId);
        redisService.setNx(slotLockedKey, 0, 3600, TimeUnit.SECONDS);

        // 验证：检查账户是否已创建
        Integer accountCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM account WHERE activity_id = ?", Integer.class, testActivityId);
        log.info("Verified seeded accounts count: {}", accountCount);
        if (accountCount == null || accountCount != threadCount) {
            throw new RuntimeException("Accounts not seeded correctly! Found: " + accountCount);
        }

        // 2. 启用混沌模式 (10% 故障率)
        ChaosContext.enable(0.1);

        // 3. 执行 50 个并发请求
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger chaosFailureCount = new AtomicInteger(0);
        AtomicInteger userIndex = new AtomicInteger(0);

        ConcurrentTestSupport.executeConcurrently(threadCount, () -> {
            int idx = userIndex.getAndIncrement();
            String userId = "USER_CHAOS_" + idx;
            LockOrderCmd cmd = getLockOrderCmd(userId);

            // 调试：检查 Repository 是否能找到用户
            Optional<Account> debugAccount = accountRepository.findByUserAndActivity(userId, testActivityId);
            if (debugAccount.isEmpty()) {
                log.error("GenericTest: AccountRepository cannot find user {} activity {}!", userId, testActivityId);
            } else {
                log.info("GenericTest: AccountRepository FOUND user {}", userId);
            }

            try {
                tradeOrderService.lockOrder(cmd);
                successCount.incrementAndGet();
            } catch (Exception e) {
                // 如果是我们的混沌异常，计数
                if (e.getMessage() != null && (e.getMessage().contains("Chaos DB Injection")
                        || e.getMessage().contains("TransientDataAccessException"))) {
                    chaosFailureCount.incrementAndGet();
                    log.info("Client caught chaos exception for user {}", userId);
                } else {
                    // 可能是合法的 "拼团已满" 或 "库存不足"
                    log.info("Client caught business exception: {}", e.getMessage());
                }
            }
        });

        // 4. 验证一致性
        String verifySlotKey = RedisKeyManager.teamSlotAvailableKey(commonOrderId); // 重新计算 Key
        long redisAvailable = redisService.getAtomicLong(verifySlotKey);

        // 统计 DB 中实际成功的记录数
        Integer dbSuccessCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM trade_order WHERE order_id = ?", Integer.class, commonOrderId);

        // 检查库存
        Integer frozenStock = jdbcTemplate.queryForObject(
                "SELECT frozen_stock FROM sku WHERE sku_id = ?", Integer.class, testSkuId);

        log.info("========== Chaos Test Results ==========");
        log.info("Attempted: {}", threadCount);
        log.info("Client Reported Success: {}", successCount.get());
        log.info("Client Reported Chaos Failures: {}", chaosFailureCount.get());
        log.info("DB Actual Success: {}", dbSuccessCount);
        log.info("Redis Available: {}", redisAvailable);
        log.info("Frozen Stock: {}", frozenStock);

        // 不变式 1: 总名额 (5) = Redis 剩余名额 + DB 成功数
        // 即使发生异常，资源也应该被回滚
        assertThat(redisAvailable + dbSuccessCount).as("Redis Slot + DB Success should equal Target")
                .isEqualTo(targetSlots);

        // 不变式 2: 冻结库存应该匹配 DB 成功数
        assertThat(frozenStock).as("Frozen Stock should match DB Success").isEqualTo(dbSuccessCount);

        // 不变式 3: DB 成功数应该匹配客户端报告的成功数 (如果没有事务问题)
        assertThat(dbSuccessCount).as("DB Success should match Client Success").isEqualTo(successCount.get());
    }

    private @NotNull LockOrderCmd getLockOrderCmd(String userId) {
        LockOrderCmd cmd = new LockOrderCmd();
        cmd.setUserId(userId);
        cmd.setActivityId(testActivityId);
        cmd.setSkuId(testSkuId);
        cmd.setOrderId(commonOrderId); // 所有用户加入同一个团
        cmd.setOutTradeNo("OUT_" + userId);
        cmd.setOriginalPrice(new BigDecimal("100.00"));
        cmd.setDeductionPrice(new BigDecimal("20.00"));
        cmd.setPayPrice(new BigDecimal("80.00"));
        cmd.setSource("APP");
        cmd.setChannel("IOS");
        return cmd;
    }

    // ================= Chaos Infrastructure =================

    public static class ChaosContext {
        private static volatile boolean enabled = false;
        private static volatile double failureRate = 0.0;

        public static void enable(double rate) {
            enabled = true;
            failureRate = rate;
        }

        public static void disable() {
            enabled = false;
        }

        public static boolean shouldFail() {
            return enabled && Math.random() < failureRate;
        }
    }

    @TestConfiguration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    public static class ChaosConfig {

        @Bean
        public ChaosAspect chaosAspect() {
            return new ChaosAspect();
        }
    }

    @Aspect
    @Slf4j
    public static class ChaosAspect {

        @Around("execution(* org.example.domain.model..repository.*Repository.save(..)) || " +
                "execution(* org.example.domain.model..repository.*Repository.update*(..)) || " +
                "execution(* org.example.domain.model..repository.*Repository.insert*(..))")
        public Object injectChaos(ProceedingJoinPoint joinPoint) throws Throwable {
            if (ChaosContext.shouldFail()) {
                log.warn("😈 Chaos Monkey injected failure into {}", joinPoint.getSignature().toShortString());
                throw new TransientDataAccessException("Chaos DB Injection: " + joinPoint.getSignature().getName()) {
                };
            }
            return joinPoint.proceed();
        }
    }
}
