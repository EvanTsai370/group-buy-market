package org.example.domain.service.refund;

import lombok.extern.slf4j.Slf4j;
import org.example.domain.model.order.Order;
import org.example.domain.model.order.repository.OrderRepository;
import org.example.domain.model.trade.TradeOrder;
import org.example.domain.model.trade.repository.TradeOrderRepository;
import org.example.domain.model.trade.valueobject.TradeStatus;

import java.util.List;
import java.util.Optional;

/**
 * 拼团退单策略
 *
 * <p>适用场景：拼团失败时，批量退还该订单下所有已支付的交易订单
 *
 * <p>处理逻辑：
 * <ol>
 *   <li>查询 Order 下所有 TradeOrder</li>
 *   <li>遍历每个 TradeOrder，根据状态选择合适的退单策略</li>
 *   <li>更新 Order 状态为 FAILED</li>
 *   <li>记录退款统计信息（成功数、失败数）</li>
 * </ol>
 *
 * <p>与其他策略的协作：
 * <ul>
 *   <li>对于 CREATE 状态的 TradeOrder，委托给 UnpaidRefundStrategy</li>
 *   <li>对于 PAID 状态的 TradeOrder，委托给 PaidRefundStrategy</li>
 *   <li>对于 SETTLED/TIMEOUT/REFUND 状态的 TradeOrder，跳过处理</li>
 * </ul>
 *
 * <p>注意事项：
 * <ul>
 *   <li>批量退单需保证事务一致性，单个失败不影响其他订单</li>
 *   <li>退单操作应记录审计日志，便于对账和排查问题</li>
 *   <li>拼团失败后，Order 的 lockCount 应归零（所有名额已释放）</li>
 * </ul>
 *
 */
@Slf4j
public class TeamRefundStrategy implements RefundStrategy {

    private final OrderRepository orderRepository;
    private final TradeOrderRepository tradeOrderRepository;
    private final UnpaidRefundStrategy unpaidRefundStrategy;
    private final PaidRefundStrategy paidRefundStrategy;

    public TeamRefundStrategy(
            OrderRepository orderRepository,
            TradeOrderRepository tradeOrderRepository,
            UnpaidRefundStrategy unpaidRefundStrategy,
            PaidRefundStrategy paidRefundStrategy) {
        this.orderRepository = orderRepository;
        this.tradeOrderRepository = tradeOrderRepository;
        this.unpaidRefundStrategy = unpaidRefundStrategy;
        this.paidRefundStrategy = paidRefundStrategy;
    }

    @Override
    public void execute(TradeOrder tradeOrder) throws Exception {
        String orderId = tradeOrder.getOrderId();

        log.info("【拼团退单策略】开始执行, orderId={}", orderId);

        // 1. 查询 Order 下所有 TradeOrder
        List<TradeOrder> tradeOrders = tradeOrderRepository.findByOrderId(orderId);

        int refundCount = 0;
        int skipCount = 0;
        int errorCount = 0;

        // 2. 批量退单
        for (TradeOrder order : tradeOrders) {
            try {
                // 只处理可以退单的订单（CREATE、PAID）
                if (!order.getStatus().canRefund()) {
                    log.info("【拼团退单策略】交易订单状态不支持退单，跳过, tradeOrderId={}, status={}",
                            order.getTradeOrderId(), order.getStatus());
                    skipCount++;
                    continue;
                }

                // 根据状态委托给不同的策略
                if (order.getStatus() == TradeStatus.CREATE) {
                    // 未支付 → 使用 UnpaidRefundStrategy
                    unpaidRefundStrategy.execute(order);
                } else if (order.getStatus() == TradeStatus.PAID) {
                    // 已支付 → 使用 PaidRefundStrategy
                    paidRefundStrategy.execute(order);
                }

                // 更新到数据库
                tradeOrderRepository.update(order);
                refundCount++;

                log.info("【拼团退单策略】交易订单退款成功, tradeOrderId={}, status={}",
                        order.getTradeOrderId(), order.getStatus());

            } catch (Exception e) {
                log.error("【拼团退单策略】交易订单退款失败, tradeOrderId={}, status={}",
                        order.getTradeOrderId(), order.getStatus(), e);
                errorCount++;
            }
        }

        // 3. 更新 Order 状态为 FAILED（可选，根据业务需求决定）
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            order.markAsFailed("拼团失败，批量退单");
            orderRepository.updateStatus(orderId, order.getStatus());
            log.info("【拼团退单策略】拼团订单已标记为失败, orderId={}", orderId);
        }

        log.info("【拼团退单策略】执行完成, orderId={}, refundCount={}, skipCount={}, errorCount={}",
                orderId, refundCount, skipCount, errorCount);

        // 如果所有退单都失败，抛出异常
        if (errorCount > 0 && refundCount == 0) {
            throw new Exception("拼团退单全部失败, orderId=" + orderId + ", errorCount=" + errorCount);
        }
    }

    @Override
    public boolean supports(TradeOrder tradeOrder) {
        // 拼团退单策略不基于单个 TradeOrder 判断，而是由外部主动调用
        // 因此这里始终返回 false，避免被 Factory 自动选择
        return false;
    }
}
