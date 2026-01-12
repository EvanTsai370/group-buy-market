package org.example.domain.service.refund;

import lombok.extern.slf4j.Slf4j;
import org.example.common.exception.BizException;
import org.example.common.util.LogDesensitizer;
import org.example.domain.gateway.IPaymentRefundGateway;
import org.example.domain.model.order.Order;
import org.example.domain.model.order.repository.OrderRepository;
import org.example.domain.model.trade.TradeOrder;
import org.example.domain.model.trade.valueobject.TradeStatus;
import org.example.domain.service.ResourceReleaseService;

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
 * <li>根据成团状态决定是否释放名额</li>
 * <li>释放库存</li>
 * <li>调用支付网关退款接口</li>
 * </ol>
 *
 * @author 开发团队
 * @since 2026-01-06
 */
@Slf4j
public class PaidRefundStrategy implements RefundStrategy {

    private final OrderRepository orderRepository;
    private final IPaymentRefundGateway paymentRefundGateway;
    private final ResourceReleaseService resourceReleaseService;

    public PaidRefundStrategy(
            OrderRepository orderRepository,
            IPaymentRefundGateway paymentRefundGateway,
            ResourceReleaseService resourceReleaseService) {
        this.orderRepository = orderRepository;
        this.paymentRefundGateway = paymentRefundGateway;
        this.resourceReleaseService = resourceReleaseService;
    }

    @Override
    public void execute(TradeOrder tradeOrder) throws Exception {
        log.info("【已支付退单策略】开始执行, tradeOrderId={}, payPrice={}",
                tradeOrder.getTradeOrderId(), LogDesensitizer.maskPrice(tradeOrder.getPayPrice(), log));

        // 1. 标记为退单
        tradeOrder.markAsRefund("已支付拼团失败退款");

        String orderId = tradeOrder.getOrderId();

        // 2. 加载Order，判断是否已成团
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BizException("拼团订单不存在"));

        // 3. 根据成团状态决定是否恢复名额
        if (order.isCompleted()) {
            // 已成团：只释放库存
            log.info("【已支付退单策略】已成团，不恢复名额，仅释放库存, orderId: {}", orderId);
            resourceReleaseService.releaseInventory(
                    tradeOrder.getGoodsId(),
                    tradeOrder.getTradeOrderId(),
                    "已支付退单-已成团");
        } else {
            // 未成团：释放全部资源（lockCount + 槽位 + 库存 + 参团次数）
            resourceReleaseService.releaseAllResources(
                    orderId,
                    tradeOrder.getActivityId(),
                    tradeOrder.getGoodsId(),
                    tradeOrder.getUserId(),
                    tradeOrder.getTradeOrderId(),
                    "已支付退单-未成团");
        }

        // 4. 调用支付网关退款（无论是否已成团都需要退款）
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
     * 调用支付网关退款
     *
     * <p>
     * 当前实现：同步调用支付网关
     * TODO：后续可改为异步处理，支持重试
     *
     * @param tradeOrder 交易订单
     */
    private void callPaymentGatewayRefund(TradeOrder tradeOrder) {
        try {
            String refundReason = "团购活动退款-" + tradeOrder.getTradeOrderId();
            String outRequestNo = "REFUND-" + tradeOrder.getTradeOrderId();

            IPaymentRefundGateway.RefundResult result = paymentRefundGateway.refund(
                    tradeOrder.getOutTradeNo(),
                    tradeOrder.getPayPrice(),
                    refundReason,
                    outRequestNo);

            if (result.success()) {
                log.info("【已支付退单策略】支付网关退款成功, tradeOrderId={}, refundId={}",
                        tradeOrder.getTradeOrderId(), result.refundId());
            } else {
                log.error("【已支付退单策略】支付网关退款失败, tradeOrderId={}, errorCode={}, errorMsg={}",
                        tradeOrder.getTradeOrderId(), result.errorCode(), result.errorMsg());
                throw new BizException("支付网关退款失败: " + result.errorMsg());
            }

        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("【已支付退单策略】支付网关退款异常, tradeOrderId={}",
                    tradeOrder.getTradeOrderId(), e);
            throw new RuntimeException("支付网关退款异常: " + e.getMessage(), e);
        }
    }
}
