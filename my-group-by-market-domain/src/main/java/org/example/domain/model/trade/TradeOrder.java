package org.example.domain.model.trade;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.example.common.exception.BizException;
import org.example.common.util.LogDesensitizer;
import org.example.domain.model.order.Order;
import org.example.domain.model.trade.valueobject.NotifyConfig;
import org.example.domain.model.trade.valueobject.TradeStatus;
import org.example.domain.shared.DomainEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * TradeOrder 聚合根（交易订单）
 *
 * <p>
 * 职责：
 * <ul>
 * <li>管理交易订单的生命周期（锁单 → 支付 → 结算 → 超时/退单）</li>
 * <li>记录交易快照（金额、商品、用户信息）</li>
 * <li>管理回调通知配置和状态</li>
 * </ul>
 *
 * <p>
 * 与Order的关系：
 * <ul>
 * <li>Order：拼团聚合，管理"拼团成功与否"</li>
 * <li>TradeOrder：交易聚合，管理"支付过程"</li>
 * <li>一个Order可能有多个TradeOrder（多个用户参与同一拼团）</li>
 * </ul>
 *
 * @author 开发团队
 * @since 2026-01-04
 */
@Slf4j
@Data
public class TradeOrder {

    /** 交易订单ID */
    private String tradeOrderId;

    /** 拼团队伍ID（关联Order表） */
    private String teamId;

    /** 拼团订单ID（关联Order表） */
    private String orderId;

    /** 活动ID */
    private String activityId;

    /** 用户ID */
    private String userId;

    /** 商品ID */
    private String skuId;

    /** 商品名称（冗余，避免JOIN） */
    private String goodsName;

    /** 原始价格 */
    private BigDecimal originalPrice;

    /** 减免金额（优惠金额） */
    private BigDecimal deductionPrice;

    /** 实付金额（原价 - 减免） */
    private BigDecimal payPrice;

    /** 交易状态 */
    private TradeStatus status;

    /** 外部交易单号（幂等性保证） */
    private String outTradeNo;

    /** 支付时间 */
    private LocalDateTime payTime;

    /** 结算时间 */
    private LocalDateTime settlementTime;

    /** 来源 */
    private String source;

    /** 渠道 */
    private String channel;

    /** 通知配置 */
    private NotifyConfig notifyConfig;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 退款原因 */
    private String refundReason;

    /** 退款时间 */
    private LocalDateTime refundTime;

    /** 领域事件列表 */
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    /**
     * 创建交易订单（锁单）
     * 工厂方法
     *
     * @param tradeOrderId   交易订单ID
     * @param teamId         拼团队伍ID
     * @param orderId        拼团订单ID
     * @param activityId     活动ID
     * @param userId         用户ID
     * @param skuId        商品ID
     * @param goodsName      商品名称
     * @param originalPrice  原始价格
     * @param deductionPrice 减免金额
     * @param payPrice       实付金额
     * @param outTradeNo     外部交易单号
     * @param source         来源
     * @param channel        渠道
     * @param notifyConfig   通知配置
     * @return 交易订单
     */
    public static TradeOrder create(
            String tradeOrderId,
            String teamId,
            String orderId,
            String activityId,
            String userId,
            String skuId,
            String goodsName,
            BigDecimal originalPrice,
            BigDecimal deductionPrice,
            BigDecimal payPrice,
            String outTradeNo,
            String source,
            String channel,
            NotifyConfig notifyConfig) {

        // 参数校验
        if (payPrice == null || payPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException("实付金额不能为负数");
        }

        // 校验通知配置
        if (notifyConfig != null && !notifyConfig.isValid()) {
            throw new BizException("通知配置无效");
        }

        TradeOrder tradeOrder = new TradeOrder();
        tradeOrder.tradeOrderId = tradeOrderId;
        tradeOrder.teamId = teamId;
        tradeOrder.orderId = orderId;
        tradeOrder.activityId = activityId;
        tradeOrder.userId = userId;
        tradeOrder.skuId = skuId;
        tradeOrder.goodsName = goodsName;
        tradeOrder.originalPrice = originalPrice;
        tradeOrder.deductionPrice = deductionPrice;
        tradeOrder.payPrice = payPrice;
        tradeOrder.status = TradeStatus.CREATE;
        tradeOrder.outTradeNo = outTradeNo;
        tradeOrder.payTime = null;
        tradeOrder.settlementTime = null;
        tradeOrder.source = source;
        tradeOrder.channel = channel;
        tradeOrder.notifyConfig = notifyConfig;
        tradeOrder.createTime = LocalDateTime.now();
        tradeOrder.updateTime = LocalDateTime.now();

        log.info("【TradeOrder聚合】交易订单创建成功, tradeOrderId: {}, teamId: {}, userId: {}, payPrice: {}",
                tradeOrderId, teamId, userId, LogDesensitizer.maskPrice(payPrice, log));

        return tradeOrder;
    }

    // ==================== 状态流转业务逻辑 ====================

    /**
     * 标记为已支付
     *
     * @param payTime 支付时间
     */
    public void markAsPaid(LocalDateTime payTime) {
        if (this.status != TradeStatus.CREATE) {
            throw new BizException("当前状态不支持支付操作，status: " + this.status);
        }

        this.status = TradeStatus.PAID;
        this.payTime = payTime;
        this.updateTime = LocalDateTime.now();

        log.info("【TradeOrder聚合】交易订单已支付, tradeOrderId: {}, payTime: {}",
                tradeOrderId, payTime);
    }

    /**
     * 标记为已结算（拼团成功）
     *
     * @param settlementTime 结算时间
     */
    public void markAsSettled(LocalDateTime settlementTime) {
        if (this.status != TradeStatus.PAID) {
            throw new BizException("当前状态不支持结算操作，status: " + this.status);
        }

        this.status = TradeStatus.SETTLED;
        this.settlementTime = settlementTime;
        this.updateTime = LocalDateTime.now();

        log.info("【TradeOrder聚合】交易订单已结算, tradeOrderId: {}, settlementTime: {}",
                tradeOrderId, settlementTime);

        // 标记通知成功（如果配置了通知）
        if (this.notifyConfig != null && this.notifyConfig.needNotify()) {
            this.notifyConfig.markSuccess();
        }
    }

    /**
     * 标记为超时
     */
    public void markAsTimeout() {
        if (this.status != TradeStatus.CREATE) {
            throw new BizException("当前状态不支持超时操作，status: " + this.status);
        }

        this.status = TradeStatus.TIMEOUT;
        this.updateTime = LocalDateTime.now();

        log.warn("【TradeOrder聚合】交易订单已超时, tradeOrderId: {}", tradeOrderId);
    }

    /**
     * 标记为退单
     *
     * @param reason 退款原因
     */
    public void markAsRefund(String reason) {
        if (!this.status.canRefund()) {
            throw new BizException("当前状态不支持退单操作，status: " + this.status);
        }

        this.status = TradeStatus.REFUND;
        this.refundReason = reason;
        this.refundTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();

        log.warn("【TradeOrder聚合】交易订单已退单, tradeOrderId: {}, reason: {}", tradeOrderId, reason);
    }

    /**
     * 判断是否可以结算（PAID → SETTLED）
     *
     * <p>
     * 业务场景：拼团成功后，批量结算所有已支付的订单
     * <p>
     * 状态要求：只有PAID状态的订单才能结算
     *
     * @return true=可以结算, false=不可以结算
     */
    public boolean canSettle() {
        return this.status == TradeStatus.PAID;
    }

    /**
     * 判断是否已支付
     * 
     * @return true=已支付或更高状态, false=未支付
     */
    public boolean isPaid() {
        return this.status == TradeStatus.PAID || this.status == TradeStatus.SETTLED;
    }

    /**
     * 判断是否已结算
     * 
     * @return true=已结算, false=未结算
     */
    public boolean isSettled() {
        return this.status == TradeStatus.SETTLED;
    }

    /**
     * 判断是否已超时
     * 
     * @return true=已超时, false=未超时
     */
    public boolean isTimeout() {
        return this.status == TradeStatus.TIMEOUT;
    }

    /**
     * 判断是否已退款
     * 
     * @return true=已退款, false=未退款
     */
    public boolean isRefunded() {
        return this.status == TradeStatus.REFUND;
    }

    /**
     * 判断是否是初始状态（可支付）
     * 
     * @return true=初始状态, false=非初始状态
     */
    public boolean isCreated() {
        return this.status == TradeStatus.CREATE;
    }

    /**
     * 校验是否可以支付（CREATE → PAID 的前置校验）
     *
     * <p>
     * 业务场景：用户支付成功后，校验订单是否可以标记为已支付
     * <p>
     * 业务规则：
     * <ul>
     * <li>只允许 CREATE 状态（PAID/SETTLED 已在幂等性检查中静默返回）</li>
     * <li>渠道不能在黑名单中（下架渠道）</li>
     * <li>支付时间必须在拼团有效期内</li>
     * </ul>
     *
     * @param order               拼团订单（用于时间校验）
     * @param blacklistedChannels 渠道黑名单（可选）
     * @throws BizException 如果不满足支付条件
     */
    public void validatePayment(Order order,
            Set<String> blacklistedChannels) {
        // 1. 状态校验 - 只允许 CREATE 状态
        // 注意：PAID/SETTLED 状态已在 SettlementService 的幂等性检查中静默返回
        if (this.status != TradeStatus.CREATE) {
            log.warn("【TradeOrder聚合】订单状态不允许支付, tradeOrderId: {}, status: {}",
                    tradeOrderId, status);
            throw new BizException("订单状态不允许支付，status: " + this.status);
        }

        // 2. 渠道黑名单校验（可选）
        if (blacklistedChannels != null && !blacklistedChannels.isEmpty()) {
            String channelKey = this.source + ":" + this.channel;
            if (blacklistedChannels.contains(channelKey)) {
                log.warn("【TradeOrder聚合】渠道已下架，不可支付, tradeOrderId: {}, channel: {}",
                        tradeOrderId, channelKey);
                throw new BizException("渠道已下架，不可支付");
            }
        }

        // 3. 拼团有效期校验
        if (order != null) {
            LocalDateTime deadlineTime = order.getDeadlineTime();
            if (deadlineTime != null && LocalDateTime.now().isAfter(deadlineTime)) {
                log.warn("【TradeOrder聚合】拼团已过期，不可支付, tradeOrderId: {}, deadlineTime: {}",
                        tradeOrderId, deadlineTime);
                throw new BizException("拼团已过期，不可支付");
            }
        }

        log.debug("【TradeOrder聚合】支付校验通过, tradeOrderId: {}", tradeOrderId);
    }

    /**
     * 判断是否需要通知
     *
     * @return true=需要通知, false=不需要通知
     */
    public boolean needNotify() {
        return this.notifyConfig != null && this.notifyConfig.needNotify();
    }

    /**
     * 添加领域事件
     */
    public void addDomainEvent(DomainEvent event) {
        this.domainEvents.add(event);
    }

    /**
     * 获取并清空领域事件
     */
    public List<DomainEvent> getDomainEvents() {
        List<DomainEvent> events = new ArrayList<>(this.domainEvents);
        this.domainEvents.clear();
        return events;
    }
}
