package org.example.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.example.common.cache.RedisKeyManager;
import org.example.common.exception.BizException;
import org.example.domain.model.trade.TradeOrder;
import org.example.domain.model.trade.message.RefundMessage;
import org.example.domain.model.trade.repository.TradeOrderRepository;
import org.example.domain.model.trade.valueobject.TradeStatus;
import org.example.domain.service.lock.IDistributedLockService;
import org.example.domain.service.refund.IRefundFallbackService;
import org.example.domain.service.refund.RefundStrategy;
import org.example.domain.service.refund.RefundStrategyFactory;
import org.example.domain.service.refund.TeamRefundStrategy;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 退单领域服务
 *
 * <p>
 * 职责：
 * <ul>
 * <li>协调Order聚合和TradeOrder聚合的退单操作</li>
 * <li>处理订单失败/超时后的退款逻辑</li>
 * <li>确保锁单量正确释放</li>
 * </ul>
 *
 * <p>
 * 业务场景：
 * <ul>
 * <li>拼团失败（未达到目标人数且超时）</li>
 * <li>用户主动取消（支付前）</li>
 * <li>系统异常取消</li>
 * </ul>
 *
 * <p>
 * 设计模式：策略模式
 * <ul>
 * <li>UnpaidRefundStrategy - 未支付退单（释放 Redis 名额）</li>
 * <li>PaidRefundStrategy - 已支付退单（调用支付网关退款）</li>
 * <li>TeamRefundStrategy - 拼团退单（批量退单）</li>
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
    private final IDistributedLockService lockService;
    // 降级策略：注入IRefundFallbackService接口（可选）
    private final IRefundFallbackService fallbackService;

    public RefundService(TradeOrderRepository tradeOrderRepository,
            RefundStrategyFactory refundStrategyFactory,
            TeamRefundStrategy teamRefundStrategy,
            IDistributedLockService lockService,
            IRefundFallbackService fallbackService) {
        this.tradeOrderRepository = tradeOrderRepository;
        this.refundStrategyFactory = refundStrategyFactory;
        this.teamRefundStrategy = teamRefundStrategy;
        this.lockService = lockService;
        this.fallbackService = fallbackService;
    }

    /**
     * 退单（单个交易订单）
     *
     * <p>
     * 使用策略模式，根据订单状态自动选择合适的退单策略
     *
     * <p>
     * 安全措施：
     * <ul>
     * <li>分布式锁：防止并发退款（基于tradeOrderId）</li>
     * <li>幂等性：通过订单状态判断，已退款订单直接返回</li>
     * </ul>
     *
     * @param tradeOrderId 交易订单ID
     * @param reason       退款原因
     */
    // TODO: 添加审计日志
    // 需要记录的信息：
    // - 操作时间、操作人、交易订单ID、退款原因、退款金额
    // - 操作结果（成功/失败）、失败原因
    // - 客户端IP、User-Agent
    // 实现方式：
    // - 方案A：数据库表（refund_audit_log）
    // - 方案B：日志文件 + 定期归档到数据库（推荐）
    // - 方案C：发送到ELK进行集中管理
    public void refundTradeOrder(String tradeOrderId, String reason) {
        // 1. 分布式锁：防止并发退款
        String lockKey = RedisKeyManager.lockKey("refund", tradeOrderId);

        try {
            // 尝试获取锁（30秒超时）
            boolean lockAcquired = lockService.tryLock(lockKey, 0, 30, TimeUnit.SECONDS);
            if (!lockAcquired) {
                log.warn("【退单服务】退款操作进行中，请稍后重试, tradeOrderId={}", tradeOrderId);
                throw new BizException("退款操作进行中，请稍后重试");
            }

            try {
                // 2. 加载 TradeOrder
                Optional<TradeOrder> tradeOrderOpt = tradeOrderRepository.findByTradeOrderId(tradeOrderId);
                if (tradeOrderOpt.isEmpty()) {
                    throw new BizException("交易订单不存在");
                }
                TradeOrder tradeOrder = tradeOrderOpt.get();

                // 3. 幂等性检查
                if (tradeOrder.getStatus() == TradeStatus.REFUND) {
                    log.info("【退单服务】订单已退款，幂等返回, tradeOrderId={}", tradeOrderId);
                    return;
                }

                // 4. 根据订单状态选择退单策略
                RefundStrategy strategy = refundStrategyFactory.getStrategy(tradeOrder);

                // 5. 执行退单策略
                strategy.execute(tradeOrder);

                // 6. 更新到数据库
                tradeOrderRepository.update(tradeOrder);

                log.info("【退单服务】退单成功, tradeOrderId={}, status={}, reason={}",
                        tradeOrderId, tradeOrder.getStatus(), reason);

            } finally {
                // 释放锁
                lockService.unlock(lockKey);
            }

        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("【退单服务】退单失败, tradeOrderId={}, reason={}", tradeOrderId, reason, e);

            // 降级策略：发送到MQ进行异步重试
            if (fallbackService != null) {
                try {
                    RefundMessage refundMessage = new RefundMessage(tradeOrderId, reason, "system");
                    fallbackService.sendToFallbackQueue(refundMessage);

                    log.warn("【退单服务】同步退款失败，已发送到MQ异步重试, tradeOrderId={}", tradeOrderId);

                    // 降级成功，不抛异常（异步处理）
                    return;
                } catch (Exception mqEx) {
                    log.error("【退单服务】发送到MQ失败, tradeOrderId={}", tradeOrderId, mqEx);
                }
            }

            throw new BizException("退单失败: " + e.getMessage());
        }
    }

    /**
     * 批量退单（拼团失败场景）
     *
     * <p>
     * 用途：当拼团失败时，退还该订单下所有已支付但未结算的交易订单
     *
     * <p>
     * 使用 TeamRefundStrategy 策略处理批量退单逻辑
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
            teamRefundStrategy.execute(tradeOrders.getFirst());

            log.info("【退单服务】拼团订单退款完成, orderId={}", orderId);

        } catch (Exception e) {
            log.error("【退单服务】拼团订单退款失败, orderId={}", orderId, e);
            throw new BizException("拼团订单退款失败: " + e.getMessage());
        }
    }

    /**
     * 批量处理超时订单（供定时任务调用）
     *
     * <p>
     * 用途：定时扫描超时的Order，批量退款
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
