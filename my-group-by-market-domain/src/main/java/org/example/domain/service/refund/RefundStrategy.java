package org.example.domain.service.refund;

import org.example.domain.model.trade.TradeOrder;

/**
 * 退单策略接口
 *
 * <p>使用策略模式处理不同场景的退单逻辑：
 * <ul>
 *   <li>UnpaidRefundStrategy - 未支付退单（释放 Redis 名额）</li>
 *   <li>PaidRefundStrategy - 已支付退单（调用支付网关退款）</li>
 *   <li>TeamRefundStrategy - 拼团退单（影响团队状态）</li>
 * </ul>
 *
 * <p>设计原则：
 * <ul>
 *   <li>策略接口定义在领域层，保持框架无关</li>
 *   <li>每个策略封装特定场景的退单逻辑</li>
 *   <li>通过 RefundStrategyFactory 根据订单状态选择合适的策略</li>
 * </ul>
 *
 * @author 开发团队
 * @since 2026-01-06
 */
public interface RefundStrategy {

    /**
     * 执行退单逻辑
     *
     * @param tradeOrder 交易订单
     * @throws Exception 退单失败时抛出异常
     */
    void execute(TradeOrder tradeOrder) throws Exception;

    /**
     * 判断是否支持此类型的退单
     *
     * @param tradeOrder 交易订单
     * @return true表示支持
     */
    boolean supports(TradeOrder tradeOrder);
}
