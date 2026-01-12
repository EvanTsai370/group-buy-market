package org.example.domain.model.order;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.example.common.exception.BizException;
import org.example.domain.model.order.event.OrderCompletedEvent;
import org.example.domain.model.order.event.OrderCreatedEvent;
import org.example.domain.model.order.event.OrderFailedEvent;
import org.example.domain.model.order.valueobject.Money;
import org.example.domain.model.order.valueobject.OrderStatus;
import org.example.domain.shared.DomainEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Order 聚合根（拼团订单）
 * 职责：管理拼团的生命周期，包括创建、加入、成团、失败等
 */
@Slf4j
@Data
public class Order {

    /** 拼团订单ID */
    private String orderId;

    /** 拼团队伍ID（8位随机数，用户友好，用于前端展示和快速查询） */
    private String teamId;

    /** 活动ID（外部引用） */
    private String activityId;

    /** 商品ID（外部引用） */
    private String goodsId;

    /** 团长用户ID */
    private String leaderUserId;

    /** 目标人数 */
    private Integer targetCount;

    /** 完成人数（实际支付成功的人数） */
    private Integer completeCount;

    /** 锁单量（已锁定名额数量，防止超卖） */
    private Integer lockCount;

    /** 订单状态 */
    private OrderStatus status;

    /** 原始价格 */
    private BigDecimal originalPrice;

    /** 折扣价格 */
    private BigDecimal deductionPrice;

    /** 拼团开始时间 */
    private LocalDateTime startTime;

    /** 参团截止时间（超时不可参与） */
    private LocalDateTime deadlineTime;

    /** 实际成团时间（成功时记录） */
    private LocalDateTime completedTime;

    /** 回调URL */
    private String notifyUrl;

    /** 来源 */
    private String source;

    /** 渠道 */
    private String channel;

    /** 乐观锁版本号（关键！防止并发超卖） */
    private Long version;

    /** 领域事件列表 */
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    /**
     * 创建拼团（团长发起）
     * 工厂方法
     */
    public static Order create(
            String orderId,
            String teamId,
            String activityId,
            String goodsId,
            String leaderUserId,
            Integer targetCount,
            Money price,
            LocalDateTime deadlineTime,
            String source,
            String channel) {

        // 参数校验
        if (targetCount == null || targetCount <= 0) {
            throw new BizException("目标人数必须大于0");
        }

        Order order = new Order();
        order.orderId = orderId;
        order.teamId = teamId;
        order.activityId = activityId;
        order.goodsId = goodsId;
        order.leaderUserId = leaderUserId;
        order.targetCount = targetCount;
        order.completeCount = 1; // 团长算一人
        order.lockCount = 1; // 团长已锁定1个名额
        order.status = OrderStatus.PENDING;
        order.originalPrice = price.getOriginalPrice();
        order.deductionPrice = price.getDeductionPrice();
        order.startTime = LocalDateTime.now();
        order.deadlineTime = deadlineTime;
        order.completedTime = null; // 初始未成团
        order.source = source;
        order.channel = channel;
        order.version = 1L;

        // 发出领域事件
        order.addDomainEvent(new OrderCreatedEvent(orderId, leaderUserId, activityId));

        log.info("【Order聚合】拼团创建成功, orderId: {}, teamId: {}, targetCount: {}",
                orderId, teamId, targetCount);
        return order;
    }

    /**
     * 标记为已成团
     */
    private void markAsCompleted() {
        this.status = OrderStatus.SUCCESS;
        this.completedTime = LocalDateTime.now(); // 记录实际成团时间
        this.addDomainEvent(new OrderCompletedEvent(this.orderId, this.activityId));
        log.info("【Order聚合】拼团成功, orderId: {}, completedTime: {}", orderId, completedTime);
    }

    /**
     * 虚拟成团（定时任务调用）
     * 适用于"到期自动成团"的活动类型
     */
    public void completeVirtually() {
        if (this.status == OrderStatus.PENDING) {
            this.status = OrderStatus.SUCCESS;
            this.completedTime = LocalDateTime.now(); // 记录虚拟成团时间
            this.addDomainEvent(new OrderCompletedEvent(this.orderId, this.activityId));
            log.info("【Order聚合】虚拟成团成功, orderId: {}, completeCount: {}/{}, completedTime: {}",
                    orderId, completeCount, targetCount, completedTime);
        }
    }

    /**
     * 标记为失败
     */
    public void markAsFailed(String reason) {
        this.status = OrderStatus.FAILED;
        this.addDomainEvent(new OrderFailedEvent(this.orderId, reason));
        log.warn("【Order聚合】拼团失败, orderId: {}, reason: {}", orderId, reason);
    }

    /**
     * 判断拼团是否已完成
     *
     * <p>
     * 业务场景：
     * <ul>
     * <li>退单时判断是否需要恢复Redis库存</li>
     * <li>已完成的拼团不需要恢复Redis库存</li>
     * </ul>
     *
     * @return true=已完成, false=未完成
     */
    public boolean isCompleted() {
        return this.status == OrderStatus.SUCCESS;
    }

    // ==================== 锁单相关业务逻辑 ====================

    /**
     * 校验是否可以锁单
     *
     * <p>
     * 业务规则：
     * <ol>
     * <li>拼团状态必须为PENDING</li>
     * <li>锁单量不能超过目标人数</li>
     * <li>不能超过截止时间</li>
     * </ol>
     *
     * @throws BizException 如果不满足锁单条件
     */
    public void validateLock() {
        // 业务规则1：检查状态
        if (this.status != OrderStatus.PENDING) {
            throw new BizException("拼团已结束，无法锁单，当前状态: " + this.status);
        }

        // 业务规则2：检查锁单量是否已达上限
        if (this.lockCount >= this.targetCount) {
            throw new BizException("拼团锁单已满，lockCount: " + this.lockCount + ", targetCount: " + this.targetCount);
        }

        // 业务规则3：检查是否已过期
        if (LocalDateTime.now().isAfter(this.deadlineTime)) {
            throw new BizException("拼团已过期，无法锁单");
        }

        log.debug("【Order聚合】锁单校验通过, orderId: {}, lockCount: {}/{}", orderId, lockCount, targetCount);
    }

    /**
     * 尝试锁定名额（业务层校验）
     *
     * <p>
     * 设计说明：
     * <ul>
     * <li>此方法只负责业务规则校验，不修改聚合状态</li>
     * <li>实际的锁单量增加由 Repository 层通过数据库原子操作完成</li>
     * <li>这样设计可以保持领域模型的纯粹性，同时保证并发安全</li>
     * </ul>
     *
     * @return true=可以锁单, false=不可以锁单
     */
    public boolean canLock() {
        try {
            validateLock();
            return true;
        } catch (BizException e) {
            log.warn("【Order聚合】锁单校验失败, orderId: {}, reason: {}", orderId, e.getMessage());
            return false;
        }
    }

    /**
     * 锁定名额成功后的回调（由Repository层调用）
     *
     * <p>
     * 用途：
     * <ul>
     * <li>Repository 层执行原子操作成功后，调用此方法同步内存状态</li>
     * <li>保持内存对象和数据库的一致性</li>
     * </ul>
     *
     * @param newLockCount 新的锁单量
     */
    public void onLockSuccess(Integer newLockCount) {
        this.lockCount = newLockCount;
        log.info("【Order聚合】锁单成功, orderId: {}, lockCount: {}/{}", orderId, lockCount, targetCount);
    }

    /**
     * 释放锁定的名额（超时或退单时调用）
     *
     * <p>
     * 业务场景：
     * <ol>
     * <li>用户下单锁定名额，但超时未支付 → 释放名额</li>
     * <li>用户支付失败或主动取消订单 → 释放名额</li>
     * </ol>
     *
     * <p>
     * 注意：此方法也只负责业务校验，实际的减少操作由Repository层完成
     */
    public void validateReleaseLock() {
        if (this.lockCount <= 0) {
            throw new BizException("锁单量已为0，无法释放");
        }

        if (this.lockCount < this.completeCount) {
            throw new BizException(
                    "锁单量不能小于完成人数，lockCount: " + this.lockCount + ", completeCount: " + this.completeCount);
        }

        log.debug("【Order聚合】释放锁单校验通过, orderId: {}, lockCount: {}", orderId, lockCount);
    }

    /**
     * 检查锁单量是否已达上限
     *
     * @return true=已达上限, false=未达上限
     */
    public boolean isLockFull() {
        return this.lockCount >= this.targetCount;
    }

    /**
     * 获取剩余可锁单名额
     *
     * @return 剩余名额数量
     */
    public int getAvailableLockCount() {
        return Math.max(0, this.targetCount - this.lockCount);
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