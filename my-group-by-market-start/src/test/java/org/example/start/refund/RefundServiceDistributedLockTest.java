package org.example.start.refund;

import lombok.extern.slf4j.Slf4j;
import org.example.domain.gateway.IPaymentRefundGateway;
import org.example.domain.model.order.Order;
import org.example.domain.model.order.repository.OrderRepository;
import org.example.domain.model.order.valueobject.Money;
import org.example.domain.model.trade.TradeOrder;
import org.example.domain.model.trade.repository.TradeOrderRepository;
import org.example.domain.model.trade.valueobject.TradeStatus;
import org.example.domain.service.RefundService;
import org.example.start.base.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

/**
 * RefundService 分布式锁超时测试
 *
 * <p>
 * Test 8: 分布式锁超时场景
 *
 * <p>
 * 目的：验证锁过期时的保护机制
 *
 * <p>
 * 场景：
 * <ol>
 * <li>Mock PaymentGateway.refund() 耗时 35 秒（超过 30s 锁）</li>
 * <li>同时发起两个退款请求</li>
 * </ol>
 *
 * <p>
 * 验证：
 * <ul>
 * <li>第一个请求获取锁，执行退款</li>
 * <li>锁在 30s 时过期</li>
 * <li>第二个请求获取到锁（不应该！）</li>
 * <li>检查支付网关是否被调用 2 次</li>
 * </ul>
 *
 * <p>
 * 可能发现的 Bug：
 * <ul>
 * <li>锁 TTL 不足 → 重复退款</li>
 * <li>PaidRefundStrategy 执行时间未考虑锁超时</li>
 * </ul>
 *
 * @author 测试团队
 * @since 2026-01-16
 */
@Slf4j
public class RefundServiceDistributedLockTest extends IntegrationTestBase {

    @Autowired
    private RefundService refundService;

    @Autowired
    private TradeOrderRepository tradeOrderRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private org.example.domain.model.goods.repository.SkuRepository skuRepository;

    @MockBean
    private IPaymentRefundGateway paymentRefundGateway;

    private String testTradeOrderId;
    private String testOrderId;
    private AtomicInteger gatewayCallCount;

    @BeforeEach
    void setUp() {
        // 初始化计数器
        gatewayCallCount = new AtomicInteger(0);

        // 创建测试数据：Order + 已支付的 TradeOrder
        testOrderId = "TEST-ORDER-" + System.currentTimeMillis();
        testTradeOrderId = "TEST-TRADE-" + System.currentTimeMillis();
        createTestData();
    }

    /**
     * Test 8: 分布式锁超时场景
     *
     * <p>
     * 验证：当退款操作耗时超过锁TTL时，是否会导致重复退款
     */
    @Test
    void testDistributedLockTimeout_shouldPreventDuplicateRefund() throws InterruptedException {
        log.info("========== Test 8: 分布式锁超时场景 ==========");

        // 1. Mock PaymentGateway.refund() 耗时 35 秒
        mockSlowRefund(35);

        // 2. 准备并发测试
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // 3. 启动两个并发线程
        for (int i = 0; i < 2; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    startLatch.await(); // 等待统一开始

                    log.info("【线程{}】开始退款请求", threadId);
                    refundService.refundTradeOrder(testTradeOrderId, "测试退款-线程" + threadId);

                    successCount.incrementAndGet();
                    log.info("【线程{}】退款成功", threadId);

                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    log.warn("【线程{}】退款失败: {}", threadId, e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        // 4. 同时触发所有线程
        startLatch.countDown();

        // 5. 等待所有线程完成（最多等待 80 秒，因为可能有两次 35 秒的调用）
        boolean finished = doneLatch.await(80, TimeUnit.SECONDS);
        assertThat(finished).isTrue();

        // 6. 验证结果
        log.info("========== 测试结果 ==========");
        log.info("成功次数: {}", successCount.get());
        log.info("失败次数: {}", failureCount.get());
        log.info("支付网关调用次数: {}", gatewayCallCount.get());

        // 7. 断言
        // 理想情况：只有一个请求成功，另一个被锁阻塞或幂等性拦截
        // Bug 场景：如果锁过期，可能两个都成功，导致支付网关被调用 2 次
        if (gatewayCallCount.get() > 1) {
            log.error("【Bug 发现】支付网关被调用了 {} 次，存在重复退款风险！", gatewayCallCount.get());
            assertThat(gatewayCallCount.get())
                    .as("支付网关应该只被调用 1 次，但实际被调用了 %d 次，说明锁 TTL 不足导致重复退款",
                            gatewayCallCount.get())
                    .isEqualTo(1);
        } else {
            log.info("【测试通过】支付网关只被调用 1 次，锁机制有效");
            assertThat(gatewayCallCount.get()).isEqualTo(1);
        }

        // 8. 验证最终状态
        Optional<TradeOrder> tradeOrderOpt = tradeOrderRepository.findByTradeOrderId(testTradeOrderId);
        assertThat(tradeOrderOpt).isPresent();
        TradeOrder tradeOrder = tradeOrderOpt.get();

        log.info("最终订单状态: {}", tradeOrder.getStatus());
        assertThat(tradeOrder.getStatus()).isEqualTo(TradeStatus.REFUND);
    }

    /**
     * Mock 支付网关退款，模拟耗时操作
     *
     * @param delaySeconds 延迟秒数
     */
    private void mockSlowRefund(int delaySeconds) {
        doAnswer(invocation -> {
            gatewayCallCount.incrementAndGet();
            int callNumber = gatewayCallCount.get();

            log.info("【支付网关】第 {} 次调用开始，模拟 {} 秒延迟...", callNumber, delaySeconds);

            // 模拟耗时操作
            Thread.sleep(delaySeconds * 1000L);

            log.info("【支付网关】第 {} 次调用完成", callNumber);

            return IPaymentRefundGateway.RefundResult.success("REFUND-" + System.currentTimeMillis());

        }).when(paymentRefundGateway).refund(anyString(), any(BigDecimal.class), anyString(), anyString());
    }

    /**
     * 创建测试数据：Order + 已支付的 TradeOrder
     */
    private void createTestData() {
        // 1. 创建 Order（3人团，已有1人支付）
        String teamId = String.format("%08d", System.currentTimeMillis() % 100000000);
        String activityId = "ACT_TEST8";
        String spuId = "SPU_TEST8";
        String leaderUserId = "USER_TEST8_1";

        Order order = Order.create(
                testOrderId,
                teamId,
                activityId,
                spuId,
                leaderUserId,
                3, // targetCount: 3人团
                Money.of(BigDecimal.valueOf(99.99), BigDecimal.valueOf(79.99)),
                LocalDateTime.now().plusMinutes(30),
                "APP",
                "iOS");

        orderRepository.save(order);
        log.info("创建测试 Order: orderId={}, targetCount={}, completeCount={}",
                testOrderId, 3, order.getCompleteCount());

        // 2. 创建 SKU 并预冻结库存（模拟已锁单）
        String skuId = "SKU_TEST8";
        org.example.domain.model.goods.Sku sku = org.example.domain.model.goods.Sku.create(skuId, spuId,
                "iPhone 15 Pro", BigDecimal.valueOf(100.00), 100);
        sku.freezeStock(1);
        skuRepository.save(sku);
        log.info("【准备数据】创建SKU成功：skuId={}, frozenStock=1", skuId);

        // 3. 创建已支付的 TradeOrder
        TradeOrder tradeOrder = TradeOrder.create(
                testTradeOrderId,
                teamId,
                testOrderId,
                activityId,
                "USER_TEST8_2",
                skuId,
                "iPhone 15 Pro",
                BigDecimal.valueOf(99.99),
                BigDecimal.valueOf(20.00),
                BigDecimal.valueOf(79.99),
                "OUT-TRADE-" + System.currentTimeMillis(),
                "WEB",
                "WECHAT",
                null);

        // 标记为已支付
        tradeOrder.markAsPaid(LocalDateTime.now());

        // 保存到数据库
        tradeOrderRepository.save(tradeOrder);

        log.info("创建测试 TradeOrder: tradeOrderId={}, status={}", testTradeOrderId, tradeOrder.getStatus());
    }
}
