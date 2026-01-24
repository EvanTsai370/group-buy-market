package org.example.domain.service.refund;

import lombok.extern.slf4j.Slf4j;
import org.example.domain.model.trade.TradeOrder;

import java.util.List;

/**
 * 退单策略工厂
 *
 * <p>职责：根据交易订单状态选择合适的退单策略
 *
 * <p>策略选择逻辑：
 * <ul>
 *   <li>CREATE 状态 → UnpaidRefundStrategy（未支付退单）</li>
 *   <li>PAID 状态 → PaidRefundStrategy（已支付退单）</li>
 *   <li>其他状态 → 不支持退单，抛出异常</li>
 * </ul>
 *
 * <p>设计模式：
 * <ul>
 *   <li>工厂模式：封装策略实例化逻辑</li>
 *   <li>策略模式：根据条件选择不同的退单策略</li>
 * </ul>
 *
 * <p>注意事项：
 * <ul>
 *   <li>TeamRefundStrategy 不通过工厂选择，而是由外部主动调用</li>
 *   <li>工厂类保持无状态，所有策略实例通过构造函数注入</li>
 * </ul>
 *
 */
@Slf4j
public class RefundStrategyFactory {

    private final List<RefundStrategy> strategies;

    /**
     * 构造函数注入所有退单策略
     *
     * @param strategies 退单策略列表
     */
    public RefundStrategyFactory(List<RefundStrategy> strategies) {
        this.strategies = strategies;
    }

    /**
     * 根据交易订单选择合适的退单策略
     *
     * @param tradeOrder 交易订单
     * @return 退单策略
     * @throws IllegalArgumentException 如果没有找到合适的策略
     */
    public RefundStrategy getStrategy(TradeOrder tradeOrder) {
        for (RefundStrategy strategy : strategies) {
            if (strategy.supports(tradeOrder)) {
                log.debug("【退单策略工厂】选择策略: {}, tradeOrderId={}, status={}",
                        strategy.getClass().getSimpleName(),
                        tradeOrder.getTradeOrderId(),
                        tradeOrder.getStatus());
                return strategy;
            }
        }

        throw new IllegalArgumentException(
                String.format("未找到合适的退单策略, tradeOrderId=%s, status=%s",
                        tradeOrder.getTradeOrderId(),
                        tradeOrder.getStatus())
        );
    }
}
