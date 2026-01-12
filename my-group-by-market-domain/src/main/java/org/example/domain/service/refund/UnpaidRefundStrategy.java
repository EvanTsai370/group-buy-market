package org.example.domain.service.refund;

import lombok.extern.slf4j.Slf4j;
import org.example.common.cache.RedisKeyManager;
import org.example.domain.model.activity.Activity;
import org.example.domain.model.activity.repository.ActivityRepository;
import org.example.domain.model.goods.repository.SkuRepository;
import org.example.domain.model.order.Order;
import org.example.domain.model.order.repository.OrderRepository;
import org.example.domain.model.trade.TradeOrder;
import org.example.domain.model.trade.repository.TradeOrderRepository;
import org.example.domain.model.trade.valueobject.TradeStatus;
import org.example.domain.service.lock.IDistributedLockService;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

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
 * <li>原子递减 Order 的 lockCount（释放锁定名额）</li>
 * <li>恢复 Redis 名额（与锁单失败回滚保持对称）</li>
 * <li>释放冻结库存（与锁单预占保持对称）</li>
 * <li>无需调用支付网关退款（用户未支付）</li>
 * </ol>
 *
 * <p>
 * Redis 名额恢复：
 * <ul>
 * <li>通过 INCR 操作恢复 Redis 的 available 名额</li>
 * <li>与 TradeOrderService.lockOrder() 的回滚逻辑保持对称</li>
 * <li>确保退款后名额立即释放，其他用户可以加入</li>
 * <li>恢复失败只记录日志，不影响主流程</li>
 * </ul>
 *
 * @author 开发团队
 * @since 2026-01-06
 */
@Slf4j
public class UnpaidRefundStrategy implements RefundStrategy {

    /** 每次释放的库存数量（与锁单预占保持对称） */
    private static final int UNFREEZE_QUANTITY = 1;

    private final OrderRepository orderRepository;
    private final TradeOrderRepository tradeOrderRepository;
    private final ActivityRepository activityRepository;
    private final IDistributedLockService lockService;
    private final SkuRepository skuRepository;

    public UnpaidRefundStrategy(
            OrderRepository orderRepository,
            TradeOrderRepository tradeOrderRepository,
            ActivityRepository activityRepository,
            IDistributedLockService lockService,
            SkuRepository skuRepository) {
        this.orderRepository = orderRepository;
        this.tradeOrderRepository = tradeOrderRepository;
        this.activityRepository = activityRepository;
        this.lockService = lockService;
        this.skuRepository = skuRepository;
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

        // 3. 恢复 Redis 名额
        String teamSlotKey = RedisKeyManager.teamSlotKey(orderId);
        Integer validTime = getValidTime(tradeOrder.getActivityId());
        recoveryRedisSlot(teamSlotKey, validTime, tradeOrder.getTradeOrderId());

        // 4. 释放冻结库存
        releaseInventory(tradeOrder.getGoodsId(), tradeOrder.getTradeOrderId());

        log.info("【未支付退单策略】执行成功, tradeOrderId={}, orderId={}",
                tradeOrder.getTradeOrderId(), orderId);
    }

    @Override
    public boolean supports(TradeOrder tradeOrder) {
        // 仅支持 CREATE 状态的订单（未支付）
        return tradeOrder.getStatus() == TradeStatus.CREATE;
    }

    /**
     * 释放冻结库存
     *
     * <p>
     * 业务场景：
     * <ul>
     * <li>用户锁单后超时未支付，需要释放冻结库存</li>
     * <li>与 InventoryOccupyHandler.freezeStock() 保持对称</li>
     * </ul>
     *
     * @param goodsId      商品ID
     * @param tradeOrderId 交易订单ID（用于日志追踪）
     */
    private void releaseInventory(String goodsId, String tradeOrderId) {
        if (goodsId == null || goodsId.isEmpty()) {
            log.warn("【未支付退单策略】商品ID为空，跳过库存释放, tradeOrderId={}", tradeOrderId);
            return;
        }

        try {
            int result = skuRepository.unfreezeStock(goodsId, UNFREEZE_QUANTITY);
            if (result > 0) {
                log.info("【未支付退单策略】库存释放成功, goodsId={}, tradeOrderId={}", goodsId, tradeOrderId);
            } else {
                log.warn("【未支付退单策略】库存释放失败（可能已释放）, goodsId={}, tradeOrderId={}", goodsId, tradeOrderId);
            }
        } catch (Exception e) {
            // 释放失败只记录日志，不影响主流程
            log.error("【未支付退单策略】库存释放异常, goodsId={}, tradeOrderId={}", goodsId, tradeOrderId, e);
        }
    }

    /**
     * 获取活动有效期
     *
     * <p>
     * 用于设置Redis Key的过期时间
     *
     * @param activityId 活动ID
     * @return 有效期（秒），默认1200秒（20分钟）
     */
    private Integer getValidTime(String activityId) {
        return activityRepository.findById(activityId)
                .map(Activity::getValidTime)
                .orElse(1200); // 默认20分钟
    }

    /**
     * 恢复Redis名额
     *
     * <p>
     * 业务场景：
     * <ul>
     * <li>用户锁单后超时未支付，需要释放Redis名额</li>
     * <li>与 TradeOrderService.rollbackTeamSlot() 保持对称</li>
     * </ul>
     *
     * <p>
     * 设计说明：
     * <ul>
     * <li>使用分布式锁防止重复恢复（基于tradeOrderId）</li>
     * <li>使用 INCR 原子操作恢复名额</li>
     * <li>恢复失败只记录日志，不影响主流程（MySQL已释放）</li>
     * </ul>
     *
     * @param teamSlotKey  Redis名额Key
     * @param validTime    有效期（秒）
     * @param tradeOrderId 交易订单ID（用于分布式锁）
     */
    private void recoveryRedisSlot(String teamSlotKey, Integer validTime, String tradeOrderId) {
        // 使用RedisKeyManager生成分布式锁key
        String lockKey = RedisKeyManager.lockKey("refund", tradeOrderId);

        try {
            // 尝试获取锁（30天过期，防止锁永久占用）
            Boolean lockAcquired = lockService.setNx(lockKey, 30 * 24 * 60, TimeUnit.MINUTES);

            if (Boolean.FALSE.equals(lockAcquired)) {
                log.warn("【未支付退单策略】名额恢复操作已在进行中，跳过重复操作, tradeOrderId: {}", tradeOrderId);
                return;
            }

            // 在锁保护下执行名额恢复
            tradeOrderRepository.recoveryTeamSlot(teamSlotKey, validTime);
            log.info("【未支付退单策略】恢复Redis名额成功, teamSlotKey: {}, tradeOrderId: {}",
                    teamSlotKey, tradeOrderId);

        } catch (Exception ex) {
            // 恢复失败：释放锁，允许重试
            lockService.delete(lockKey);
            log.error("【未支付退单策略】恢复Redis名额失败, teamSlotKey: {}, tradeOrderId: {}",
                    teamSlotKey, tradeOrderId, ex);
            // 不抛异常，避免影响主流程（MySQL已释放）
        }
    }
}
