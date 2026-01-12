package org.example.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.example.common.exception.BizException;
import org.example.domain.model.order.Order;
import org.example.domain.model.order.repository.OrderRepository;
import org.example.domain.model.trade.TradeOrder;
import org.example.domain.model.trade.repository.TradeOrderRepository;
import org.example.domain.model.trade.valueobject.NotifyConfig;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * 锁单领域服务
 *
 * <p>
 * 职责：
 * <ul>
 * <li>协调Order聚合和TradeOrder聚合的锁单操作</li>
 * <li>确保锁单的原子性和一致性</li>
 * <li>实现补偿机制（如果TradeOrder创建失败，需要释放Order的锁定）</li>
 * </ul>
 *
 * <p>
 * 业务流程：
 * <ol>
 * <li>校验Order是否可以锁定（validateLock）</li>
 * <li>创建TradeOrder聚合</li>
 * <li>保存TradeOrder到数据库</li>
 * <li>原子更新Order的lockCount（Repository层保证原子性）</li>
 * <li>如果任何步骤失败，抛出异常（由调用方处理事务回滚）</li>
 * </ol>
 *
 * @author 开发团队
 * @since 2026-01-04
 */
@Slf4j
public class LockOrderService {

    private final OrderRepository orderRepository;
    private final TradeOrderRepository tradeOrderRepository;

    public LockOrderService(OrderRepository orderRepository,
            TradeOrderRepository tradeOrderRepository) {
        this.orderRepository = orderRepository;
        this.tradeOrderRepository = tradeOrderRepository;
    }

    /**
     * 锁单
     *
     * @param tradeOrderId   交易订单ID
     * @param orderId        拼团订单ID
     * @param activityId     活动ID
     * @param userId         用户ID
     * @param skuId          商品ID
     * @param goodsName      商品名称
     * @param originalPrice  原始价格
     * @param deductionPrice 减免金额
     * @param payPrice       实付金额
     * @param outTradeNo     外部交易单号（幂等性保证）
     * @param source         来源
     * @param channel        渠道
     * @param notifyConfig   通知配置
     * @return 创建的交易订单
     */
    // 考虑在 TradeOrderService 中缓存 Order 对象，传递过来
    public TradeOrder lockOrder(
            String tradeOrderId,
            String orderId,
            String activityId,
            String userId,
            String skuId,
            String spuId, // 新增：用于校验是否匹配拼团的商品大类
            String goodsName,
            BigDecimal originalPrice,
            BigDecimal deductionPrice,
            BigDecimal payPrice,
            String outTradeNo,
            String source,
            String channel,
            NotifyConfig notifyConfig) {

        // 1. 幂等性校验：检查外部交易单号是否已存在
        Optional<TradeOrder> existingTradeOrder = tradeOrderRepository.findByOutTradeNo(outTradeNo);
        if (existingTradeOrder.isPresent()) {
            log.warn("【锁单服务】交易单号已存在，返回已有订单, outTradeNo: {}", outTradeNo);
            return existingTradeOrder.get();
        }

        // 2. 加载Order聚合
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            throw new BizException("拼团订单不存在");
        }
        Order order = orderOpt.get();

        // 3.1 校验商品类型是否匹配（SPU 级别匹配）
        // 允许不同规格（SKU）的用户参与同一个拼团（SPU），只要它们属于同一个商品大类
        if (!order.getSpuId().equals(spuId)) {
            log.warn("【锁单服务】商品类型不匹配, orderId: {}, orderSpuId: {}, requestSpuId: {}",
                    orderId, order.getSpuId(), spuId);
            throw new BizException("商品类型不匹配，无法参与此拼团");
        }

        // 3.2 校验Order是否可以锁定
        order.validateLock();

        // 4. 创建TradeOrder聚合
        TradeOrder tradeOrder = TradeOrder.create(
                tradeOrderId,
                order.getTeamId(),
                orderId,
                activityId,
                userId,
                skuId,
                goodsName,
                originalPrice,
                deductionPrice,
                payPrice,
                outTradeNo,
                source,
                channel,
                notifyConfig);

        // 5. 保存TradeOrder
        tradeOrderRepository.save(tradeOrder);

        // 6. 原子更新Order的lockCount（Repository层会执行：UPDATE order SET lock_count =
        // lock_count + 1 WHERE order_id = ? AND lock_count < target_count）
        int newLockCount = orderRepository.incrementLockCount(orderId);
        if (newLockCount == -1) {
            throw new BizException("锁单失败，拼团名额已满或订单状态异常");
        }

        // 7. 更新Order聚合的内存状态（使用数据库返回的真实值）
        order.onLockSuccess(newLockCount);

        log.info("【锁单服务】锁单成功, tradeOrderId: {}, orderId: {}, userId: {}, lockCount: {}/{}",
                tradeOrderId, orderId, userId, newLockCount, order.getTargetCount());

        return tradeOrder;
    }
}
