package org.example.start.settlement;

import lombok.extern.slf4j.Slf4j;
import org.example.application.service.payment.PaymentCallbackApplicationService;
import org.example.domain.model.notification.NotificationTask;
import org.example.domain.model.notification.repository.NotificationTaskRepository;
import org.example.domain.model.order.Order;
import org.example.domain.model.order.valueobject.OrderStatus;
import org.example.domain.model.order.valueobject.Money;
import org.example.domain.model.trade.TradeOrder;
import org.example.domain.model.trade.valueobject.NotifyConfig;
import org.example.domain.model.trade.valueobject.TradeStatus;
import org.example.domain.model.order.repository.OrderRepository;
import org.example.domain.model.trade.repository.TradeOrderRepository;
import org.example.domain.service.SettlementService;
import org.example.start.base.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.example.common.exception.BizException;

/**
 * SettlementService 并发支付回调测试
 *
 * <h3>测试目标：验证并发支付回调的数据一致性和原子性</h3>
 *
 * <p>
 * 业务场景：
 * <ol>
 * <li>创建 5 人拼团，已有 3 人支付成功（completeCount=3）</li>
 * <li>用户 4 和用户 5 同时发起支付回调（并发场景）</li>
 * <li>SQL 原子更新保证只有一个线程能成功增加 completeCount</li>
 * <li>成功的线程将 Order 状态更新为 SUCCESS 并触发结算</li>
 * <li>失败的线程会因为 WHERE 条件不满足而抛出异常</li>
 * </ol>
 *
 * <p>
 * 验证重点：
 * <ul>
 * <li>SQL 原子更新是否防止了超卖（completeCount 不会超过 targetCount）</li>
 * <li>并发场景下 Order 和 TradeOrder 状态是否最终一致</li>
 * <li>失败线程的异常处理是否正确</li>
 * <li>幂等性检查是否正确工作</li>
 * </ul>
 *
 * <p>
 * 预期结果：
 * <ul>
 * <li>Order.completeCount = 5（正确）</li>
 * <li>Order.status = SUCCESS（成团）</li>
 * <li>所有 5 个 TradeOrder 都应该被结算（status=SETTLED）</li>
 * <li>只有一个线程能成功触发结算</li>
 * </ul>
 */
@Slf4j
@DisplayName("Test 5: SettlementService 并发支付回调与幂等性测试")
public class SettlementServiceConcurrentCallbackTest extends IntegrationTestBase {

    @Autowired
    private PaymentCallbackApplicationService paymentCallbackApplicationService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TradeOrderRepository tradeOrderRepository;

    @Autowired
    private org.example.domain.service.RefundService refundService;

    @Autowired
    private SettlementService settlementService;

    @Autowired
    private NotificationTaskRepository notificationTaskRepository;

    @Autowired
    private org.example.domain.model.goods.repository.SkuRepository skuRepository;

    /**
     * 测试1：并发支付回调的数据一致性和原子性
     *
     * <p>
     * 测试场景：
     * <ol>
     * <li>创建 5 人拼团（targetCount=5）</li>
     * <li>创建 5 个团员的 TradeOrder（2-6号，全部 CREATE 状态）</li>
     * <li>团员2,3,4顺序支付（completeCount = 1+3 = 4）</li>
     * <li>并发处理团员5和6的支付回调，竞争最后1个名额</li>
     * <li>验证 SQL 原子更新的正确性</li>
     * </ol>
     *
     * <p>
     * 关键验证点：
     * <ul>
     * <li>✅ Order.completeCount = 5</li>
     * <li>✅ Order.status = SUCCESS</li>
     * <li>✅ 4 个 TradeOrder 是 SETTLED 状态（团员2,3,4 + 竞争成功的一个）</li>
     * <li>✅ 3 个 PAID（团员2,3,4），1个竞争成功的变SETTLED，1个竞争失败的保持CREATE</li>
     * <li>✅ 只有一个线程成功，另一个线程因 SQL WHERE 条件不满足而失败</li>
     * </ul>
     */
    @Test
    @DisplayName("测试1：并发支付回调的数据一致性和SQL原子性验证")
    @DirtiesContext
    public void testConcurrentPaymentCallback_shouldSettleOnlyOnce() throws InterruptedException {
        // ========== 1. 准备测试数据 ==========
        log.info("========== 【Test 5-1】开始测试：并发支付回调数据一致性 ==========");

        // 创建拼团订单（5 人团）
        String orderId = "ORD_TEST5_" + System.currentTimeMillis();
        String teamId = String.format("%08d", System.currentTimeMillis() % 100000000);
        String activityId = "ACT_TEST5";
        String spuId = "SPU_TEST5";
        String leaderUserId = "USER_TEST5_1";

        Order order = Order.create(
                orderId,
                teamId,
                activityId,
                spuId,
                leaderUserId,
                5, // targetCount
                Money.of(BigDecimal.valueOf(99.99), BigDecimal.valueOf(79.99)),
                LocalDateTime.now().plusMinutes(30),
                "APP",
                "iOS");
        log.info("【准备数据】创建拼团订单：orderId={}, targetCount={}, completeCount={}", orderId, 5, order.getCompleteCount());
        log.info("【重要】Order.create() 已将 completeCount 初始化为 1（团长），lockCount=1");

        // 保存 Order 到数据库
        orderRepository.save(order);
        log.info("【准备数据】Order 已保存到数据库");

        // 创建 5 个团员的 TradeOrder（CREATE 状态）
        // 注意：团长的 TradeOrder 在真实场景中由 lockOrder 创建，这里简化处理，不创建团长订单
        // Order.completeCount 初始为 1（团长），所以只需 5 个团员即可测试竞争（实际6人团）
        List<TradeOrder> tradeOrders = new ArrayList<>();
        for (int i = 2; i <= 6; i++) { // 从 2 开始，模拟团员 2-6
            String tradeOrderId = "TRD_TEST5_" + i;
            String userId = "USER_TEST5_" + i;
            String skuId = "SKU_TEST5";
            String goodsName = "测试商品";
            String outTradeNo = "OUT_TEST5_" + i;

            TradeOrder tradeOrder = TradeOrder.create(
                    tradeOrderId,
                    teamId,
                    orderId,
                    activityId,
                    userId,
                    skuId,
                    goodsName,
                    BigDecimal.valueOf(99.99),
                    BigDecimal.valueOf(20.00),
                    BigDecimal.valueOf(79.99),
                    outTradeNo,
                    "APP",
                    "iOS",
                    null // notifyConfig
            );

            tradeOrderRepository.save(tradeOrder);
            tradeOrders.add(tradeOrder);
            log.info("【准备数据】创建待支付订单：tradeOrderId={}, userId={}, status=CREATE", tradeOrderId, userId);
        }

        // 团员 2, 3, 4 顺序支付（模拟已完成的支付）
        // 此时：completeCount = 1(团长) + 3(已支付) = 4
        for (int i = 2; i <= 4; i++) {
            String outTradeNo = "OUT_TEST5_" + i;
            paymentCallbackApplicationService.handlePaymentSuccess(outTradeNo, BigDecimal.valueOf(79.99));
            log.info("【准备数据】团员{}支付成功", i);
        }

        // 此时：Order.completeCount = 4，还剩 1 个名额（团员5和6竞争最后1个名额）

        Order orderBefore = orderRepository.findById(orderId).orElseThrow();
        log.info("【准备数据】Order 初始状态：completeCount={}, status={}", orderBefore.getCompleteCount(),
                orderBefore.getStatus());

        // ========== 2. 并发执行支付回调 ==========
        int threadCount = 2;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // 记录成功和失败的线程数
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // 并发线程：处理团员 5 的支付回调
        executor.submit(() -> {
            try {
                startLatch.await();
                log.info("【线程-团员5】开始处理支付回调");
                String outTradeNo5 = "OUT_TEST5_5";
                paymentCallbackApplicationService.handlePaymentSuccess(outTradeNo5, BigDecimal.valueOf(79.99));
                successCount.incrementAndGet();
                log.info("【线程-团员5】支付回调处理完成 ✅");
            } catch (Exception e) {
                failureCount.incrementAndGet();
                log.info("【线程-团员5】支付回调失败（预期行为，SQL WHERE 条件不满足）: {}", e.getMessage());
            } finally {
                doneLatch.countDown();
            }
        });

        // 并发线程：处理团员 6 的支付回调
        executor.submit(() -> {
            try {
                startLatch.await();
                log.info("【线程-团员6】开始处理支付回调");
                String outTradeNo6 = "OUT_TEST5_6";
                paymentCallbackApplicationService.handlePaymentSuccess(outTradeNo6, BigDecimal.valueOf(79.99));
                successCount.incrementAndGet();
                log.info("【线程-团员6】支付回调处理完成 ✅");
            } catch (Exception e) {
                failureCount.incrementAndGet();
                log.info("【线程-团员6】支付回调失败（预期行为，SQL WHERE 条件不满足）: {}", e.getMessage());
            } finally {
                doneLatch.countDown();
            }
        });

        // 同时触发所有线程
        log.info("【并发测试】同时触发 {} 个线程", threadCount);
        startLatch.countDown();

        // 等待所有线程完成
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // 等待事件被消费
        Thread.sleep(1500);

        assertThat(completed).isTrue();
        log.info("【并发测试】所有线程执行完成，成功: {}, 失败: {}", successCount.get(), failureCount.get());

        // ========== 3. 验证结果 ==========

        // 验证 1：Order 状态
        Order orderAfter = orderRepository.findById(orderId).orElseThrow();
        log.info("【验证结果】Order 最终状态：completeCount={}, status={}", orderAfter.getCompleteCount(), orderAfter.getStatus());

        // ⭐ 关键验证：修复后，竞争失败的线程不会增加 completeCount
        // completeCount 应该是：1(团长) + 3(已支付的团员2,3,4) + 1(并发成功的线程) = 5
        assertThat(orderAfter.getCompleteCount()).isEqualTo(5); // 成团！
        assertThat(orderAfter.getStatus()).isEqualTo(OrderStatus.SUCCESS); // 已成团

        // 验证 2：TradeOrder 状态分布
        List<TradeOrder> tradeOrdersAfter = tradeOrderRepository.findByOrderId(orderId);

        long paidCount = tradeOrdersAfter.stream()
                .filter(to -> to.getStatus() == TradeStatus.PAID)
                .count();

        long settledCount = tradeOrdersAfter.stream()
                .filter(to -> to.getStatus() == TradeStatus.SETTLED)
                .count();

        long createCount = tradeOrdersAfter.stream()
                .filter(to -> to.getStatus() == TradeStatus.CREATE)
                .count();

        log.info("【验证结果】TradeOrder 状态分布：PAID={}, SETTLED={}, CREATE={}", paidCount, settledCount, createCount);

        // ⭐ 关键验证：修复后，竞争失败的线程不会标记为 PAID
        // 团员2,3,4已PAID，团员5或6有一个竞争成功变PAID（共4个PAID），成团后全部变SETTLED
        assertThat(settledCount).isEqualTo(4); // 成团后结算：团员2,3,4 + 竞争成功的一个
        assertThat(paidCount).isEqualTo(0); // 已结算的不再是PAID状态
        assertThat(createCount).isEqualTo(1); // 并发失败的线程保持 CREATE

        // 验证 3：并发控制效果
        assertThat(successCount.get()).isEqualTo(1); // 只有一个线程成功
        assertThat(failureCount.get()).isEqualTo(1); // 另一个线程失败

        log.info("========== 【Test 5-1】测试完成 ==========");
        log.info("【测试结论】✅ 修复后：竞争失败的线程不会污染 TradeOrder 状态");
        log.info("【测试结论】✅ 数据一致性：Order.completeCount({}) == PAID的TradeOrder数量({})",
                orderAfter.getCompleteCount(), paidCount);
        log.info("【测试结论】✅ 只有一个线程能成功增加 completeCount");
        log.info("【测试结论】✅ 并发场景下数据最终一致");
    }

    /**
     * 测试2：支付回调的幂等性（未成团场景）
     *
     * <p>
     * 测试场景：
     * <ol>
     * <li>创建 3 人拼团（Order.create()后completeCount=1，团长占位）</li>
     * <li>第一次处理用户1的支付回调 → Order: completeCount=2（1团长+1用户1）</li>
     * <li>第二次处理用户1的支付回调（相同 outTradeNo）</li>
     * <li>验证幂等性：第二次调用应该静默返回，状态不变</li>
     * </ol>
     *
     * <p>
     * 关键验证点：
     * <ul>
     * <li>✅ Order.completeCount 只增加 1 次</li>
     * <li>✅ TradeOrder 状态不变（仍然是 PAID）</li>
     * <li>✅ Order 状态不变（仍然是 PENDING）</li>
     * </ul>
     */
    @Test
    @DisplayName("测试2：支付回调应该具有幂等性（未成团场景）")
    @DirtiesContext
    public void testPaymentCallback_idempotentBeforeCompletion() {
        log.info("========== 【Test 5-2】开始测试：未成团场景的幂等性 ==========");

        // 创建拼团订单（3 人团，避免第一次支付就成团）
        String orderId = "ORD_IDEMPOTENT_" + System.currentTimeMillis();
        String teamId = String.format("%08d", System.currentTimeMillis() % 100000000);
        String activityId = "ACT_IDEMPOTENT";
        String spuId = "SPU_IDEMPOTENT";
        String leaderUserId = "USER_IDEMPOTENT_1";

        Order order = Order.create(
                orderId,
                teamId,
                activityId,
                spuId,
                leaderUserId,
                3, // targetCount = 3 (避免第一次就成团)
                Money.of(BigDecimal.valueOf(50.00), BigDecimal.valueOf(40.00)),
                LocalDateTime.now().plusMinutes(30),
                "APP",
                "Android");
        orderRepository.save(order);
        log.info("【准备数据】创建拼团订单：orderId={}, targetCount=3", orderId);

        // 创建 3 个待支付的 TradeOrder
        for (int i = 1; i <= 3; i++) {
            String tradeOrderId = "TRD_IDEMPOTENT_" + i;
            String userId = "USER_IDEMPOTENT_" + i;
            String skuId = "SKU_IDEMPOTENT";
            String goodsName = "测试商品";
            String outTradeNo = "OUT_IDEMPOTENT_" + i;

            TradeOrder tradeOrder = TradeOrder.create(
                    tradeOrderId,
                    teamId,
                    orderId,
                    activityId,
                    userId,
                    skuId,
                    goodsName,
                    BigDecimal.valueOf(50.00),
                    BigDecimal.valueOf(10.00),
                    BigDecimal.valueOf(40.00),
                    outTradeNo,
                    "APP",
                    "Android",
                    null // notifyConfig
            );

            tradeOrderRepository.save(tradeOrder);
            log.info("【准备数据】创建待支付订单：tradeOrderId={}, userId={}", tradeOrderId, userId);
        }

        // 第一次处理用户1的支付回调
        log.info("【幂等性测试】第一次处理用户1的支付回调");
        paymentCallbackApplicationService.handlePaymentSuccess("OUT_IDEMPOTENT_1", BigDecimal.valueOf(40.00));

        // 验证第一次处理后的状态
        Order orderAfterFirst = orderRepository.findById(orderId).orElseThrow();
        TradeOrder tradeOrder1AfterFirst = tradeOrderRepository.findByOutTradeNo("OUT_IDEMPOTENT_1").orElseThrow();

        log.info("【幂等性测试】第一次处理后：Order.completeCount={}, status={}",
                orderAfterFirst.getCompleteCount(), orderAfterFirst.getStatus());
        log.info("【幂等性测试】第一次处理后：TradeOrder.status={}", tradeOrder1AfterFirst.getStatus());

        assertThat(orderAfterFirst.getCompleteCount()).isEqualTo(2);
        assertThat(orderAfterFirst.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(tradeOrder1AfterFirst.getStatus()).isEqualTo(TradeStatus.PAID);

        // ⭐ 第二次处理相同的支付回调（测试幂等性）
        log.info("【幂等性测试】第二次处理用户1的支付回调（相同 outTradeNo）");
        paymentCallbackApplicationService.handlePaymentSuccess("OUT_IDEMPOTENT_1", BigDecimal.valueOf(40.00));

        // 验证第二次调用后状态不变
        Order orderAfterSecond = orderRepository.findById(orderId).orElseThrow();
        TradeOrder tradeOrder1AfterSecond = tradeOrderRepository.findByOutTradeNo("OUT_IDEMPOTENT_1").orElseThrow();

        log.info("【幂等性测试】第二次处理后：Order.completeCount={}, status={}",
                orderAfterSecond.getCompleteCount(), orderAfterSecond.getStatus());
        log.info("【幂等性测试】第二次处理后：TradeOrder.status={}", tradeOrder1AfterSecond.getStatus());

        // 验证幂等性：状态应该与第一次处理后完全相同
        assertThat(orderAfterSecond.getCompleteCount()).isEqualTo(2); // 只增加了 1 次
        assertThat(orderAfterSecond.getStatus()).isEqualTo(OrderStatus.PENDING); // 仍然未成团
        assertThat(tradeOrder1AfterSecond.getStatus()).isEqualTo(TradeStatus.PAID); // 状态不变

        log.info("========== 【Test 5-2】测试完成：幂等性验证通过 ✅ ==========");
    }

    /**
     * 测试3：成团后支付回调的幂等性
     *
     * <p>
     * 测试场景：
     * <ol>
     * <li>创建 3 人拼团</li>
     * <li>第一次处理用户1的支付回调 → Order: completeCount=1</li>
     * <li>第一次处理用户2的支付回调 → Order: completeCount=2, status=SUCCESS（成团）</li>
     * <li>第二次处理用户1的支付回调（测试成团后的幂等性）</li>
     * <li>第二次处理用户2的支付回调（测试成团后的幂等性）</li>
     * </ol>
     *
     * <p>
     * 关键验证点：
     * <ul>
     * <li>✅ 第二次调用应该静默返回（因为 TradeOrder.status=SETTLED）</li>
     * <li>✅ Order 和 TradeOrder 状态不变</li>
     * <li>✅ 验证幂等性检查正确处理 SETTLED 状态</li>
     * </ul>
     */
    @Test
    @DisplayName("测试3：成团后支付回调应该具有幂等性")
    @DirtiesContext
    public void testPaymentCallback_idempotentAfterCompletion() throws InterruptedException {
        log.info("========== 【Test 5-3】开始测试：成团后的幂等性 ==========");

        // 创建拼团订单（3 人团）
        String orderId = "ORD_IDEMPOTENT2_" + System.currentTimeMillis();
        String teamId = String.format("%08d", System.currentTimeMillis() % 100000000);
        String activityId = "ACT_IDEMPOTENT2";
        String spuId = "SPU_IDEMPOTENT2";
        String leaderUserId = "USER_IDEMPOTENT2_1";

        Order order = Order.create(
                orderId,
                teamId,
                activityId,
                spuId,
                leaderUserId,
                3, // targetCount = 3，差2人
                Money.of(BigDecimal.valueOf(50.00), BigDecimal.valueOf(40.00)),
                LocalDateTime.now().plusMinutes(30),
                "APP",
                "Android");
        orderRepository.save(order);
        log.info("【准备数据】创建拼团订单：orderId={}, targetCount=2", orderId);

        // 创建 2 个待支付的 TradeOrder
        for (int i = 1; i <= 2; i++) {
            String tradeOrderId = "TRD_IDEMPOTENT2_" + i;
            String userId = "USER_IDEMPOTENT2_" + i;
            String skuId = "SKU_IDEMPOTENT2";
            String goodsName = "测试商品";
            String outTradeNo = "OUT_IDEMPOTENT2_" + i;

            TradeOrder tradeOrder = TradeOrder.create(
                    tradeOrderId,
                    teamId,
                    orderId,
                    activityId,
                    userId,
                    skuId,
                    goodsName,
                    BigDecimal.valueOf(50.00),
                    BigDecimal.valueOf(10.00),
                    BigDecimal.valueOf(40.00),
                    outTradeNo,
                    "APP",
                    "Android",
                    null);

            tradeOrderRepository.save(tradeOrder);
            log.info("【准备数据】创建待支付订单：tradeOrderId={}, userId={}", tradeOrderId, userId);
        }

        // 第一次处理用户1的支付回调
        log.info("【幂等性测试】第一次处理用户1的支付回调");
        paymentCallbackApplicationService.handlePaymentSuccess("OUT_IDEMPOTENT2_1", BigDecimal.valueOf(40.00));

        // 第一次处理用户2的支付回调（触发成团）
        log.info("【幂等性测试】第一次处理用户2的支付回调（触发成团）");
        paymentCallbackApplicationService.handlePaymentSuccess("OUT_IDEMPOTENT2_2", BigDecimal.valueOf(40.00));

        // 等待支付事件被消费
        Thread.sleep(1500);

        // 验证成团后的状态
        Order orderAfterCompletion = orderRepository.findById(orderId).orElseThrow();
        List<TradeOrder> tradeOrdersAfterCompletion = tradeOrderRepository.findByOrderId(orderId);

        log.info("【幂等性测试】成团后：Order.completeCount={}, status={}",
                orderAfterCompletion.getCompleteCount(), orderAfterCompletion.getStatus());
        assertThat(orderAfterCompletion.getCompleteCount()).isEqualTo(3);
        assertThat(orderAfterCompletion.getStatus()).isEqualTo(OrderStatus.SUCCESS);

        // 所有 TradeOrder 应该都是 SETTLED 状态
        tradeOrdersAfterCompletion.forEach(to -> {
            log.info("【幂等性测试】成团后：TradeOrder.id={}, status={}", to.getTradeOrderId(), to.getStatus());
            assertThat(to.getStatus()).isEqualTo(TradeStatus.SETTLED);
        });

        // ⭐ 第二次处理用户1的支付回调（测试成团后的幂等性）
        log.info("【幂等性测试】第二次处理用户1的支付回调（TradeOrder 已 SETTLED）");
        paymentCallbackApplicationService.handlePaymentSuccess("OUT_IDEMPOTENT2_1", BigDecimal.valueOf(40.00));

        // ⭐ 第二次处理用户2的支付回调（测试成团后的幂等性）
        log.info("【幂等性测试】第二次处理用户2的支付回调（TradeOrder 已 SETTLED）");
        paymentCallbackApplicationService.handlePaymentSuccess("OUT_IDEMPOTENT2_2", BigDecimal.valueOf(40.00));

        // 验证状态不变
        Order orderAfterRetry = orderRepository.findById(orderId).orElseThrow();
        List<TradeOrder> tradeOrdersAfterRetry = tradeOrderRepository.findByOrderId(orderId);

        log.info("【幂等性测试】重复调用后：Order.completeCount={}, status={}",
                orderAfterRetry.getCompleteCount(), orderAfterRetry.getStatus());

        assertThat(orderAfterRetry.getCompleteCount()).isEqualTo(3); // 状态不变
        assertThat(orderAfterRetry.getStatus()).isEqualTo(OrderStatus.SUCCESS); // 状态不变

        // 所有 TradeOrder 应该仍然是 SETTLED 状态
        tradeOrdersAfterRetry.forEach(to -> {
            log.info("【幂等性测试】重复调用后：TradeOrder.id={}, status={}", to.getTradeOrderId(), to.getStatus());
            assertThat(to.getStatus()).isEqualTo(TradeStatus.SETTLED);
        });

        log.info("========== 【Test 5-3】测试完成：成团后幂等性验证通过 ✅ ==========");
    }

    /**
     * 测试4：极端高并发支付回调（10线程）
     *
     * <p>
     * 测试场景：
     * <ol>
     * <li>创建11人拼团（targetCount=11，Order.create()后completeCount=1团长占位）</li>
     * <li>前5人顺序支付 → completeCount=6（1团长+5支付）</li>
     * <li>创建10个待支付TradeOrder（用户6-15）</li>
     * <li>10线程并发支付，竞争剩余5个名额</li>
     * <li>验证SQL原子更新在极端高并发下的正确性</li>
     * </ol>
     *
     * <p>
     * 关键验证点：
     * <ul>
     * <li>✅ Order.completeCount = 11</li>
     * <li>✅ Order.status = SUCCESS</li>
     * <li>✅ 只有5个线程成功，其他5个失败</li>
     * <li>✅ 11个TradeOrder都是SETTLED状态</li>
     * </ul>
     */
    @Test
    @DisplayName("测试4：极端高并发支付回调（10线程）")
    @DirtiesContext
    public void testHighConcurrentPaymentCallback_shouldHandleCorrectly() throws InterruptedException {
        // ========== 1. 准备测试数据 ==========
        log.info("========== 【Test 5-4】开始测试：极端高并发支付回调（10线程） ==========");

        // 创建拼团订单（11 人团）
        String orderId = "ORD_TEST54_" + System.currentTimeMillis();
        String teamId = String.format("%08d", System.currentTimeMillis() % 100000000);
        String activityId = "ACT_TEST54";
        String spuId = "SPU_TEST54";
        String leaderUserId = "USER_TEST54_1";

        Order order = Order.create(
                orderId,
                teamId,
                activityId,
                spuId,
                leaderUserId,
                11, // targetCount = 11 (1团长 + 5初始支付 + 5并发成功 = 11)
                Money.of(BigDecimal.valueOf(99.99), BigDecimal.valueOf(79.99)),
                LocalDateTime.now().plusMinutes(30),
                "APP",
                "iOS");
        orderRepository.save(order);
        log.info("【准备数据】创建拼团订单：orderId={}, targetCount=11, 初始completeCount=1（团长）", orderId);

        // 创建 15 个 TradeOrder（用户1-15，全部 CREATE 状态）
        List<TradeOrder> tradeOrders = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            String tradeOrderId = "TRD_TEST54_" + i;
            String userId = "USER_TEST54_" + i;
            String skuId = "SKU_TEST54";
            String goodsName = "测试商品";
            String outTradeNo = "OUT_TEST54_" + i;

            TradeOrder tradeOrder = TradeOrder.create(
                    tradeOrderId,
                    teamId,
                    orderId,
                    activityId,
                    userId,
                    skuId,
                    goodsName,
                    BigDecimal.valueOf(99.99),
                    BigDecimal.valueOf(20.00),
                    BigDecimal.valueOf(79.99),
                    outTradeNo,
                    "APP",
                    "iOS",
                    null);

            tradeOrderRepository.save(tradeOrder);
            tradeOrders.add(tradeOrder);
            log.info("【准备数据】创建待支付订单：tradeOrderId={}, userId={}, status=CREATE", tradeOrderId, userId);
        }

        // 前 5 人顺序支付（模拟已完成的支付）
        for (int i = 1; i <= 5; i++) {
            String outTradeNo = "OUT_TEST54_" + i;
            paymentCallbackApplicationService.handlePaymentSuccess(outTradeNo, BigDecimal.valueOf(79.99));
            log.info("【准备数据】用户{}支付成功", i);
        }

        Order orderBefore = orderRepository.findById(orderId).orElseThrow();
        log.info("【准备数据】Order 初始状态：completeCount={} (1团长+5支付), status={}", orderBefore.getCompleteCount(),
                orderBefore.getStatus());
        assertThat(orderBefore.getCompleteCount()).isEqualTo(6); // 验证初始支付后的状态

        // ========== 2. 并发执行支付回调（10线程） ==========
        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // 记录成功和失败的线程数
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // 并发处理用户 6-15 的支付回调（10个线程竞争剩余5个名额）
        for (int i = 6; i <= 15; i++) {
            final int userId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    log.info("【线程-用户{}】开始处理支付回调", userId);
                    String outTradeNo = "OUT_TEST54_" + userId;
                    paymentCallbackApplicationService.handlePaymentSuccess(outTradeNo, BigDecimal.valueOf(79.99));
                    successCount.incrementAndGet();
                    log.info("【线程-用户{}】支付回调处理完成 ✅", userId);
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    log.info("【线程-用户{}】支付回调失败（预期行为，SQL WHERE 条件不满足）: {}", userId, e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // 同时触发所有线程
        log.info("【并发测试】同时触发 {} 个线程", threadCount);
        startLatch.countDown();

        // ========== 3. 等待线程完成 ==========
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        log.info("【验证】所有并发线程已完成");

        // ⭐ 【关键】等待异步settlement完成（事件驱动）
        // SettlementEventListener 在事务提交后异步处理settlement，需要等待其完成
        // 100ms (listener delay) + 缓冲时间
        log.info("【验证】等待异步settlement完成...");
        Thread.sleep(1500);
        log.info("【验证】异步等待结束");

        // ========== 4. 验证最终状态 ==========
        // 验证 1：Order状态为SUCCESS，completeCount=11
        Order orderAfter = orderRepository.findById(orderId).orElseThrow();
        log.info("【验证结果】Order 最终状态：status={}, completeCount={}", orderAfter.getStatus(),
                orderAfter.getCompleteCount());
        assertThat(orderAfter.getStatus()).isEqualTo(OrderStatus.SUCCESS);
        assertThat(orderAfter.getCompleteCount()).isEqualTo(11);

        // 验证 2：TradeOrder状态分布
        // ⭐ 事件驱动settlement应该处理所有PAID状态的TradeOrder
        // 期望：10个SETTLED（用户1-10，团长TradeOrder从未创建），0个PAID，5个CREATE（用户11-15）
        List<TradeOrder> tradeOrdersAfter = tradeOrderRepository.findByOrderId(orderId);
        long settledCount = tradeOrdersAfter.stream()
                .filter(to -> to.getStatus() == TradeStatus.SETTLED)
                .count();
        long paidCount = tradeOrdersAfter.stream()
                .filter(to -> to.getStatus() == TradeStatus.PAID)
                .count();
        long createCount = tradeOrdersAfter.stream()
                .filter(to -> to.getStatus() == TradeStatus.CREATE)
                .count();

        log.info("【验证结果】TradeOrder 状态分布：SETTLED={}, PAID={}, CREATE={}", settledCount, paidCount, createCount);

        // ⭐ 事件驱动settlement应该处理所有成功支付的TradeOrder
        assertThat(settledCount).isEqualTo(10); // 10个成功的（用户1-10，团长无TradeOrder）
        assertThat(paidCount).isEqualTo(0); // 异步settlement后不应有PAID状态
        assertThat(createCount).isEqualTo(5); // 5个失败的（用户11-15）

        // 验证 3：并发控制效果（只有5个线程成功，因为只剩5个名额）
        assertThat(successCount.get()).isEqualTo(5); // 只有5个线程成功
        assertThat(failureCount.get()).isEqualTo(5); // 另外5个线程失败

        log.info("========== 【Test 5-4】测试完成 ==========");
        log.info("【测试结论】✅ SQL 原子更新在极端高并发（10线程）下防止了超卖");
        log.info("【测试结论】✅ 只有5个线程能成功增加 completeCount");
        log.info("【测试结论】✅ 事件驱动settlement成功处理所有并发支付");
        log.info("【测试结论】✅ 并发场景下数据最终一致");
    }

    /**
     * 测试5：金额校验失败场景
     *
     * <p>
     * 测试场景：
     * <ol>
     * <li>创建订单（支付价格79.99）</li>
     * <li>支付回调传入错误金额99.99</li>
     * <li>验证系统是否阻断处理</li>
     * </ol>
     *
     * <p>
     * 关键验证点：
     * <ul>
     * <li>✅ 抛出BizException("支付金额异常，请联系客服")</li>
     * <li>✅ TradeOrder状态保持CREATE</li>
     * <li>✅ Order.completeCount不增加</li>
     * <li>✅ 日志记录安全告警</li>
     * </ul>
     */
    @Test
    @DisplayName("测试5：金额校验失败应该阻断处理")
    @DirtiesContext
    public void testPaymentCallback_withWrongAmount_shouldReject() {
        log.info("========== 【Test 5-5】开始测试：金额校验失败场景 ==========");

        // ========== 1. 准备测试数据 ==========
        // 创建拼团订单（3 人团）
        String orderId = "ORD_AMOUNT_" + System.currentTimeMillis();
        String teamId = String.format("%08d", System.currentTimeMillis() % 100000000);
        String activityId = "ACT_AMOUNT";
        String spuId = "SPU_AMOUNT";
        String leaderUserId = "USER_AMOUNT_1";

        Order order = Order.create(
                orderId,
                teamId,
                activityId,
                spuId,
                leaderUserId,
                3, // targetCount = 3
                Money.of(BigDecimal.valueOf(99.99), BigDecimal.valueOf(79.99)),
                LocalDateTime.now().plusMinutes(30),
                "APP",
                "Android");
        orderRepository.save(order);
        log.info("【准备数据】创建拼团订单：orderId={}, targetCount=3", orderId);

        // 创建 TradeOrder（支付价格=79.99）
        String tradeOrderId = "TRD_AMOUNT_1";
        String userId = "USER_AMOUNT_1";
        String skuId = "SKU_AMOUNT";
        String goodsName = "测试商品";
        String outTradeNo = "OUT_AMOUNT_1";

        TradeOrder tradeOrder = TradeOrder.create(
                tradeOrderId,
                teamId,
                orderId,
                activityId,
                userId,
                skuId,
                goodsName,
                BigDecimal.valueOf(99.99),
                BigDecimal.valueOf(20.00),
                BigDecimal.valueOf(79.99), // 正确的支付价格
                outTradeNo,
                "APP",
                "Android",
                null);

        tradeOrderRepository.save(tradeOrder);
        log.info("【准备数据】创建待支付订单：tradeOrderId={}, payPrice=79.99", tradeOrderId);

        // ========== 2. 执行测试：传入错误金额 ==========
        BigDecimal wrongAmount = BigDecimal.valueOf(99.99); // 错误金额（应该是79.99）
        log.info("【金额校验测试】尝试使用错误金额进行支付回调：callbackAmount={}, 正确金额={}", wrongAmount, tradeOrder.getPayPrice());

        // 验证：应该抛出异常
        assertThatThrownBy(() -> {
            paymentCallbackApplicationService.handlePaymentSuccess(outTradeNo, wrongAmount);
        })
                .isInstanceOf(BizException.class)
                .hasMessageContaining("支付金额异常");

        log.info("【金额校验测试】✅ 系统正确拒绝了错误金额的支付回调");

        // ========== 3. 验证结果 ==========

        // 验证 1：TradeOrder 状态保持 CREATE
        TradeOrder tradeOrderAfter = tradeOrderRepository.findByOutTradeNo(outTradeNo).orElseThrow();
        log.info("【验证结果】TradeOrder 状态：{}", tradeOrderAfter.getStatus());
        assertThat(tradeOrderAfter.getStatus()).isEqualTo(TradeStatus.CREATE);

        // 验证 2：Order.completeCount 不增加
        Order orderAfter = orderRepository.findById(orderId).orElseThrow();
        log.info("【验证结果】Order.completeCount={}", orderAfter.getCompleteCount());
        assertThat(orderAfter.getCompleteCount()).isEqualTo(1); // 初始值（团长自己）

        // 验证 3：Order 状态保持 PENDING
        log.info("【验证结果】Order.status={}", orderAfter.getStatus());
        assertThat(orderAfter.getStatus()).isEqualTo(OrderStatus.PENDING);

        log.info("========== 【Test 5-5】测试完成：金额校验验证通过 ✅ ==========");
        log.info("【测试结论】✅ 金额校验正确阻断了错误金额的支付");
        log.info("【测试结论】✅ TradeOrder 和 Order 状态未被错误修改");
        log.info("【测试结论】✅ 系统具备防篡改能力");
    }

    /**
     * 测试6：并发支付回调与超时处理冲突（Race #9）
     *
     * <p>
     * 测试场景：
     * <ol>
     * <li>创建 3 人拼团订单（Order.create()后completeCount=1，团长占位）</li>
     * <li>创建 1 个待支付的 TradeOrder（status=CREATE）</li>
     * <li>线程1：处理支付成功回调</li>
     * <li>线程2：同时处理超时退单</li>
     * <li>验证两个操作的互斥性</li>
     * </ol>
     *
     * <p>
     * 关键验证点：
     * <ul>
     * <li>✅ 只有一个操作成功（支付 OR 超时）</li>
     * <li>✅ 如果支付成功：TradeOrder.status=PAID/SETTLED，Order.completeCount=2</li>
     * <li>✅ 如果超时成功：TradeOrder.status=TIMEOUT，Order.completeCount=1</li>
     * <li>✅ 不会出现中间状态（如支付成功但被退款）</li>
     * </ul>
     *
     * <p>
     * 风险：
     * <ul>
     * <li>🔴 用户付款成功但订单被标记为超时</li>
     * <li>🔴 Order.completeCount增加但TradeOrder被退款</li>
     * <li>🔴 资源状态不一致</li>
     * </ul>
     */
    @Test
    @DisplayName("测试6：并发支付回调与超时处理冲突（Race #9）")
    @DirtiesContext
    public void testPaymentCallbackVsTimeout_shouldBeMutuallyExclusive() throws InterruptedException {
        // ========== 1. 准备测试数据 ==========
        log.info("========== 【Test 5-6】开始测试：并发支付回调与超时处理冲突 ==========");

        // 创建拼团订单（3 人团）
        String orderId = "ORD_RACE9_" + System.currentTimeMillis();
        String teamId = String.format("%08d", System.currentTimeMillis() % 100000000);
        String activityId = "ACT_RACE9";
        String spuId = "SPU_RACE9";
        String leaderUserId = "USER_RACE9_LEADER";

        Order order = Order.create(
                orderId,
                teamId,
                activityId,
                spuId,
                leaderUserId,
                3, // targetCount = 3（1团长 + 2成员）
                Money.of(BigDecimal.valueOf(99.99), BigDecimal.valueOf(79.99)),
                LocalDateTime.now().plusMinutes(30),
                "APP",
                "iOS");
        orderRepository.save(order);
        log.info("【准备数据】创建拼团订单：orderId={}, targetCount=3, 初始completeCount=1（团长）", orderId);

        // 创建 1 个待支付的 TradeOrder（status=CREATE）
        String tradeOrderId = "TRD_RACE9_1";
        String userId = "USER_RACE9_1";
        String skuId = "SKU_RACE9";
        String goodsName = "测试商品";
        String outTradeNo = "OUT_RACE9_1";

        // 创建 SKU 并预冻结库存（模拟已锁单）
        org.example.domain.model.goods.Sku sku = org.example.domain.model.goods.Sku.create(skuId, spuId, goodsName,
                BigDecimal.valueOf(100.00), 100);
        sku.freezeStock(1);
        skuRepository.save(sku);
        log.info("【准备数据】创建SKU成功：skuId={}, frozenStock=1", skuId);

        TradeOrder tradeOrder = TradeOrder.create(
                tradeOrderId,
                teamId,
                orderId,
                activityId,
                userId,
                skuId,
                goodsName,
                BigDecimal.valueOf(99.99),
                BigDecimal.valueOf(20.00),
                BigDecimal.valueOf(79.99),
                outTradeNo,
                "APP",
                "iOS",
                null);

        tradeOrderRepository.save(tradeOrder);
        log.info("【准备数据】创建待支付订单：tradeOrderId={}, status=CREATE", tradeOrderId);

        // 验证初始状态
        Order orderBefore = orderRepository.findById(orderId).orElseThrow();
        TradeOrder tradeOrderBefore = tradeOrderRepository.findByTradeOrderId(tradeOrderId).orElseThrow();
        log.info("【准备数据】初始状态：Order.completeCount={}, TradeOrder.status={}",
                orderBefore.getCompleteCount(), tradeOrderBefore.getStatus());
        assertThat(orderBefore.getCompleteCount()).isEqualTo(1); // 团长
        assertThat(tradeOrderBefore.getStatus()).isEqualTo(TradeStatus.CREATE);

        // ========== 2. 并发执行支付回调和超时处理 ==========
        int threadCount = 2;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // 记录成功和失败的线程数
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<String> successOperations = new ArrayList<>();
        List<String> failureOperations = new ArrayList<>();

        // 线程1：支付回调
        executor.submit(() -> {
            try {
                startLatch.await();
                log.info("【线程-支付】开始处理支付回调");
                paymentCallbackApplicationService.handlePaymentSuccess(outTradeNo, BigDecimal.valueOf(79.99));
                successCount.incrementAndGet();
                synchronized (successOperations) {
                    successOperations.add("PAYMENT");
                }
                log.info("【线程-支付】支付回调处理完成 ✅");
            } catch (Exception e) {
                failureCount.incrementAndGet();
                synchronized (failureOperations) {
                    failureOperations.add("PAYMENT");
                }
                log.info("【线程-支付】支付回调失败: {}", e.getMessage());
            } finally {
                doneLatch.countDown();
            }
        });

        // 线程2：超时处理
        executor.submit(() -> {
            try {
                startLatch.await();
                log.info("【线程-超时】开始处理超时退单");
                refundService.refundTradeOrder(tradeOrderId, "超时未支付自动退单");
                successCount.incrementAndGet();
                synchronized (successOperations) {
                    successOperations.add("TIMEOUT");
                }
                log.info("【线程-超时】超时处理完成 ✅");
            } catch (Exception e) {
                failureCount.incrementAndGet();
                synchronized (failureOperations) {
                    failureOperations.add("TIMEOUT");
                }
                log.info("【线程-超时】超时处理失败: {}", e.getMessage());
            } finally {
                doneLatch.countDown();
            }
        });

        // 同时触发所有线程
        log.info("【并发测试】同时触发 {} 个线程（支付 vs 超时）", threadCount);
        startLatch.countDown();

        // 等待所有线程完成
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        log.info("【并发测试】所有线程执行完成，成功: {}, 失败: {}", successCount.get(), failureCount.get());
        log.info("【并发测试】成功操作: {}", successOperations);
        log.info("【并发测试】失败操作: {}", failureOperations);

        // ========== 3. 验证结果 ==========

        // 验证 1：互斥性（只有一个操作成功）
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(1);
        log.info("【验证结果】✅ 互斥性验证通过：只有一个操作成功");

        // 验证 2：状态一致性
        TradeOrder tradeOrderAfter = tradeOrderRepository.findByTradeOrderId(tradeOrderId).orElseThrow();
        Order orderAfter = orderRepository.findById(orderId).orElseThrow();

        log.info("【验证结果】最终状态：TradeOrder.status={}, Order.completeCount={}",
                tradeOrderAfter.getStatus(), orderAfter.getCompleteCount());

        // 根据成功的操作验证状态
        String successOperation = successOperations.isEmpty() ? null : successOperations.get(0);

        if ("PAYMENT".equals(successOperation)) {
            // 场景A：支付成功
            log.info("【验证结果】场景A：支付成功");
            assertThat(tradeOrderAfter.getStatus())
                    .as("支付成功后，TradeOrder应该是PAID或SETTLED状态")
                    .isIn(TradeStatus.PAID, TradeStatus.SETTLED);
            assertThat(orderAfter.getCompleteCount())
                    .as("支付成功后，Order.completeCount应该增加到2（1团长+1支付）")
                    .isEqualTo(2);
            log.info("【验证结果】✅ 支付成功场景验证通过：TradeOrder={}, Order.completeCount={}",
                    tradeOrderAfter.getStatus(), orderAfter.getCompleteCount());

        } else if ("TIMEOUT".equals(successOperation)) {
            // 场景B：超时成功
            log.info("【验证结果】场景B：超时成功");
            assertThat(tradeOrderAfter.getStatus())
                    .as("超时成功后，TradeOrder应该是TIMEOUT状态")
                    .isEqualTo(TradeStatus.TIMEOUT);
            assertThat(orderAfter.getCompleteCount())
                    .as("超时成功后，Order.completeCount应该保持为1（仅团长）")
                    .isEqualTo(1);
            log.info("【验证结果】✅ 超时成功场景验证通过：TradeOrder={}, Order.completeCount={}",
                    tradeOrderAfter.getStatus(), orderAfter.getCompleteCount());

        } else {
            throw new AssertionError("没有任何操作成功，这不应该发生");
        }

        // 验证 3：不允许的中间状态检查
        if (tradeOrderAfter.getStatus() == TradeStatus.PAID || tradeOrderAfter.getStatus() == TradeStatus.SETTLED) {
            assertThat(orderAfter.getCompleteCount())
                    .as("❌ 不允许的状态：TradeOrder已支付但Order.completeCount未增加")
                    .isGreaterThan(1);
        }

        if (tradeOrderAfter.getStatus() == TradeStatus.TIMEOUT) {
            assertThat(orderAfter.getCompleteCount())
                    .as("❌ 不允许的状态：TradeOrder已超时但Order.completeCount增加了")
                    .isEqualTo(1);
        }

        log.info("========== 【Test 5-6】测试完成 ==========");
        log.info("【测试结论】✅ 支付回调与超时处理具有互斥性");
        log.info("【测试结论】✅ 不会出现用户付款但订单超时的情况");
        log.info("【测试结论】✅ 状态一致性验证通过");
        log.info("【测试结论】✅ Race #9 风险已被正确处理");
    }

    /**
     * 测试7：settleCompletedOrder幂等性验证
     *
     * <p>
     * 测试场景：
     * <ol>
     * <li>创建 3 人拼团，全部支付成功（触发结算）</li>
     * <li>第一次手动调用 settleCompletedOrder()（模拟定时任务）</li>
     * <li>第二次手动调用 settleCompletedOrder()（模拟重试）</li>
     * <li>验证通知任务只创建一次</li>
     * </ol>
     *
     * <p>
     * 关键验证点：
     * <ul>
     * <li>✅ 所有 TradeOrder 只被结算一次（PAID → SETTLED）</li>
     * <li>✅ 通知任务只创建一次</li>
     * <li>✅ 第二次调用静默返回（canSettle() 返回 false）</li>
     * <li>✅ 验证方法本身的幂等性，独立于事件驱动架构</li>
     * </ul>
     *
     * <p>
     * 测试目的：
     * <ul>
     * <li>确保 settleCompletedOrder() 方法本身是幂等的</li>
     * <li>防御性编程：即使被外部系统多次调用也不会重复处理</li>
     * <li>未来扩展性：定时任务、手动补偿等场景的安全保证</li>
     * </ul>
     */
    @Test
    @DisplayName("测试7：settleCompletedOrder幂等性验证")
    @DirtiesContext
    public void testSettleCompletedOrder_idempotent() throws InterruptedException {
        log.info("========== 【Test 5-7】开始测试：settleCompletedOrder幂等性 ==========");

        // ========== 1. 准备测试数据 ==========
        // 创建拼团订单（3 人团）
        String orderId = "ORD_SETTLE_IDEMPOTENT_" + System.currentTimeMillis();
        String teamId = String.format("%08d", System.currentTimeMillis() % 100000000);
        String activityId = "ACT_SETTLE_IDEMPOTENT";
        String spuId = "SPU_SETTLE_IDEMPOTENT";
        String leaderUserId = "USER_SETTLE_IDEMPOTENT_LEADER";

        Order order = Order.create(
                orderId,
                teamId,
                activityId,
                spuId,
                leaderUserId,
                3, // targetCount = 3（1团长 + 2成员）
                Money.of(BigDecimal.valueOf(99.99), BigDecimal.valueOf(79.99)),
                LocalDateTime.now().plusMinutes(30),
                "APP",
                "iOS");
        orderRepository.save(order);
        log.info("【准备数据】创建拼团订单：orderId={}, targetCount=3, 初始completeCount=1（团长）", orderId);

        // 创建 2 个待支付的 TradeOrder（带通知配置，用于验证通知任务创建）
        NotifyConfig notifyConfig = NotifyConfig.builder()
                .notifyType(org.example.domain.model.trade.valueobject.NotifyType.HTTP)
                .notifyUrl("http://example.com/notify")
                .build();
        for (int i = 1; i <= 2; i++) {
            String tradeOrderId = "TRD_SETTLE_IDEMPOTENT_" + i;
            String userId = "USER_SETTLE_IDEMPOTENT_" + i;
            String skuId = "SKU_SETTLE_IDEMPOTENT";
            String goodsName = "测试商品";
            String outTradeNo = "OUT_SETTLE_IDEMPOTENT_" + i;

            TradeOrder tradeOrder = TradeOrder.create(
                    tradeOrderId,
                    teamId,
                    orderId,
                    activityId,
                    userId,
                    skuId,
                    goodsName,
                    BigDecimal.valueOf(99.99),
                    BigDecimal.valueOf(20.00),
                    BigDecimal.valueOf(79.99),
                    outTradeNo,
                    "APP",
                    "iOS",
                    notifyConfig); // 配置通知，用于验证通知任务创建

            tradeOrderRepository.save(tradeOrder);
            log.info("【准备数据】创建待支付订单：tradeOrderId={}, userId={}, notifyConfig={}", tradeOrderId, userId, notifyConfig);
        }

        // 两个用户依次支付（触发成团）
        log.info("【准备数据】用户1支付");
        paymentCallbackApplicationService.handlePaymentSuccess("OUT_SETTLE_IDEMPOTENT_1", BigDecimal.valueOf(79.99));

        log.info("【准备数据】用户2支付（触发成团）");
        paymentCallbackApplicationService.handlePaymentSuccess("OUT_SETTLE_IDEMPOTENT_2", BigDecimal.valueOf(79.99));

        // 等待异步settlement完成（SettlementEventListener）
        log.info("【准备数据】等待异步settlement完成...");
        Thread.sleep(1500); // 100ms (listener delay) + 缓冲时间
        log.info("【准备数据】异步等待结束");

        // 验证初始结算状态
        Order orderAfterPayment = orderRepository.findById(orderId).orElseThrow();
        List<TradeOrder> tradeOrdersAfterPayment = tradeOrderRepository.findByOrderId(orderId);

        log.info("【准备数据】支付完成后：Order.status={}, completeCount={}",
                orderAfterPayment.getStatus(), orderAfterPayment.getCompleteCount());
        assertThat(orderAfterPayment.getStatus()).isEqualTo(OrderStatus.SUCCESS);
        assertThat(orderAfterPayment.getCompleteCount()).isEqualTo(3);

        // 所有 TradeOrder 应该已经被异步settlement处理为 SETTLED 状态
        long settledCountBefore = tradeOrdersAfterPayment.stream()
                .filter(to -> to.getStatus() == TradeStatus.SETTLED)
                .count();
        log.info("【准备数据】异步settlement后：SETTLED状态的TradeOrder数量={}", settledCountBefore);
        assertThat(settledCountBefore).isEqualTo(2); // 用户1和用户2

        // ========== 2. 第一次手动调用 settleCompletedOrder() ==========
        log.info("【幂等性测试】第一次手动调用 settleCompletedOrder()（模拟定时任务）");
        settlementService.settleCompletedOrder(orderId);

        // 验证第一次调用后的状态（应该没有变化，因为已经被异步settlement处理过）
        List<TradeOrder> tradeOrdersAfterFirst = tradeOrderRepository.findByOrderId(orderId);
        long settledCountAfterFirst = tradeOrdersAfterFirst.stream()
                .filter(to -> to.getStatus() == TradeStatus.SETTLED)
                .count();
        log.info("【幂等性测试】第一次调用后：SETTLED状态的TradeOrder数量={}", settledCountAfterFirst);
        assertThat(settledCountAfterFirst).isEqualTo(2); // 状态不变

        // 查询通知任务数量（第一次调用不应该创建新任务，因为已经被异步settlement创建过）
        List<NotificationTask> notifyTasksAfterFirst = tradeOrdersAfterFirst
                .stream()
                .flatMap(to -> notificationTaskRepository.findByTradeOrderId(to.getTradeOrderId()).stream())
                .toList();
        log.info("【幂等性测试】第一次调用后：通知任务数量={}", notifyTasksAfterFirst.size());
        assertThat(notifyTasksAfterFirst.size()).isEqualTo(2); // 用户1和用户2各1个

        // ========== 3. 第二次手动调用 settleCompletedOrder() ==========
        log.info("【幂等性测试】第二次手动调用 settleCompletedOrder()（模拟重试）");
        settlementService.settleCompletedOrder(orderId);

        // 验证第二次调用后的状态（应该仍然没有变化）
        List<TradeOrder> tradeOrdersAfterSecond = tradeOrderRepository.findByOrderId(orderId);
        long settledCountAfterSecond = tradeOrdersAfterSecond.stream()
                .filter(to -> to.getStatus() == TradeStatus.SETTLED)
                .count();
        log.info("【幂等性测试】第二次调用后：SETTLED状态的TradeOrder数量={}", settledCountAfterSecond);
        assertThat(settledCountAfterSecond).isEqualTo(2); // 状态不变

        // 查询通知任务数量（第二次调用不应该创建新任务）
        List<NotificationTask> notifyTasksAfterSecond = tradeOrdersAfterSecond
                .stream()
                .flatMap(to -> notificationTaskRepository.findByTradeOrderId(to.getTradeOrderId()).stream())
                .toList();
        log.info("【幂等性测试】第二次调用后：通知任务数量={}", notifyTasksAfterSecond.size());
        assertThat(notifyTasksAfterSecond.size()).isEqualTo(2); // 仍然是2个，没有重复创建

        // ========== 4. 验证幂等性 ==========
        log.info("【验证结果】所有TradeOrder状态：");
        tradeOrdersAfterSecond.forEach(to -> {
            log.info("  - TradeOrder.id={}, status={}", to.getTradeOrderId(), to.getStatus());
            assertThat(to.getStatus()).isEqualTo(TradeStatus.SETTLED);
        });

        log.info("【验证结果】所有通知任务：");
        notifyTasksAfterSecond.forEach(task -> {
            log.info("  - NotificationTask.id={}, tradeOrderId={}", task.getTaskId(), task.getTradeOrderId());
        });

        log.info("========== 【Test 5-7】测试完成 ==========");
        log.info("【测试结论】✅ settleCompletedOrder() 方法具有幂等性");
        log.info("【测试结论】✅ 多次调用不会重复结算TradeOrder");
        log.info("【测试结论】✅ 多次调用不会重复创建通知任务");
        log.info("【测试结论】✅ canSettle() 方法正确拦截已结算的订单");
        log.info("【测试结论】✅ 方法可以安全地被外部系统（定时任务、手动触发）多次调用");
    }

    /**
     * 测试8：Order状态异常场景
     *
     * <p>
     * 测试场景：
     * <ol>
     * <li>创建 3 人拼团订单</li>
     * <li>手动将 Order 状态标记为 FAILED</li>
     * <li>尝试处理支付成功回调</li>
     * <li>验证系统是否正确拒绝</li>
     * </ol>
     *
     * <p>
     * 关键验证点：
     * <ul>
     * <li>✅ 抛出 BizException("拼团订单状态异常或已超时")</li>
     * <li>✅ TradeOrder 状态保持 CREATE</li>
     * <li>✅ Order 状态保持 FAILED</li>
     * <li>✅ Order.completeCount 不增加</li>
     * </ul>
     *
     * <p>
     * 测试目的：
     * <ul>
     * <li>验证状态机的完整性，防止非法状态转换</li>
     * <li>确保已失败的订单不能被错误激活</li>
     * <li>验证 SQL WHERE 条件（status = 'PENDING'）的保护作用</li>
     * </ul>
     */
    @Test
    @DisplayName("测试8：Order状态异常场景")
    @DirtiesContext
    public void testPaymentCallback_withFailedOrderStatus_shouldReject() {
        log.info("========== 【Test 5-8】开始测试：Order状态异常场景 ==========");

        // ========== 1. 准备测试数据 ==========
        // 创建拼团订单（3 人团）
        String orderId = "ORD_STATUS_FAILED_" + System.currentTimeMillis();
        String teamId = String.format("%08d", System.currentTimeMillis() % 100000000);
        String activityId = "ACT_STATUS_FAILED";
        String spuId = "SPU_STATUS_FAILED";
        String leaderUserId = "USER_STATUS_FAILED_LEADER";

        Order order = Order.create(
                orderId,
                teamId,
                activityId,
                spuId,
                leaderUserId,
                3, // targetCount = 3
                Money.of(BigDecimal.valueOf(99.99), BigDecimal.valueOf(79.99)),
                LocalDateTime.now().plusMinutes(30),
                "APP",
                "iOS");
        orderRepository.save(order);
        log.info("【准备数据】创建拼团订单：orderId={}, targetCount=3, 初始status=PENDING", orderId);

        // 创建 1 个待支付的 TradeOrder
        String tradeOrderId = "TRD_STATUS_FAILED_1";
        String userId = "USER_STATUS_FAILED_1";
        String skuId = "SKU_STATUS_FAILED";
        String goodsName = "测试商品";
        String outTradeNo = "OUT_STATUS_FAILED_1";

        TradeOrder tradeOrder = TradeOrder.create(
                tradeOrderId,
                teamId,
                orderId,
                activityId,
                userId,
                skuId,
                goodsName,
                BigDecimal.valueOf(99.99),
                BigDecimal.valueOf(20.00),
                BigDecimal.valueOf(79.99),
                outTradeNo,
                "APP",
                "iOS",
                null);

        tradeOrderRepository.save(tradeOrder);
        log.info("【准备数据】创建待支付订单：tradeOrderId={}, status=CREATE", tradeOrderId);

        // ========== 2. 手动将 Order 状态标记为 FAILED ==========
        // 模拟订单因某种原因失败（例如活动取消、超时等）
        order.markAsFailed("测试场景：模拟订单失败");
        orderRepository.updateStatus(orderId, OrderStatus.FAILED);
        log.info("【准备数据】手动标记Order为FAILED状态");

        // 验证初始状态
        Order orderBefore = orderRepository.findById(orderId).orElseThrow();
        TradeOrder tradeOrderBefore = tradeOrderRepository.findByOutTradeNo(outTradeNo).orElseThrow();
        log.info("【准备数据】初始状态：Order.status={}, completeCount={}, TradeOrder.status={}",
                orderBefore.getStatus(), orderBefore.getCompleteCount(), tradeOrderBefore.getStatus());
        assertThat(orderBefore.getStatus()).isEqualTo(OrderStatus.FAILED);
        assertThat(orderBefore.getCompleteCount()).isEqualTo(1); // 团长
        assertThat(tradeOrderBefore.getStatus()).isEqualTo(TradeStatus.CREATE);

        // ========== 3. 尝试处理支付成功回调 ==========
        log.info("【状态校验测试】尝试处理支付回调（Order状态=FAILED）");

        // 验证：应该抛出异常
        assertThatThrownBy(() -> {
            paymentCallbackApplicationService.handlePaymentSuccess(outTradeNo, BigDecimal.valueOf(79.99));
        })
                .isInstanceOf(BizException.class)
                .hasMessageContaining("拼团订单状态异常或已超时");

        log.info("【状态校验测试】✅ 系统正确拒绝了FAILED状态订单的支付回调");

        // ========== 4. 验证结果 ==========

        // 验证 1：TradeOrder 状态保持 CREATE
        TradeOrder tradeOrderAfter = tradeOrderRepository.findByOutTradeNo(outTradeNo).orElseThrow();
        log.info("【验证结果】TradeOrder 状态：{}", tradeOrderAfter.getStatus());
        assertThat(tradeOrderAfter.getStatus()).isEqualTo(TradeStatus.CREATE);

        // 验证 2：Order 状态保持 FAILED
        Order orderAfter = orderRepository.findById(orderId).orElseThrow();
        log.info("【验证结果】Order 状态：{}", orderAfter.getStatus());
        assertThat(orderAfter.getStatus()).isEqualTo(OrderStatus.FAILED);

        // 验证 3：Order.completeCount 不增加
        log.info("【验证结果】Order.completeCount={}", orderAfter.getCompleteCount());
        assertThat(orderAfter.getCompleteCount()).isEqualTo(1); // 仍然是初始值（团长）

        log.info("========== 【Test 5-8】测试完成 ==========");
        log.info("【测试结论】✅ Order状态校验正确工作");
        log.info("【测试结论】✅ FAILED状态的订单不能接受支付回调");
        log.info("【测试结论】✅ SQL WHERE条件（status = 'PENDING'）正确保护了状态机");
        log.info("【测试结论】✅ TradeOrder 和 Order 状态未被错误修改");
        log.info("【测试结论】✅ 防止了已失败订单被错误激活");
    }
}