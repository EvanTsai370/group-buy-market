package org.example.domain.model.order;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.example.common.exception.BizException;
import org.example.domain.model.order.event.OrderCompletedEvent;
import org.example.domain.model.order.event.OrderCreatedEvent;
import org.example.domain.model.order.event.OrderFailedEvent;
import org.example.domain.model.order.event.UserJoinedOrderEvent;
import org.example.domain.model.order.valueobject.Money;
import org.example.domain.model.order.valueobject.OrderStatus;
import org.example.domain.model.order.valueobject.UserType;
import org.example.domain.shared.DomainEvent;
import org.example.domain.shared.IdGenerator;

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

    /** 活动ID（外部引用） */
    private String activityId;

    /** 商品ID（外部引用） */
    private String goodsId;

    /** 团长用户ID */
    private String leaderUserId;

    /** 目标人数 */
    private Integer targetCount;

    /** 完成人数 */
    private Integer completeCount;

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

    /** 订单明细列表（内部实体） */
    private List<OrderDetail> details = new ArrayList<>();

    /** 领域事件列表 */
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    /**
     * 创建拼团（团长发起）
     * 工厂方法
     */
    public static Order create(
            String orderId,
            String activityId,
            String goodsId,
            String leaderUserId,
            Integer targetCount,
            Money price,
            LocalDateTime deadlineTime,
            String source,
            String channel,
            IdGenerator idGenerator) {

        // 参数校验
        if (targetCount == null || targetCount <= 0) {
            throw new BizException("目标人数必须大于0");
        }

        Order order = new Order();
        order.orderId = orderId;
        order.activityId = activityId;
        order.goodsId = goodsId;
        order.leaderUserId = leaderUserId;
        order.targetCount = targetCount;
        order.completeCount = 1; // 团长算一人
        order.status = OrderStatus.PENDING;
        order.originalPrice = price.getOriginalPrice();
        order.deductionPrice = price.getDeductionPrice();
        order.startTime = LocalDateTime.now();
        order.deadlineTime = deadlineTime;
        order.completedTime = null; // 初始未成团
        order.source = source;
        order.channel = channel;
        order.version = 1L;

        // 创建团长明细
        OrderDetail leaderDetail = OrderDetail.create(
                leaderUserId,
                UserType.LEADER,
                null,
                price.getDeductionPrice(),
                idGenerator
        );
        order.details.add(leaderDetail);

        // 发出领域事件
        order.addDomainEvent(new OrderCreatedEvent(orderId, leaderUserId, activityId));

        log.info("【Order聚合】拼团创建成功, orderId: {}, targetCount: {}", orderId, targetCount);
        return order;
    }

    /**
     * 校验是否可以加入拼团（业务规则校验）
     *
     * 设计说明：
     * - 领域模型负责业务规则的表达和校验
     * - 并发安全由 Repository 层通过数据库原子操作保证
     * - 这种设计既保持了 DDD 的富领域模型，又解决了高并发下的误杀问题
     *
     * @param userId 用户ID
     * @throws BizException 如果不满足业务规则
     */
    public void validateJoin(String userId) {
        // 业务规则1：检查状态
        if (this.status != OrderStatus.PENDING) {
            throw new BizException("拼团已结束，当前状态: " + this.status);
        }

        // 业务规则2：检查是否已满
        if (this.completeCount >= this.targetCount) {
            throw new BizException("拼团已满");
        }

        // 业务规则3：检查是否已过期
        if (LocalDateTime.now().isAfter(this.deadlineTime)) {
            throw new BizException("拼团已过期");
        }

        // 业务规则4：防止重复加入
        boolean alreadyJoined = this.details.stream()
                .anyMatch(d -> d.getUserId().equals(userId));
        if (alreadyJoined) {
            throw new BizException("您已参与该拼团");
        }

        log.debug("【Order聚合】用户加入校验通过, orderId: {}, userId: {}", orderId, userId);
    }

    /**
     * 创建订单明细（工厂方法）
     *
     * 注意：此方法不修改聚合状态（completeCount），
     * 状态变更由 Repository 层的原子操作完成
     *
     * @param userId 用户ID
     * @param outTradeNo 外部交易单号
     * @param payAmount 支付金额
     * @param idGenerator ID生成器
     * @return 订单明细
     */
    public OrderDetail createDetail(String userId, String outTradeNo,
                                    BigDecimal payAmount, IdGenerator idGenerator) {
        return OrderDetail.create(
                userId,
                UserType.MEMBER,
                outTradeNo,
                payAmount,
                idGenerator
        );
    }

    /**
     * 用户加入拼团（团员）- 保留原方法用于低并发场景或测试
     *
     * @deprecated 高并发场景请使用 validateJoin + Repository.tryIncrementCompleteCount
     */
    @Deprecated
    public OrderDetail join(String userId, String outTradeNo, BigDecimal payAmount, IdGenerator idGenerator) {
        // 业务规则校验
        validateJoin(userId);

        // 状态变更
        OrderDetail detail = OrderDetail.create(
                userId,
                UserType.MEMBER,
                outTradeNo,
                payAmount,
                idGenerator
        );
        this.details.add(detail);
        this.completeCount++;
        // 乐观锁版本号递增由Repository层的update操作控制

        // 发出事件
        this.addDomainEvent(new UserJoinedOrderEvent(orderId, userId, completeCount, targetCount));

        log.info("【Order聚合】用户加入成功, orderId: {}, userId: {}, progress: {}/{}",
                orderId, userId, completeCount, targetCount);

        // 判断是否成团
        if (this.completeCount >= this.targetCount) {
            this.markAsCompleted();
        }

        return detail;
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
     * 检查是否可以加入
     */
    public boolean canJoin() {
        return this.status == OrderStatus.PENDING
                && this.completeCount < this.targetCount
                && LocalDateTime.now().isBefore(this.deadlineTime);
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