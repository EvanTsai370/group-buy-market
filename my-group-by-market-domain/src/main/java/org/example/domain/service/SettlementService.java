package org.example.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.example.common.cache.RedisKeyManager;
import org.example.common.exception.BizException;
import org.example.domain.model.notification.NotificationTask;
import org.example.domain.model.notification.repository.NotificationTaskRepository;
import org.example.domain.model.order.Order;
import org.example.domain.model.order.repository.OrderRepository;
import org.example.domain.model.order.valueobject.OrderStatus;
import org.example.domain.model.trade.TradeOrder;
import org.example.domain.model.trade.repository.TradeOrderRepository;
import org.example.domain.service.lock.IDistributedLockService;
import org.example.domain.shared.IdGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 结算领域服务
 *
 * <p>
 * 职责：
 * <ul>
 * <li>协调Order聚合和TradeOrder聚合的结算操作</li>
 * <li>处理支付成功后的业务逻辑</li>
 * <li>处理拼团成功后的结算逻辑</li>
 * </ul>
 *
 * <p>
 * 业务流程：
 * <ol>
 * <li>支付成功流程：TradeOrder标记为PAID → Order增加completeCount → 检查是否成团</li>
 * <li>拼团成功流程：批量将该Order下所有PAID状态的TradeOrder标记为SETTLED</li>
 * </ol>
 *
 */
@Slf4j
public class SettlementService {

    private final OrderRepository orderRepository;
    private final TradeOrderRepository tradeOrderRepository;
    private final NotificationTaskRepository notificationTaskRepository;
    private final IdGenerator idGenerator;
    private final ResourceReleaseService resourceReleaseService;
    private final IDistributedLockService lockService;

    public SettlementService(OrderRepository orderRepository,
            TradeOrderRepository tradeOrderRepository,
            NotificationTaskRepository notificationTaskRepository,
            IdGenerator idGenerator,
            ResourceReleaseService resourceReleaseService,
            IDistributedLockService lockService) {
        this.orderRepository = orderRepository;
        this.tradeOrderRepository = tradeOrderRepository;
        this.notificationTaskRepository = notificationTaskRepository;
        this.idGenerator = idGenerator;
        this.resourceReleaseService = resourceReleaseService;
        this.lockService = lockService;
    }

    /**
     * 处理支付成功（通过外部交易单号）
     *
     * <p>
     * 供支付宝回调使用，包含幂等性检查和金额校验
     *
     * @param outTradeNo     外部交易单号
     * @param callbackAmount 回调金额（用于防御性校验）
     */
    public void handlePaymentSuccessByOutTradeNo(String outTradeNo, BigDecimal callbackAmount) {
        log.info("【结算服务】处理支付成功（外部单号）, outTradeNo: {}, callbackAmount: {}", outTradeNo, callbackAmount);

        // 1. 根据outTradeNo查找交易订单
        Optional<TradeOrder> tradeOrderOpt = tradeOrderRepository.findByOutTradeNo(outTradeNo);
        if (tradeOrderOpt.isEmpty()) {
            throw new BizException("交易订单不存在, outTradeNo: " + outTradeNo);
        }
        TradeOrder tradeOrder = tradeOrderOpt.get();

        // 2. 幂等性检查：已支付或已结算则静默返回（不是错误，是正常重复回调）
        if (tradeOrder.isPaid() || tradeOrder.isSettled()) {
            log.info("【结算服务】订单已处理，跳过重复回调, outTradeNo: {}, status: {}",
                    outTradeNo, tradeOrder.getStatus());
            return;
        }

        // 3. 金额校验（防御性编程，阻断金额不一致的支付）
        if (callbackAmount != null && callbackAmount.compareTo(tradeOrder.getPayPrice()) != 0) {
            log.error("【结算服务】严重安全告警：支付金额不匹配！订单金额: {}, 实付金额: {}, outTradeNo: {}",
                    tradeOrder.getPayPrice(), callbackAmount, outTradeNo);
            // 阻断业务处理，防止恶意篡改金额
            throw new BizException("支付金额异常，请联系客服");
        }

        // 4. 调用核心结算逻辑
        handlePaymentSuccess(tradeOrder.getTradeOrderId());
    }

    /**
     * 处理支付失败/交易关闭（通过外部交易单号）
     *
     * <p>
     * 供支付宝回调使用（TRADE_CLOSED 状态）
     * 
     * <p>
     * 业务场景：
     * <ul>
     * <li>用户未支付，交易超时关闭</li>
     * <li>用户支付后全额退款，交易关闭</li>
     * </ul>
     * 
     * <p>
     * 处理逻辑：
     * <ol>
     * <li>查找交易订单</li>
     * <li>幂等性检查：已超时/已退款则静默返回</li>
     * <li>标记为超时并释放资源（名额+库存）</li>
     * </ol>
     *
     * @param outTradeNo 外部交易单号
     */
    public void handlePaymentFailedByOutTradeNo(String outTradeNo) {
        log.info("【结算服务】处理支付失败/交易关闭（外部单号）, outTradeNo: {}", outTradeNo);

        // 1. 根据outTradeNo查找交易订单
        Optional<TradeOrder> tradeOrderOpt = tradeOrderRepository.findByOutTradeNo(outTradeNo);
        if (tradeOrderOpt.isEmpty()) {
            log.warn("【结算服务】交易订单不存在，可能已手动处理, outTradeNo: {}", outTradeNo);
            return;
        }
        TradeOrder tradeOrder = tradeOrderOpt.get();

        // 2. 幂等性检查：如果已是终态则静默返回
        if (tradeOrder.isTimeout() || tradeOrder.isRefunded()) {
            log.info("【结算服务】订单已处理（超时/退款），跳过重复处理, outTradeNo: {}, status: {}",
                    outTradeNo, tradeOrder.getStatus());
            return;
        }

        // 3. 只处理 CREATE 状态的订单（未支付）
        // 已支付订单的 TRADE_CLOSED 需要走退款流程，这里只处理未支付情况
        if (tradeOrder.isCreated()) {
            log.info("【结算服务】未支付订单交易关闭，标记为超时并释放资源, outTradeNo: {}", outTradeNo);
            tradeOrder.markAsTimeout();
            tradeOrderRepository.update(tradeOrder);

            // 4. 释放全部预占资源（委托给 ResourceReleaseService）
            resourceReleaseService.releaseAllResources(
                    tradeOrder.getOrderId(),
                    tradeOrder.getActivityId(),
                    tradeOrder.getSkuId(),
                    tradeOrder.getUserId(),
                    tradeOrder.getTradeOrderId(),
                    "交易关闭回调");
        } else {
            log.warn("【结算服务】非 CREATE 状态的订单收到 TRADE_CLOSED，需人工检查, outTradeNo: {}, status: {}",
                    outTradeNo, tradeOrder.getStatus());
        }
    }

    /**
     * 处理支付成功
     *
     * <p>
     * 业务流程：
     * <ol>
     * <li>获取分布式锁（与退款使用相同锁，确保互斥）</li>
     * <li>加载TradeOrder，校验状态</li>
     * <li>加载Order，用于时间校验</li>
     * <li>执行增强的结算校验（状态+渠道+时间）</li>
     * <li>标记TradeOrder为PAID</li>
     * <li>原子增加Order的completeCount（SQL层）</li>
     * <li>重新加载Order以同步最新状态（关键！）</li>
     * <li>如果成团，触发拼团成功流程</li>
     * </ol>
     *
     * <p>
     * 并发控制设计（遵循CONCURRENCY.md）：
     * <ul>
     * <li>使用分布式锁防止与超时处理并发（Race #9修复）</li>
     * <li>使用 SQL 原子更新避免乐观锁误杀</li>
     * <li>SQL 更新后立即 Reload 聚合根同步状态</li>
     * <li>基于最新状态触发领域事件</li>
     * <li>口诀："SQL 写完必 Reload，事件发布看内存"</li>
     * </ul>
     *
     * @param tradeOrderId 交易订单ID
     */
    public void handlePaymentSuccess(String tradeOrderId) {
        // 1. 分布式锁：防止与超时处理并发（Race #9修复）
        // 使用与RefundService相同的锁key，确保支付回调和超时处理互斥
        String lockKey = RedisKeyManager.lockKey("refund", tradeOrderId);

        try {
            // 尝试获取锁（30秒超时）
            boolean lockAcquired = lockService.tryLock(lockKey, 0, 30, TimeUnit.SECONDS);
            if (!lockAcquired) {
                log.warn("【结算服务】订单处理中，请稍后重试, tradeOrderId={}", tradeOrderId);
                throw new BizException("订单处理中，请稍后重试");
            }

            try {
                // 2. 加载TradeOrder
                Optional<TradeOrder> tradeOrderOpt = tradeOrderRepository.findByTradeOrderId(tradeOrderId);
                if (tradeOrderOpt.isEmpty()) {
                    throw new BizException("交易订单不存在");
                }
                TradeOrder tradeOrder = tradeOrderOpt.get();

                // 3. 幂等性检查：已支付、已结算或已超时则静默返回
                if (tradeOrder.isPaid() || tradeOrder.isSettled() || tradeOrder.isTimeout()) {
                    log.info("【结算服务】订单已处理，跳过重复处理, tradeOrderId: {}, status: {}",
                            tradeOrderId, tradeOrder.getStatus());
                    return;
                }

                // 4. 加载Order（用于时间校验）
                String orderId = tradeOrder.getOrderId();
                Optional<Order> orderOpt = orderRepository.findById(orderId);
                if (orderOpt.isEmpty()) {
                    throw new BizException("拼团订单不存在");
                }
                Order order = orderOpt.get();

                // 5. 获取渠道黑名单（TODO: 可以从配置中心或数据库加载）
                // 目前暂不启用渠道黑名单，传空集合
                Set<String> blacklistedChannels = Set.of();

                // 6. 校验是否可以支付（增强版：状态+渠道+时间）
                tradeOrder.validatePayment(order, blacklistedChannels);

                // 7. 先原子增加Order的completeCount（SQL原子操作，防止超卖）
                // 【重要】此顺序确保竞争失败的线程在此步骤就失败，不会污染TradeOrder状态
                int newCompleteCount = orderRepository.tryIncrementCompleteCount(orderId);
                if (newCompleteCount == -1) {
                    throw new BizException("拼团订单状态异常或已超时");
                }

                // 8. 再标记为已支付（此时已确认有名额，可以安全地标记）
                tradeOrder.markAsPaid(LocalDateTime.now());
                tradeOrderRepository.update(tradeOrder);

                log.info("【结算服务】支付成功，拼团进度更新, tradeOrderId: {}, orderId: {}, completeCount: {}",
                        tradeOrderId, orderId, newCompleteCount);

                // 9. 重新加载Order以同步最新状态（关键！遵循CONCURRENCY.md）
                order = orderRepository.findById(orderId)
                        .orElseThrow(() -> new BizException("拼团订单不存在"));

                log.debug("【结算服务】重新加载Order, orderId: {}, status: {}, completeCount: {}/{}",
                        orderId, order.getStatus(), order.getCompleteCount(), order.getTargetCount());

                // 10. 结算流程完全依赖事件驱动（SettlementEventListener）
                // 移除同步调用，避免与异步事件监听器重复执行
                if (order.getStatus() == OrderStatus.SUCCESS) {
                    log.info("【结算服务】拼团成功，将由SettlementEventListener异步处理结算, orderId: {}", orderId);
                }

            } finally {
                // 释放锁
                lockService.unlock(lockKey);
            }

        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("【结算服务】支付处理失败, tradeOrderId={}", tradeOrderId, e);
            throw new BizException("支付处理失败: " + e.getMessage());
        }
    }

    /**
     * 结算已完成的拼团订单
     *
     * <p>
     * 业务流程：
     * <ol>
     * <li>查询该Order下所有PAID状态的TradeOrder</li>
     * <li>批量标记为SETTLED</li>
     * <li>生成通知任务（如果配置了通知）</li>
     * </ol>
     *
     * @param orderId 拼团订单ID
     */
    public void settleCompletedOrder(String orderId) {
        // 1. 查询Order下所有TradeOrder
        List<TradeOrder> tradeOrders = tradeOrderRepository.findByOrderId(orderId);

        LocalDateTime settlementTime = LocalDateTime.now();
        int settledCount = 0;

        // 2. 批量结算
        for (TradeOrder tradeOrder : tradeOrders) {
            if (tradeOrder.canSettle()) {
                tradeOrder.markAsSettled(settlementTime);
                tradeOrderRepository.update(tradeOrder);
                settledCount++;

                // 3. 生成通知任务（如果配置了通知）
                if (tradeOrder.needNotify()) {
                    createNotificationTask(tradeOrder);
                }

                log.info("【结算服务】交易订单已结算, tradeOrderId: {}, orderId: {}",
                        tradeOrder.getTradeOrderId(), orderId);
            }
        }

        log.info("【结算服务】拼团订单结算完成, orderId: {}, settledCount: {}", orderId, settledCount);
    }

    /**
     * 创建通知任务
     *
     * @param tradeOrder 交易订单
     */
    private void createNotificationTask(TradeOrder tradeOrder) {
        String taskId = "NOTIFY-" + idGenerator.nextId();
        NotificationTask task = NotificationTask.create(
                taskId,
                tradeOrder.getTradeOrderId(),
                tradeOrder.getNotifyConfig());
        notificationTaskRepository.save(task);

        log.info("【结算服务】创建通知任务, taskId: {}, tradeOrderId: {}",
                taskId, tradeOrder.getTradeOrderId());
    }

    /**
     * 批量结算超时订单（供定时任务调用）
     *
     * <p>
     * 用途：定时扫描超时的Order，如果支持虚拟成团则进行结算
     *
     * @param orderIds 订单ID列表
     */
    public void batchSettleOrders(List<String> orderIds) {
        for (String orderId : orderIds) {
            try {
                settleCompletedOrder(orderId);
            } catch (Exception e) {
                log.error("【结算服务】结算订单失败, orderId: {}", orderId, e);
            }
        }
    }
}
