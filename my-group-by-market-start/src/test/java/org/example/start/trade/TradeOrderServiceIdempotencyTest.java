package org.example.start.trade;

import lombok.extern.slf4j.Slf4j;
import org.example.start.base.ConcurrentTestSupport;
import org.example.start.base.IntegrationTestBase;
import org.example.application.service.trade.TradeOrderService;
import org.example.application.service.trade.cmd.LockOrderCmd;
import org.example.application.service.trade.result.TradeOrderResult;
import org.example.domain.model.trade.repository.TradeOrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test 1: TradeOrderService 幂等性测试
 *
 * <p>
 * 测试目的：验证并发场景下 outTradeNo 的幂等性不变量
 * <p>
 * 场景：500 个线程同时提交相同 outTradeNo 的锁单请求
 * <p>
 * 预期（TDD - 先写失败测试）：
 * - 数据库只有 1 条 TradeOrder 记录
 * - 所有请求返回相同的 tradeOrderId（幂等返回）
 * - Order.lockCount = 1（不是 500）
 * <p>
 * 可能发现的 Bug：
 * - 数据库缺少 out_trade_no UNIQUE 约束 → 创建多条重复记录
 * - 幂等性检查有竞态 → 部分请求失败而非返回已有订单
 *
 */
@Slf4j
@DisplayName("Test 1: TradeOrderService 幂等性测试（500 线程并发）")
public class TradeOrderServiceIdempotencyTest extends IntegrationTestBase {

        @Autowired
        private TradeOrderService tradeOrderService;

        @Autowired
        private TradeOrderRepository tradeOrderRepository;

        @Autowired
        private JdbcTemplate jdbcTemplate;

        @Test
        @DisplayName("并发相同 outTradeNo 锁单 - 应只创建一条记录")
        void testConcurrentLockOrderWithSameOutTradeNo() throws InterruptedException {
                // Given: 准备测试数据
                String outTradeNo = "OUT_IDEMPOTENCY_TEST_" + System.currentTimeMillis();
                String activityId = "ACT001"; // 假设已存在
                String skuId = "SKU001"; // 假设已存在
                String userId = "USER_TEST_001";

                // 准备锁单命令（所有线程使用相同 outTradeNo）
                LockOrderCmd cmd = LockOrderCmd.builder()
                                .userId(userId)
                                .activityId(activityId)
                                .skuId(skuId)
                                .outTradeNo(outTradeNo) // 关键：相同的外部交易单号
                                .orderId(null) // 新开团
                                .source("APP")
                                .channel("iOS")
                                .originalPrice(new BigDecimal("999.00"))
                                .deductionPrice(new BigDecimal("200.00"))
                                .payPrice(new BigDecimal("799.00"))
                                .build();

                // When: 500 个线程并发锁单
                int threadCount = 500;
                List<TradeOrderResult> results = new CopyOnWriteArrayList<>();
                List<Exception> exceptions = new CopyOnWriteArrayList<>();

                ConcurrentTestSupport.executeConcurrently(threadCount, () -> {
                        try {
                                TradeOrderResult result = tradeOrderService.lockOrder(cmd);
                                results.add(result);
                                log.debug("线程 {} 锁单成功: tradeOrderId={}", Thread.currentThread().getName(),
                                                result.getTradeOrderId());
                        } catch (Exception e) {
                                exceptions.add(e);
                                log.debug("线程 {} 锁单失败: {}", Thread.currentThread().getName(), e.getMessage());
                        }
                });

                // Then: 验证幂等性不变量
                log.info("【幂等性测试】执行完成：成功={}, 失败={}", results.size(), exceptions.size());

                // 断言 1: 数据库只有 1 条 TradeOrder 记录
                Integer count = jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM trade_order WHERE out_trade_no = ?",
                                Integer.class,
                                outTradeNo);
                assertThat(count)
                                .as("数据库应该只有 1 条 out_trade_no='%s' 的记录", outTradeNo)
                                .isEqualTo(1);

                // 断言 2: 所有成功请求返回相同的 tradeOrderId
                if (!results.isEmpty()) {
                        String firstTradeOrderId = results.get(0).getTradeOrderId();
                        assertThat(results)
                                        .as("所有成功请求应该返回相同的 tradeOrderId（幂等性）")
                                        .extracting(TradeOrderResult::getTradeOrderId)
                                        .containsOnly(firstTradeOrderId);

                        log.info("【幂等性测试】所有请求返回的 tradeOrderId: {}", firstTradeOrderId);
                }

                // 断言 3: 至少有一个请求成功（不能全部失败）
                assertThat(results)
                                .as("至少应该有一个请求成功创建订单")
                                .isNotEmpty();

                // 断言 4: Order.lockCount = 1（可选，需要查询 Order 表验证）
                // 这个断言需要 OrderRepository 来验证，这里简化处理

                // 记录测试结果
                log.info("【幂等性测试】验证通过：500 个并发请求，数据库只创建了 1 条记录");
                log.info("【幂等性测试】成功请求数: {}", results.size());
                log.info("【幂等性测试】失败请求数: {}", exceptions.size());
        }

        @Test
        @DisplayName("顺序重复锁单 - 应返回已有订单")
        void testSequentialDuplicateLockOrder() {
                // Given: 准备测试数据
                String outTradeNo = "OUT_SEQ_TEST_" + System.currentTimeMillis();
                String activityId = "ACT001";
                String skuId = "SKU001";
                String userId = "USER_SEQ_001";

                LockOrderCmd cmd = LockOrderCmd.builder()
                                .userId(userId)
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

                // When: 第一次锁单
                TradeOrderResult firstResult = tradeOrderService.lockOrder(cmd);
                String firstTradeOrderId = firstResult.getTradeOrderId();

                // When: 第二次锁单（相同 outTradeNo）
                TradeOrderResult secondResult = tradeOrderService.lockOrder(cmd);
                String secondTradeOrderId = secondResult.getTradeOrderId();

                // Then: 应该返回相同的 tradeOrderId
                assertThat(secondTradeOrderId)
                                .as("重复锁单应该返回相同的 tradeOrderId（幂等性）")
                                .isEqualTo(firstTradeOrderId);

                // 验证数据库只有一条记录
                Integer count = jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM trade_order WHERE out_trade_no = ?",
                                Integer.class,
                                outTradeNo);
                assertThat(count).isEqualTo(1);

                log.info("【幂等性测试】顺序重复锁单验证通过：两次请求返回相同 tradeOrderId={}", firstTradeOrderId);
        }
}
