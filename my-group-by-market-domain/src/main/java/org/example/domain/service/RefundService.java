package org.example.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.example.common.exception.BizException;
import org.example.domain.model.trade.TradeOrder;
import org.example.domain.model.trade.repository.TradeOrderRepository;
import org.example.domain.service.refund.RefundStrategy;
import org.example.domain.service.refund.RefundStrategyFactory;
import org.example.domain.service.refund.TeamRefundStrategy;

import java.util.List;
import java.util.Optional;

/**
 * 退单领域服务
 *
 * <p>职责：
 * <ul>
 *   <li>协调Order聚合和TradeOrder聚合的退单操作</li>
 *   <li>处理订单失败/超时后的退款逻辑</li>
 *   <li>确保锁单量正确释放</li>
 * </ul>
 *
 * <p>业务场景：
 * <ul>
 *   <li>拼团失败（未达到目标人数且超时）</li>
 *   <li>用户主动取消（支付前）</li>
 *   <li>系统异常取消</li>
 * </ul>
 *
 * <p>设计模式：策略模式
 * <ul>
 *   <li>UnpaidRefundStrategy - 未支付退单（释放 Redis 库存）</li>
 *   <li>PaidRefundStrategy - 已支付退单（调用支付网关退款）</li>
 *   <li>TeamRefundStrategy - 拼团退单（批量退单）</li>
 * </ul>
 *
 * @author 开发团队
 * @since 2026-01-04
 */
@Slf4j
public class RefundService {

    private final TradeOrderRepository tradeOrderRepository;
    private final RefundStrategyFactory refundStrategyFactory;
    private final TeamRefundStrategy teamRefundStrategy;

    public RefundService(TradeOrderRepository tradeOrderRepository,
                         RefundStrategyFactory refundStrategyFactory,
                         TeamRefundStrategy teamRefundStrategy) {
        this.tradeOrderRepository = tradeOrderRepository;
        this.refundStrategyFactory = refundStrategyFactory;
        this.teamRefundStrategy = teamRefundStrategy;
    }

    /**
     * 退单（单个交易订单）
     *
     * <p>使用策略模式，根据订单状态自动选择合适的退单策略
     *
     * @param tradeOrderId 交易订单ID
     */
    public void refundTradeOrder(String tradeOrderId) {
        // 1. 加载 TradeOrder
        Optional<TradeOrder> tradeOrderOpt = tradeOrderRepository.findByTradeOrderId(tradeOrderId);
        if (tradeOrderOpt.isEmpty()) {
            throw new BizException("交易订单不存在");
        }
        TradeOrder tradeOrder = tradeOrderOpt.get();

        try {
            // 2. 根据订单状态选择退单策略
            RefundStrategy strategy = refundStrategyFactory.getStrategy(tradeOrder);

            // 3. 执行退单策略
            strategy.execute(tradeOrder);

            // 4. 更新到数据库
            tradeOrderRepository.update(tradeOrder);

            log.info("【退单服务】退单成功, tradeOrderId={}, status={}",
                    tradeOrderId, tradeOrder.getStatus());

        } catch (Exception e) {
            log.error("【退单服务】退单失败, tradeOrderId={}", tradeOrderId, e);
            throw new BizException("退单失败: " + e.getMessage());
        }
    }

    /**
     * 批量退单（拼团失败场景）
     *
     * <p>用途：当拼团失败时，退还该订单下所有已支付但未结算的交易订单
     *
     * <p>使用 TeamRefundStrategy 策略处理批量退单逻辑
     *
     * @param orderId 拼团订单ID
     */
    public void refundFailedOrder(String orderId) {
        try {
            // 1. 查询 Order 下任意一个 TradeOrder（用于传递 orderId）
            List<TradeOrder> tradeOrders = tradeOrderRepository.findByOrderId(orderId);

            if (tradeOrders.isEmpty()) {
                log.warn("【退单服务】拼团订单下无交易订单, orderId={}", orderId);
                return;
            }

            // 2. 使用 TeamRefundStrategy 批量退单
            teamRefundStrategy.execute(tradeOrders.get(0));

            log.info("【退单服务】拼团订单退款完成, orderId={}", orderId);

        } catch (Exception e) {
            log.error("【退单服务】拼团订单退款失败, orderId={}", orderId, e);
            throw new BizException("拼团订单退款失败: " + e.getMessage());
        }
    }

    /**
     * 批量处理超时订单（供定时任务调用）
     *
     * <p>用途：定时扫描超时的Order，批量退款
     *
     * @param orderIds 订单ID列表
     */
    public void batchRefundTimeoutOrders(List<String> orderIds) {
        for (String orderId : orderIds) {
            try {
                refundFailedOrder(orderId);
            } catch (Exception e) {
                log.error("【退单服务】处理超时订单失败, orderId: {}", orderId, e);
            }
        }
    }
}
