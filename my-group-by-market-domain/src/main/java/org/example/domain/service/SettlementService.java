package org.example.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.example.common.exception.BizException;
import org.example.domain.model.notification.NotificationTask;
import org.example.domain.model.notification.repository.NotificationTaskRepository;
import org.example.domain.model.order.Order;
import org.example.domain.model.order.repository.OrderRepository;
import org.example.domain.model.order.valueobject.OrderStatus;
import org.example.domain.model.trade.TradeOrder;
import org.example.domain.model.trade.repository.TradeOrderRepository;
import org.example.domain.shared.IdGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
 * @author 开发团队
 * @since 2026-01-04
 */
@Slf4j
public class SettlementService {

    private final OrderRepository orderRepository;
    private final TradeOrderRepository tradeOrderRepository;
    private final NotificationTaskRepository notificationTaskRepository;
    private final IdGenerator idGenerator;

    public SettlementService(OrderRepository orderRepository,
            TradeOrderRepository tradeOrderRepository,
            NotificationTaskRepository notificationTaskRepository,
            IdGenerator idGenerator) {
        this.orderRepository = orderRepository;
        this.tradeOrderRepository = tradeOrderRepository;
        this.notificationTaskRepository = notificationTaskRepository;
        this.idGenerator = idGenerator;
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

        // 3. 金额校验（防御性编程，告警但不阻断）
        if (callbackAmount != null && callbackAmount.compareTo(tradeOrder.getPayPrice()) != 0) {
            log.error("【结算服务】金额不匹配！expected: {}, actual: {}, outTradeNo: {}",
                    tradeOrder.getPayPrice(), callbackAmount, outTradeNo);
            // 继续处理，但记录告警（可接入监控系统）
        }

        // 4. 调用核心结算逻辑
        handlePaymentSuccess(tradeOrder.getTradeOrderId());
    }

    /**
     * 处理支付成功
     *
     * <p>
     * 业务流程：
     * <ol>
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
     * <li>使用 SQL 原子更新避免乐观锁误杀</li>
     * <li>SQL 更新后立即 Reload 聚合根同步状态</li>
     * <li>基于最新状态触发领域事件</li>
     * <li>口诀："SQL 写完必 Reload，事件发布看内存"</li>
     * </ul>
     *
     * @param tradeOrderId 交易订单ID
     */
    public void handlePaymentSuccess(String tradeOrderId) {
        // 1. 加载TradeOrder
        Optional<TradeOrder> tradeOrderOpt = tradeOrderRepository.findByTradeOrderId(tradeOrderId);
        if (tradeOrderOpt.isEmpty()) {
            throw new BizException("交易订单不存在");
        }
        TradeOrder tradeOrder = tradeOrderOpt.get();

        // 2. 幂等性检查：已支付或已结算则静默返回
        if (tradeOrder.isPaid() || tradeOrder.isSettled()) {
            log.info("【结算服务】订单已处理，跳过重复处理, tradeOrderId: {}, status: {}",
                    tradeOrderId, tradeOrder.getStatus());
            return;
        }

        // 3. 加载Order（用于时间校验）
        String orderId = tradeOrder.getOrderId();
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            throw new BizException("拼团订单不存在");
        }
        Order order = orderOpt.get();

        // 4. 获取渠道黑名单（TODO: 可以从配置中心或数据库加载）
        // 目前暂不启用渠道黑名单，传空集合
        Set<String> blacklistedChannels = Set.of();

        // 5. 校验是否可以支付（增强版：状态+渠道+时间）
        tradeOrder.validatePayment(order, blacklistedChannels);

        // 6. 标记为已支付
        tradeOrder.markAsPaid(LocalDateTime.now());
        tradeOrderRepository.update(tradeOrder);

        // 7. 原子增加Order的completeCount（数据库层并发控制）
        int newCompleteCount = orderRepository.tryIncrementCompleteCount(orderId);
        if (newCompleteCount == -1) {
            throw new BizException("拼团订单状态异常或已超时");
        }

        log.info("【结算服务】支付成功，拼团进度更新, tradeOrderId: {}, orderId: {}, completeCount: {}",
                tradeOrderId, orderId, newCompleteCount);

        // 8. 重新加载Order以同步最新状态（关键！遵循CONCURRENCY.md）
        order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BizException("拼团订单不存在"));

        log.debug("【结算服务】重新加载Order, orderId: {}, status: {}, completeCount: {}/{}",
                orderId, order.getStatus(), order.getCompleteCount(), order.getTargetCount());

        // 9. 基于最新状态触发结算流程
        if (order.getStatus() == OrderStatus.SUCCESS) {
            log.info("【结算服务】拼团成功，开始结算, orderId: {}", orderId);
            settleCompletedOrder(orderId);
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
