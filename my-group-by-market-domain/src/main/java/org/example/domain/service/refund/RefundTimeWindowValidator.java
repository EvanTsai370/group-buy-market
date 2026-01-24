package org.example.domain.service.refund;

import lombok.extern.slf4j.Slf4j;
import org.example.common.exception.BizException;
import org.example.domain.model.order.Order;
import org.example.domain.model.order.repository.OrderRepository;
import org.example.domain.model.trade.TradeOrder;
import org.example.domain.model.trade.valueobject.TradeStatus;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 退款时间窗口验证器
 *
 * <p>
 * 职责：验证不同状态订单的退款时间窗口
 *
 * <p>
 * 业务规则：
 * <ul>
 * <li>CREATE状态：30分钟内可退（与超时时间一致）</li>
 * <li>PAID状态：活动截止前可退</li>
 * <li>SETTLED状态：不允许用户主动退款</li>
 * <li>TIMEOUT/REFUND状态：已经是终态，不允许退款</li>
 * </ul>
 *
 */
@Slf4j
public class RefundTimeWindowValidator {

    /** 未支付订单超时时间（30分钟） */
    private static final int UNPAID_TIMEOUT_MINUTES = 30;

    private final OrderRepository orderRepository;

    public RefundTimeWindowValidator(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * 验证是否在退款时间窗口内
     *
     * @param tradeOrder 交易订单
     * @throws BizException 如果不在退款时间窗口内
     */
    public void validate(TradeOrder tradeOrder) {
        TradeStatus status = tradeOrder.getStatus();
        LocalDateTime createTime = tradeOrder.getCreateTime();
        LocalDateTime now = LocalDateTime.now();

        switch (status) {
            case CREATE:
                // 未支付订单：30分钟内可退款（超过30分钟会自动退单）
                long minutesSinceCreate = Duration.between(createTime, now).toMinutes();
                if (minutesSinceCreate > UNPAID_TIMEOUT_MINUTES) {
                    log.warn("【退款时间窗口】未支付订单超过30分钟，不支持退款, tradeOrderId: {}, createTime: {}",
                            tradeOrder.getTradeOrderId(), createTime);
                    throw new BizException("订单创建超过30分钟，不支持退款");
                }
                break;

            case PAID:
                // 已支付未成团：活动截止前可退款
                Order order = orderRepository.findById(tradeOrder.getOrderId())
                        .orElseThrow(() -> new BizException("拼团订单不存在"));

                if (now.isAfter(order.getDeadlineTime())) {
                    log.warn("【退款时间窗口】已支付订单活动已截止，不支持退款, tradeOrderId: {}, deadlineTime: {}",
                            tradeOrder.getTradeOrderId(), order.getDeadlineTime());
                    throw new BizException("活动已截止，不支持退款");
                }
                break;

            case SETTLED:
                // 已结算订单：不允许用户主动退款
                log.warn("【退款时间窗口】订单已结算，不支持退款, tradeOrderId: {}", tradeOrder.getTradeOrderId());
                throw new BizException("订单已结算，不支持退款");

            case TIMEOUT:
                // 超时订单：系统已自动处理
                log.warn("【退款时间窗口】订单已超时，系统将自动处理, tradeOrderId: {}", tradeOrder.getTradeOrderId());
                throw new BizException("订单已超时，系统将自动处理");

            case REFUND:
                // 已退款订单：幂等性处理
                log.warn("【退款时间窗口】订单已退款, tradeOrderId: {}", tradeOrder.getTradeOrderId());
                // 不抛异常，返回成功（幂等性）
                break;

            default:
                log.error("【退款时间窗口】未知订单状态, tradeOrderId: {}, status: {}",
                        tradeOrder.getTradeOrderId(), status);
                throw new BizException("订单状态异常");
        }

        log.debug("【退款时间窗口】验证通过, tradeOrderId: {}, status: {}",
                tradeOrder.getTradeOrderId(), status);
    }

    /**
     * 检查是否可以退款（不抛异常版本）
     *
     * @param tradeOrder 交易订单
     * @return true=可以退款, false=不可以退款
     */
    public boolean canRefund(TradeOrder tradeOrder) {
        try {
            validate(tradeOrder);
            return true;
        } catch (BizException e) {
            return false;
        }
    }
}
