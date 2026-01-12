package org.example.domain.service.refund;

import lombok.extern.slf4j.Slf4j;
import org.example.domain.model.trade.TradeOrder;
import org.example.domain.model.trade.valueobject.TradeStatus;
import org.example.domain.service.ResourceReleaseService;

/**
 * 未支付退单策略
 *
 * <p>
 * 适用场景：用户下单后超时未支付，需要释放锁定的拼团名额
 *
 * <p>
 * 处理逻辑：
 * <ol>
 * <li>标记 TradeOrder 为 TIMEOUT 状态</li>
 * <li>委托 ResourceReleaseService 释放全部资源</li>
 * <li>无需调用支付网关退款（用户未支付）</li>
 * </ol>
 *
 * @author 开发团队
 * @since 2026-01-06
 */
@Slf4j
public class UnpaidRefundStrategy implements RefundStrategy {

    private final ResourceReleaseService resourceReleaseService;

    public UnpaidRefundStrategy(ResourceReleaseService resourceReleaseService) {
        this.resourceReleaseService = resourceReleaseService;
    }

    @Override
    public void execute(TradeOrder tradeOrder) throws Exception {
        log.info("【未支付退单策略】开始执行, tradeOrderId={}, status={}",
                tradeOrder.getTradeOrderId(), tradeOrder.getStatus());

        // 1. 标记为超时
        tradeOrder.markAsTimeout();

        // 2. 释放全部预占资源（委托给 ResourceReleaseService）
        resourceReleaseService.releaseAllResources(
                tradeOrder.getOrderId(),
                tradeOrder.getActivityId(),
                tradeOrder.getGoodsId(),
                tradeOrder.getTradeOrderId(),
                "未支付退单");

        log.info("【未支付退单策略】执行成功, tradeOrderId={}, orderId={}",
                tradeOrder.getTradeOrderId(), tradeOrder.getOrderId());
    }

    @Override
    public boolean supports(TradeOrder tradeOrder) {
        // 仅支持 CREATE 状态的订单（未支付）
        return tradeOrder.getStatus() == TradeStatus.CREATE;
    }
}
