package org.example.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.example.common.cache.RedisKeyManager;
import org.example.domain.model.activity.Activity;
import org.example.domain.model.activity.repository.ActivityRepository;
import org.example.domain.model.account.Account;
import org.example.domain.model.account.repository.AccountRepository;
import org.example.domain.model.goods.repository.SkuRepository;
import org.example.domain.model.order.Order;
import org.example.domain.model.order.repository.OrderRepository;
import org.example.domain.model.trade.repository.TradeOrderRepository;
import org.example.domain.service.lock.IDistributedLockService;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 资源释放领域服务
 *
 * <p>
 * 职责：
 * <ul>
 * <li>统一管理预占资源的释放</li>
 * <li>确保释放顺序一致，避免遗漏</li>
 * <li>提供分布式锁保护，防止重复释放</li>
 * </ul>
 *
 * <p>
 * 释放场景：
 * <ol>
 * <li>锁单失败回滚（TradeOrderService）</li>
 * <li>交易关闭回调（SettlementService）</li>
 * <li>超时未支付退单（UnpaidRefundStrategy）</li>
 * <li>已支付退单（PaidRefundStrategy）</li>
 * </ol>
 *
 * @author 开发团队
 * @since 2026-01-12
 */
@Slf4j
public class ResourceReleaseService {

    /** 每次释放的库存数量（与锁单预占保持对称） */
    private static final int UNFREEZE_QUANTITY = 1;
    /** 默认有效期（20分钟） */
    private static final int DEFAULT_VALID_TIME = 1200;

    private final OrderRepository orderRepository;
    private final TradeOrderRepository tradeOrderRepository;
    private final SkuRepository skuRepository;
    private final IDistributedLockService lockService;
    private final ActivityRepository activityRepository;
    private final AccountRepository accountRepository;

    public ResourceReleaseService(
            OrderRepository orderRepository,
            TradeOrderRepository tradeOrderRepository,
            SkuRepository skuRepository,
            IDistributedLockService lockService,
            ActivityRepository activityRepository,
            AccountRepository accountRepository) {
        this.orderRepository = orderRepository;
        this.tradeOrderRepository = tradeOrderRepository;
        this.skuRepository = skuRepository;
        this.lockService = lockService;
        this.activityRepository = activityRepository;
        this.accountRepository = accountRepository;
    }

    /**
     * 释放全部预占资源
     *
     * <p>
     * 包括：Order.lockCount、名额槽位、冻结库存、参团次数
     *
     * @param orderId      订单ID
     * @param activityId   活动ID
     * @param skuId        商品ID
     * @param userId       用户ID
     * @param tradeOrderId 交易订单ID（用于分布式锁）
     * @param scene        场景标识（用于日志区分）
     */
    public void releaseAllResources(String orderId, String activityId,
            String skuId, String userId, String tradeOrderId, String scene) {
        log.info("【{}】开始释放全部预占资源, orderId={}, tradeOrderId={}", scene, orderId, tradeOrderId);

        // 1. 释放 Order.lockCount
        releaseLockCount(orderId, scene);

        // 2. 释放名额槽位（需要从orderId构造teamSlotKey）
        String teamSlotKey = orderId != null ? RedisKeyManager.teamSlotKey(orderId) : null;
        releaseSlot(teamSlotKey, activityId, tradeOrderId, scene);

        // 3. 释放冻结库存
        releaseInventory(skuId, tradeOrderId, scene);

        // 4. 释放参团次数
        releaseParticipationCount(userId, activityId, scene);

        log.info("【{}】全部预占资源释放完成, orderId={}, tradeOrderId={}", scene, orderId, tradeOrderId);
    }

    /**
     * 释放部分资源（锁单失败回滚专用）
     *
     * <p>
     * 锁单失败时 Order.lockCount 还未增加，只需释放名额槽位和库存
     *
     * <p>
     * 智能回滚机制：
     * <ul>
     * <li>如果teamSlotKey为null，自动跳过槽位释放（说明TeamSlotOccupyHandler未执行或失败）</li>
     * <li>如果skuId为null，自动跳过库存释放（说明InventoryOccupyHandler未执行或失败）</li>
     * </ul>
     *
     * @param teamSlotKey  Redis槽位key（从context.recoveryTeamSlotKey获取，可为null）
     * @param activityId   活动ID
     * @param skuId        商品ID（从context.recoverySkuId获取，可为null）
     * @param tradeOrderId 交易订单ID（用于分布式锁，可为null）
     * @param scene        场景标识
     */
    public void releaseSlotAndInventory(String teamSlotKey, String activityId,
            String skuId, String tradeOrderId, String scene) {
        log.info("【{}】开始释放槽位和库存, teamSlotKey={}, skuId={}", scene, teamSlotKey, skuId);

        // 1. 释放名额槽位（如果teamSlotKey为null会自动跳过）
        releaseSlot(teamSlotKey, activityId, tradeOrderId, scene);

        // 2. 释放冻结库存（如果skuId为null会自动跳过）
        releaseInventory(skuId, tradeOrderId, scene);

        log.info("【{}】槽位和库存释放完成", scene);
    }

    /**
     * 释放 Order.lockCount
     *
     * <p>
     * 执行流程：
     * <ol>
     * <li>加载 Order 聚合</li>
     * <li>调用 Order.validateReleaseLock() 进行业务校验</li>
     * <li>调用 Repository 原子递减 lockCount</li>
     * </ol>
     *
     * @param orderId 订单ID
     * @param scene   场景标识
     * @return true=成功, false=失败（可能已释放或lockCount为0）
     */
    public boolean releaseLockCount(String orderId, String scene) {
        if (orderId == null || orderId.isEmpty()) {
            log.warn("【{}】订单ID为空，跳过lockCount释放", scene);
            return false;
        }

        try {
            // 1. 加载 Order 聚合
            Order order = orderRepository.findById(orderId).orElse(null);
            if (order == null) {
                log.warn("【{}】Order不存在，跳过lockCount释放, orderId={}", scene, orderId);
                return false;
            }

            // 2. 业务校验
            order.validateReleaseLock();

            // 3. 原子递减 lockCount
            boolean success = orderRepository.decrementLockCount(orderId);
            if (success) {
                log.info("【{}】Order.lockCount释放成功, orderId={}", scene, orderId);
            } else {
                log.warn("【{}】Order.lockCount释放失败（可能已为0）, orderId={}", scene, orderId);
            }
            return success;
        } catch (Exception e) {
            log.error("【{}】Order.lockCount释放异常, orderId={}", scene, orderId, e);
            return false;
        }
    }

    /**
     * 释放名额槽位
     *
     * <p>
     * 与 TeamSlotOccupyHandler.DECR 操作对称，使用 INCR 恢复
     *
     * <p>
     * 设计改进：直接接受teamSlotKey，避免orderId ↔ teamSlotKey的来回转换
     *
     * @param teamSlotKey  Redis槽位key（格式：team_slot:{orderId}，可为null）
     * @param activityId   活动ID
     * @param tradeOrderId 交易订单ID（用于分布式锁）
     * @param scene        场景标识
     */
    public void releaseSlot(String teamSlotKey, String activityId,
            String tradeOrderId, String scene) {
        if (teamSlotKey == null || teamSlotKey.isEmpty()) {
            log.warn("【{}】teamSlotKey为空，跳过槽位释放", scene);
            return;
        }

        // ✅ 只在需要时才提取orderId（用于生成lockKey）
        String orderId = RedisKeyManager.extractOrderIdFromTeamSlotKey(teamSlotKey);
        Integer validTime = getValidTime(activityId);
        String lockKey = RedisKeyManager.lockKey("resource-release", tradeOrderId != null ? tradeOrderId : orderId);

        try {
            // 尝试获取锁（30天过期，防止锁永久占用）
            Boolean lockAcquired = lockService.setNx(lockKey, 30 * 24 * 60, TimeUnit.MINUTES);

            if (Boolean.FALSE.equals(lockAcquired)) {
                log.warn("【{}】槽位恢复操作已在进行中，跳过重复操作, teamSlotKey={}", scene, teamSlotKey);
                return;
            }

            // ✅ 直接使用传入的teamSlotKey，不再重新组装
            tradeOrderRepository.recoveryTeamSlot(teamSlotKey, validTime);
            log.info("【{}】槽位释放成功, teamSlotKey={}", scene, teamSlotKey);

        } catch (Exception ex) {
            // 恢复失败：释放锁，允许重试
            lockService.delete(lockKey);
            log.error("【{}】槽位释放失败, teamSlotKey={}", scene, teamSlotKey, ex);
        }
    }

    /**
     * 释放冻结库存
     *
     * <p>
     * 与 InventoryOccupyHandler.freezeStock() 保持对称
     *
     * @param skuId        商品ID
     * @param tradeOrderId 交易订单ID（用于日志追踪）
     * @param scene        场景标识
     */
    public void releaseInventory(String skuId, String tradeOrderId, String scene) {
        if (skuId == null || skuId.isEmpty()) {
            log.warn("【{}】商品ID为空，跳过库存释放, tradeOrderId={}", scene, tradeOrderId);
            return;
        }

        try {
            int result = skuRepository.unfreezeStock(skuId, UNFREEZE_QUANTITY);
            if (result > 0) {
                log.info("【{}】库存释放成功, skuId={}, tradeOrderId={}", scene, skuId, tradeOrderId);
            } else {
                log.warn("【{}】库存释放失败（可能已释放）, skuId={}, tradeOrderId={}", scene, skuId, tradeOrderId);
            }
        } catch (Exception e) {
            // 释放失败只记录日志，不影响主流程
            log.error("【{}】库存释放异常, skuId={}, tradeOrderId={}", scene, skuId, tradeOrderId, e);
        }
    }

    /**
     * 获取活动有效期
     *
     * @param activityId 活动ID
     * @return 有效期（秒）
     */
    private Integer getValidTime(String activityId) {
        if (activityId == null || activityId.isEmpty()) {
            return DEFAULT_VALID_TIME;
        }

        try {
            Optional<Activity> activityOpt = activityRepository.findById(activityId);
            if (activityOpt.isPresent() && activityOpt.get().getValidTime() != null) {
                return activityOpt.get().getValidTime();
            }
        } catch (Exception e) {
            log.warn("【ResourceReleaseService】获取活动有效期失败，使用默认值, activityId={}", activityId);
        }
        return DEFAULT_VALID_TIME;
    }

    /**
     * 释放参团次数
     *
     * <p>
     * 与 Account.deductCount() 保持对称，调用 compensateCount() 恢复
     *
     * @param userId     用户ID
     * @param activityId 活动ID
     * @param scene      场景标识
     */
    public void releaseParticipationCount(String userId, String activityId, String scene) {
        if (userId == null || userId.isEmpty() || activityId == null || activityId.isEmpty()) {
            log.warn("【{}】用户ID或活动ID为空，跳过参团次数释放", scene);
            return;
        }

        try {
            // 1. 加载 Account 聚合
            Account account = accountRepository.findByUserAndActivity(userId, activityId).orElse(null);
            if (account == null) {
                log.warn("【{}】Account不存在，跳过参团次数释放, userId={}, activityId={}", scene, userId, activityId);
                return;
            }

            // 2. 调用聚合根方法补偿
            account.compensateCount();

            // 3. 持久化
            accountRepository.save(account);

            log.info("【{}】参团次数释放成功, userId={}, activityId={}", scene, userId, activityId);
        } catch (Exception e) {
            log.error("【{}】参团次数释放异常, userId={}, activityId={}", scene, userId, activityId, e);
        }
    }
}
