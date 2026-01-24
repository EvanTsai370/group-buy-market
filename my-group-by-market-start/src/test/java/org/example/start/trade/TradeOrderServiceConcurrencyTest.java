package org.example.start.trade;

import lombok.extern.slf4j.Slf4j;
import org.example.start.base.ConcurrentTestSupport;
import org.example.start.base.IntegrationTestBase;
import org.example.application.service.trade.TradeOrderService;
import org.example.application.service.trade.cmd.LockOrderCmd;
import org.example.application.service.trade.result.TradeOrderResult;
import org.example.domain.model.account.Account;
import org.example.domain.model.account.repository.AccountRepository;
import org.example.domain.model.trade.repository.TradeOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test 2: Account乐观锁冲突下的资源回滚测试
 *
 * <p>
 * 测试场景：Account 乐观锁冲突下的资源回滚
 * <p>
 * 设置：
 * - Account: participationCount=9, limit=10（还剩1次）
 * <p>
 * 场景：
 * <ol>
 * <li>模拟：2 个线程同时锁单（同一用户同一活动）</li>
 * <li>预期：一个成功，另一个因 version 冲突失败</li>
 * <li>关键：失败的请求已占用 Redis slot 和 inventory</li>
 * </ol>
 * <p>
 * 验证：
 * <ul>
 * <li>成功线程：Redis available 减 1，DB frozen_stock 加 1</li>
 * <li>失败线程：Redis available 恢复，DB frozen_stock 恢复</li>
 * <li>Account.participationCount 只增加 1</li>
 * </ul>
 * <p>
 * 可能发现的 Bug：
 * <ul>
 * <li>失败请求未调用 rollbackResources() → Redis 槽位泄漏</li>
 * <li>catch 块只捕获 BizException，OptimisticLockException 未捕获</li>
 * </ul>
 *
 */
@Slf4j
@DisplayName("Test 2: Account 乐观锁冲突下的资源回滚测试")
public class TradeOrderServiceConcurrencyTest extends IntegrationTestBase {

    @Autowired
    private TradeOrderService tradeOrderService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TradeOrderRepository tradeOrderRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private String testUserId;
    private String testAccountId;

    @BeforeEach
    void setUpTestAccount() {
        // 准备测试用户（使用不同的用户ID避免与Test 1冲突）
        testUserId = "USER_CONCURRENCY_" + System.currentTimeMillis();
        testAccountId = "ACC_CONCURRENCY_" + System.currentTimeMillis();

        // 创建测试账户：participationCount=9, limit=10（还剩1次）
        // ACT001 的 participation_limit 是 10，所以设置为 9 才能留下 1 次机会
        jdbcTemplate.update(
                "INSERT INTO account (account_id, user_id, activity_id, participation_count, version) " +
                        "VALUES (?, ?, 'ACT001', 9, 1)",
                testAccountId, testUserId);

        log.info("【测试准备】创建测试账户, accountId: {}, userId: {}, participationCount: 9, limit: 10",
                testAccountId, testUserId);
    }

    @Test
    @DisplayName("并发锁单导致Account乐观锁冲突 - 应正确回滚资源")
    void testAccountOptimisticLockWithResourceRollback() throws InterruptedException {
        // Given: 准备测试数据
        String activityId = "ACT001";
        String skuId = "SKU001";

        // 准备2个不同的 outTradeNo（避免幂等性干扰）
        String outTradeNo1 = "OUT_CONCURRENCY_1_" + System.currentTimeMillis();
        String outTradeNo2 = "OUT_CONCURRENCY_2_" + System.currentTimeMillis() + "_SUFFIX";

        LockOrderCmd cmd1 = LockOrderCmd.builder()
                .userId(testUserId)
                .activityId(activityId)
                .skuId(skuId)
                .outTradeNo(outTradeNo1)
                .orderId(null) // 新开团
                .source("APP")
                .channel("iOS")
                .originalPrice(new BigDecimal("999.00"))
                .deductionPrice(new BigDecimal("200.00"))
                .payPrice(new BigDecimal("799.00"))
                .build();

        LockOrderCmd cmd2 = LockOrderCmd.builder()
                .userId(testUserId)
                .activityId(activityId)
                .skuId(skuId)
                .outTradeNo(outTradeNo2)
                .orderId(null) // 新开团
                .source("APP")
                .channel("iOS")
                .originalPrice(new BigDecimal("999.00"))
                .deductionPrice(new BigDecimal("200.00"))
                .payPrice(new BigDecimal("799.00"))
                .build();

        // 查询初始库存
        Integer initialFrozenStock = jdbcTemplate.queryForObject(
                "SELECT frozen_stock FROM sku WHERE sku_id = ?",
                Integer.class,
                skuId);
        log.info("【测试准备】初始冻结库存: {}", initialFrozenStock);

        // When: 2个线程并发锁单（同一用户，不同outTradeNo）
        List<TradeOrderResult> results = new CopyOnWriteArrayList<>();
        List<Exception> exceptions = new CopyOnWriteArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        Thread thread1 = new Thread(() -> {
            try {
                TradeOrderResult result = tradeOrderService.lockOrder(cmd1);
                results.add(result);
                successCount.incrementAndGet();
                log.info("【线程1】锁单成功: tradeOrderId={}", result.getTradeOrderId());
            } catch (Exception e) {
                exceptions.add(e);
                failureCount.incrementAndGet();
                log.warn("【线程1】锁单失败: {}", e.getMessage());
            }
        });

        Thread thread2 = new Thread(() -> {
            try {
                TradeOrderResult result = tradeOrderService.lockOrder(cmd2);
                results.add(result);
                successCount.incrementAndGet();
                log.info("【线程2】锁单成功: tradeOrderId={}", result.getTradeOrderId());
            } catch (Exception e) {
                exceptions.add(e);
                failureCount.incrementAndGet();
                log.warn("【线程2】锁单失败: {}", e.getMessage());
            }
        });

        // 启动两个线程
        thread1.start();
        thread2.start();

        // 等待两个线程完成
        thread1.join(10000); // 最多等待10秒
        thread2.join(10000);

        // Then: 验证结果
        log.info("【测试结果】成功: {}, 失败: {}", successCount.get(), failureCount.get());

        // 断言1: 由于participationLimit=10，participationCount=9，只能再参加1次
        // 所以两个线程中只有一个能成功
        assertThat(successCount.get())
                .as("应该只有1个请求成功（因为只剩1次参团机会）")
                .isEqualTo(1);

        assertThat(failureCount.get())
                .as("应该有1个请求失败")
                .isEqualTo(1);

        // 断言2: Account.participationCount 应该只增加1次
        Integer finalParticipationCount = jdbcTemplate.queryForObject(
                "SELECT participation_count FROM account WHERE account_id = ?",
                Integer.class,
                testAccountId);
        assertThat(finalParticipationCount)
                .as("Account 参团次数应该只增加1次（从9到10）")
                .isEqualTo(10);

        // 断言3: 冻结库存应该只增加1（成功的那个订单）
        Integer finalFrozenStock = jdbcTemplate.queryForObject(
                "SELECT frozen_stock FROM sku WHERE sku_id = ?",
                Integer.class,
                skuId);
        assertThat(finalFrozenStock)
                .as("冻结库存应该只增加1（失败的请求应该回滚）")
                .isEqualTo(initialFrozenStock + 1);

        // 断言4: 数据库应该只有1条成功的 TradeOrder 记录
        Integer tradeOrderCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM trade_order WHERE user_id = ?",
                Integer.class,
                testUserId);
        assertThat(tradeOrderCount)
                .as("数据库应该只有1条成功的 TradeOrder 记录")
                .isEqualTo(1);

        // 断言5: 检查 Redis team_slot 是否正确
        // 注意：由于成功的请求创建了Order，需要查询orderId
        if (!results.isEmpty()) {
            String successOrderId = results.get(0).getOrderId();
            String slotKey = "team_slot:" + successOrderId + ":available";
            Object availableSlots = redisTemplate.opsForValue().get(slotKey);

            log.info("【Redis检查】orderId: {}, slotKey: {}, available: {}",
                    successOrderId, slotKey, availableSlots);

            // 成功的订单应该占用了1个槽位（5人团，已用1个，剩4个）
            if (availableSlots != null) {
                assertThat(Integer.parseInt(availableSlots.toString()))
                        .as("Redis team_slot 应该减少1个（失败的请求应该回滚）")
                        .isEqualTo(4); // 5人团，已用1个，剩4个
            }
        }

        log.info("【测试通过】Account乐观锁冲突下的资源回滚验证成功");
        log.info("【测试详情】初始冻结库存: {}, 最终冻结库存: {}, 增加: {}",
                initialFrozenStock, finalFrozenStock, finalFrozenStock - initialFrozenStock);
    }

    @Test
    @DisplayName("高并发数据一致性测试 - 10个线程竞争10个名额")
    void testHighConcurrencyWithLimitedSlots() throws InterruptedException {
        // Given: 准备新的测试用户（participationCount=0, limit=10）
        //
        // 测试目标：验证高并发场景下的数据一致性
        // 在乐观锁机制下，可能的结果：
        // 1. 极端高并发：1个成功，9个失败（所有线程同时读取version=1）
        // 2. 中等并发：2-9个成功（部分线程读到更新后的version）
        // 3. 低并发/串行：10个全部成功（每个线程读到最新version）
        //
        // 核心验证：无论成功多少个，必须保证：
        // - participationCount == 成功数
        // - frozen_stock增量 == 成功数
        // - trade_order记录数 == 成功数
        String concurrentUserId = "USER_HIGH_CONCURRENT_" + System.currentTimeMillis();
        String concurrentAccountId = "ACC_HIGH_CONCURRENT_" + System.currentTimeMillis();

        jdbcTemplate.update(
                "INSERT INTO account (account_id, user_id, activity_id, participation_count, version) " +
                        "VALUES (?, ?, 'ACT001', 0, 1)",
                concurrentAccountId, concurrentUserId);

        String activityId = "ACT001";
        String skuId = "SKU001";

        // 准备10个不同的 outTradeNo
        List<LockOrderCmd> commands = new CopyOnWriteArrayList<>();
        for (int i = 0; i < 10; i++) {
            String outTradeNo = "OUT_HIGH_CONCURRENT_" + System.currentTimeMillis() + "_" + i;
            LockOrderCmd cmd = LockOrderCmd.builder()
                    .userId(concurrentUserId)
                    .activityId(activityId)
                    .skuId(skuId)
                    .outTradeNo(outTradeNo)
                    .orderId(null)
                    .source("APP")
                    .channel("iOS")
                    .originalPrice(new BigDecimal("999.00"))
                    .deductionPrice(new BigDecimal("200.00"))
                    .payPrice(new BigDecimal("799.00"))
                    .build();
            commands.add(cmd);
        }

        // 查询初始库存
        Integer initialFrozenStock = jdbcTemplate.queryForObject(
                "SELECT frozen_stock FROM sku WHERE sku_id = ?",
                Integer.class,
                skuId);

        // When: 10个线程并发锁单
        int threadCount = 10;
        List<TradeOrderResult> results = new CopyOnWriteArrayList<>();
        List<Exception> exceptions = new CopyOnWriteArrayList<>();

        ConcurrentTestSupport.executeConcurrently(threadCount, () -> {
            int index = results.size() + exceptions.size();
            if (index < commands.size()) {
                LockOrderCmd cmd = commands.get(index);
                try {
                    TradeOrderResult result = tradeOrderService.lockOrder(cmd);
                    results.add(result);
                    log.debug("线程 {} 锁单成功: tradeOrderId={}",
                            Thread.currentThread().getName(), result.getTradeOrderId());
                } catch (Exception e) {
                    exceptions.add(e);
                    log.debug("线程 {} 锁单失败: {}",
                            Thread.currentThread().getName(), e.getMessage());
                }
            }
        });

        // Then: 验证结果
        log.info("【高并发测试】成功: {}, 失败: {}", results.size(), exceptions.size());

        // 断言：成功的请求数量应该 <= 10（participationLimit）
        assertThat(results.size())
                .as("成功的请求数量应该 <= 10")
                .isLessThanOrEqualTo(10);

        // 断言: Account.participationCount 应该等于成功的请求数
        Integer finalParticipationCount = jdbcTemplate.queryForObject(
                "SELECT participation_count FROM account WHERE account_id = ?",
                Integer.class,
                concurrentAccountId);
        assertThat(finalParticipationCount)
                .as("Account 参团次数应该等于成功的请求数")
                .isEqualTo(results.size());

        // 断言: 冻结库存增加量应该等于成功的请求数
        Integer finalFrozenStock = jdbcTemplate.queryForObject(
                "SELECT frozen_stock FROM sku WHERE sku_id = ?",
                Integer.class,
                skuId);
        assertThat(finalFrozenStock)
                .as("冻结库存增加量应该等于成功的请求数")
                .isEqualTo(initialFrozenStock + results.size());

        log.info("【高并发测试通过】10个线程并发，成功: {}, 失败: {}, 资源回滚正确",
                results.size(), exceptions.size());
    }
}
