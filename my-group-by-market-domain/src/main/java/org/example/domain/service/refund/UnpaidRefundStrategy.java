package org.example.domain.service.refund;

import lombok.extern.slf4j.Slf4j;
import org.example.domain.model.order.Order;
import org.example.domain.model.order.repository.OrderRepository;
import org.example.domain.model.trade.TradeOrder;
import org.example.domain.model.trade.valueobject.TradeStatus;

import java.util.Optional;

/**
 * 未支付退单策略
 *
 * <p>适用场景：用户下单后超时未支付，需要释放锁定的拼团名额
 *
 * <p>处理逻辑：
 * <ol>
 *   <li>标记 TradeOrder 为 TIMEOUT 状态</li>
 *   <li>原子递减 Order 的 lockCount（释放锁定名额）</li>
 *   <li>无需调用支付网关退款（用户未支付）</li>
 * </ol>
 *
 * <p>Redis 库存释放：
 * <ul>
 *   <li>lockCount 保存在 MySQL 中，通过 ORDER BY ... FOR UPDATE 原子递减</li>
 *   <li>Redis 中的库存仅用于快速拦截（允许少量超卖，由 MySQL 兜底）</li>
 *   <li>未支付退单时无需同步更新 Redis，避免额外开销</li>
 * </ul>
 *
 * @author 开发团队
 * @since 2026-01-06
 */
@Slf4j
public class UnpaidRefundStrategy implements RefundStrategy {

    private final OrderRepository orderRepository;

    public UnpaidRefundStrategy(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public void execute(TradeOrder tradeOrder) throws Exception {
        log.info("【未支付退单策略】开始执行, tradeOrderId={}, status={}",
                tradeOrder.getTradeOrderId(), tradeOrder.getStatus());

        // 1. 标记为超时
        tradeOrder.markAsTimeout();

        // 2. 释放 Order 的锁定名额（原子递减 lockCount）
        String orderId = tradeOrder.getOrderId();
        boolean success = orderRepository.decrementLockCount(orderId);

        if (!success) {
            log.warn("【未支付退单策略】释放锁定名额失败，可能 lockCount 已为 0, orderId={}", orderId);
        }

        // 3. 更新 Order 聚合的内存状态（可选，确保数据一致性）
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            order.onReleaseLockSuccess(Math.max(0, order.getLockCount() - 1));
        }

        log.info("【未支付退单策略】执行成功, tradeOrderId={}, orderId={}",
                tradeOrder.getTradeOrderId(), orderId);
    }

    @Override
    public boolean supports(TradeOrder tradeOrder) {
        // 仅支持 CREATE 状态的订单（未支付）
        return tradeOrder.getStatus() == TradeStatus.CREATE;
    }
}
