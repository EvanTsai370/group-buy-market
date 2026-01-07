package org.example.domain.service.refund;

import lombok.extern.slf4j.Slf4j;
import org.example.common.util.LogDesensitizer;
import org.example.domain.model.order.Order;
import org.example.domain.model.order.repository.OrderRepository;
import org.example.domain.model.trade.TradeOrder;
import org.example.domain.model.trade.valueobject.TradeStatus;

import java.util.Optional;

/**
 * 已支付退单策略
 *
 * <p>
 * 适用场景：用户已支付但拼团失败，需要调用支付网关退款
 *
 * <p>
 * 处理逻辑：
 * <ol>
 * <li>标记 TradeOrder 为 REFUND 状态</li>
 * <li>原子递减 Order 的 lockCount（释放锁定名额）</li>
 * <li>调用支付网关退款接口（同步/异步）</li>
 * <li>记录退款流水（用于对账）</li>
 * </ol>
 *
 * <p>
 * 注意事项：
 * <ul>
 * <li>退款操作应保证幂等性（基于 tradeOrderId 去重）</li>
 * <li>支付网关可能返回异步结果，需处理回调</li>
 * <li>退款失败时应记录日志并触发告警，人工介入处理</li>
 * </ul>
 *
 * <p>
 * TODO：集成真实支付网关（当前为模拟实现）
 *
 * @author 开发团队
 * @since 2026-01-06
 */
@Slf4j
public class PaidRefundStrategy implements RefundStrategy {

    private final OrderRepository orderRepository;

    public PaidRefundStrategy(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public void execute(TradeOrder tradeOrder) throws Exception {
        log.info("【已支付退单策略】开始执行, tradeOrderId={}, payPrice={}",
                tradeOrder.getTradeOrderId(), LogDesensitizer.maskPrice(tradeOrder.getPayPrice(), log));

        // 1. 标记为退单
        tradeOrder.markAsRefund();

        // 2. 释放 Order 的锁定名额（原子递减 lockCount）
        String orderId = tradeOrder.getOrderId();
        boolean success = orderRepository.decrementLockCount(orderId);

        if (!success) {
            log.warn("【已支付退单策略】释放锁定名额失败，可能 lockCount 已为 0, orderId={}", orderId);
        }

        // 3. 更新 Order 聚合的内存状态（可选，确保数据一致性）
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            order.onReleaseLockSuccess(Math.max(0, order.getLockCount() - 1));
        }

        // 4. 调用支付网关退款（当前为模拟实现）
        callPaymentGatewayRefund(tradeOrder);

        log.info("【已支付退单策略】执行成功, tradeOrderId={}, orderId={}",
                tradeOrder.getTradeOrderId(), orderId);
    }

    @Override
    public boolean supports(TradeOrder tradeOrder) {
        // 仅支持 PAID 状态的订单（已支付）
        return tradeOrder.getStatus() == TradeStatus.PAID;
    }

    /**
     * 调用支付网关退款接口（模拟实现）
     *
     * <p>
     * 真实实现需要：
     * <ul>
     * <li>根据 outTradeNo 调用支付宝/微信退款 API</li>
     * <li>处理退款结果（同步/异步）</li>
     * <li>记录退款流水到数据库</li>
     * <li>处理退款失败的补偿逻辑</li>
     * </ul>
     *
     * @param tradeOrder 交易订单
     */
    private void callPaymentGatewayRefund(TradeOrder tradeOrder) {
        log.info("【支付网关退款】模拟调用退款接口, tradeOrderId={}, outTradeNo={}, amount={}",
                tradeOrder.getTradeOrderId(),
                tradeOrder.getOutTradeNo(),
                LogDesensitizer.maskPrice(tradeOrder.getPayPrice(), log));

        // TODO: 集成真实支付网关
        // 示例：
        // RefundRequest request = new RefundRequest();
        // request.setOutTradeNo(tradeOrder.getOutTradeNo());
        // request.setRefundAmount(tradeOrder.getPayPrice());
        // RefundResponse response = paymentGateway.refund(request);
        //
        // if (!response.isSuccess()) {
        // throw new BizException("支付网关退款失败: " + response.getErrorMsg());
        // }

        log.info("【支付网关退款】退款成功（模拟）, tradeOrderId={}", tradeOrder.getTradeOrderId());
    }
}
