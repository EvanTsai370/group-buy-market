package org.example.application.service.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.event.PaymentCompletedEvent;
import org.example.domain.model.order.Order;
import org.example.domain.model.order.repository.OrderRepository;
import org.example.domain.model.trade.TradeOrder;
import org.example.domain.model.trade.repository.TradeOrderRepository;
import org.example.domain.service.SettlementService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付回调应用服务
 * <p>
 * 职责：
 * <ul>
 * <li>管理事务边界（@Transactional）</li>
 * <li>协调领域服务调用</li>
 * <li>发布领域事件（PaymentCompletedEvent）</li>
 * <li>确保支付回调处理的原子性</li>
 * </ul>
 * <p>
 * 架构说明：
 * <ul>
 * <li>Application层负责事务管理和事件发布</li>
 * <li>Domain层保持纯粹（无Spring依赖）</li>
 * <li>Controller和Test都通过此服务调用</li>
 * </ul>
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCallbackApplicationService {

    private final SettlementService settlementService;
    private final TradeOrderRepository tradeOrderRepository;
    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 处理支付成功回调（通过外部交易单号）
     * <p>
     * 事务边界：此方法确保以下操作的原子性：
     * <ol>
     * <li>幂等性检查</li>
     * <li>金额校验</li>
     * <li>原子增加 completeCount</li>
     * <li>标记 TradeOrder 为 PAID</li>
     * <li>发布 PaymentCompletedEvent（事务提交后异步处理settlement）</li>
     * <li>触发同步settlement（作为fallback）</li>
     * </ol>
     *
     * @param outTradeNo     外部交易单号
     * @param callbackAmount 回调金额（用于防御性校验）
     * @throws org.example.common.exception.BizException 业务异常（幂等性、金额不符、订单不存在等）
     */
    @Transactional(rollbackFor = Exception.class)
    public void handlePaymentSuccess(String outTradeNo, BigDecimal callbackAmount) {
        log.info("【PaymentCallbackApplicationService】处理支付成功回调, outTradeNo: {}, amount: {}",
                outTradeNo, callbackAmount);

        // 1. 调用领域服务处理支付成功
        settlementService.handlePaymentSuccessByOutTradeNo(outTradeNo, callbackAmount);

        // 2. 发布支付完成事件（在当前事务中，事务提交后异步处理settlement）
        // 查询TradeOrder和Order信息用于事件
        TradeOrder tradeOrder = tradeOrderRepository.findByOutTradeNo(outTradeNo)
                .orElseThrow(() -> new RuntimeException("TradeOrder not found"));
        Order order = orderRepository.findById(tradeOrder.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        PaymentCompletedEvent event = new PaymentCompletedEvent(
                tradeOrder.getTradeOrderId(),
                tradeOrder.getOrderId(),
                tradeOrder.getUserId(),
                order.getCompleteCount(),
                order.getTargetCount(),
                LocalDateTime.now());
        eventPublisher.publishEvent(event);
        log.info("【PaymentCallbackApplicationService】发布支付完成事件, orderId: {}, completeCount: {}/{}",
                order.getOrderId(), order.getCompleteCount(), order.getTargetCount());

        log.info("【PaymentCallbackApplicationService】支付回调处理完成, outTradeNo: {}", outTradeNo);
    }

    /**
     * 处理支付失败/交易关闭回调
     *
     * <p>
     * 业务场景：
     * <ul>
     * <li>交易超时关闭 (TRADE_CLOSED)</li>
     * <li>用户未支付导致交易关闭</li>
     * </ul>
     *
     * <p>
     * 处理逻辑：
     * <ol>
     * <li>标记订单为超时状态</li>
     * <li>释放预占资源（名额+库存）</li>
     * </ol>
     *
     * @param outTradeNo 外部交易单号
     */
    @Transactional(rollbackFor = Exception.class)
    public void handlePaymentFailure(String outTradeNo) {
        log.info("【PaymentCallbackApplicationService】处理支付失败/交易关闭, outTradeNo: {}", outTradeNo);

        settlementService.handlePaymentFailedByOutTradeNo(outTradeNo);

        log.info("【PaymentCallbackApplicationService】支付失败处理完成, outTradeNo: {}", outTradeNo);
    }
}
