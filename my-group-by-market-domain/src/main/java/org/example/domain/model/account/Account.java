package org.example.domain.model.account;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.example.common.exception.BizException;
import org.example.common.exception.ErrorCode;
import org.example.domain.model.account.event.ParticipationCountCompensatedEvent;
import org.example.domain.model.account.event.ParticipationCountDeductedEvent;
import org.example.domain.model.activity.Activity;
import org.example.domain.shared.DomainEvent;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Account 聚合根（用户拼团账户）
 * 职责：管理用户的参团次数限制
 */
@Slf4j
@Data
public class Account {

    /** 账户ID */
    private String accountId;

    /** 用户ID */
    private String userId;

    /** 活动ID */
    private String activityId;

    /** 已使用次数 */
    private Integer participationCount;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 乐观锁版本号 */
    private Long version;

    /** 领域事件列表 */
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    /**
     * 创建账户（工厂方法）
     */
    public static Account create(String accountId, String userId, String activityId, Integer participationLimit) {
        Account account = new Account();
        account.accountId = accountId;
        account.userId = userId;
        account.activityId = activityId;
        // participationLimit 不再存储，从 Activity 获取
        account.participationCount = 0;
        account.createTime = LocalDateTime.now();
        account.updateTime = LocalDateTime.now();

        log.info("【Account聚合】账户创建成功, accountId: {}, userId: {}",
                accountId, userId);
        return account;
    }

    /**
     * 扣减参团次数
     * 
     * @param activity 活动聚合（用于获取最新的限制次数并校验）
     * @throws BizException 如果参团次数已达上限
     */
    public void deductCount(Activity activity) {
        // 从 Activity 获取最新的限制次数
        Integer limit = activity.getParticipationLimit();

        // 业务规则：检查是否已用尽
        if (limit != null && limit > 0 && this.participationCount >= limit) {
            throw new BizException(
                    ErrorCode.ACCOUNT_PARTICIPATION_LIMIT_REACHED,
                    this.participationCount,
                    limit);
        }

        // 状态变更
        this.participationCount++;
        this.updateTime = LocalDateTime.now();

        // 发出事件
        this.addDomainEvent(new ParticipationCountDeductedEvent(accountId, userId, activityId));

        log.info("【Account聚合】参团次数扣减成功, accountId: {}, used: {}, limit: {}",
                accountId, participationCount, limit);
    }

    /**
     * 补偿：恢复参团次数
     * 场景：订单创建失败时调用
     */
    public void compensateCount() {
        if (this.participationCount > 0) {
            this.participationCount--;
            this.updateTime = LocalDateTime.now();

            this.addDomainEvent(new ParticipationCountCompensatedEvent(accountId, userId));

            log.warn("【Account聚合】参团次数已补偿, accountId: {}, used: {}",
                    accountId, participationCount);
        }
    }

    /**
     * 检查是否还有可用次数
     * 
     * @param activity 活动聚合（用于获取最新的限制次数）
     * @return true=还有可用次数, false=已达上限
     */
    public boolean hasAvailableCount(Activity activity) {
        Integer limit = activity.getParticipationLimit();
        if (limit == null || limit == 0) {
            return true; // 未设置限制，允许无限参与
        }
        return this.participationCount < limit;
    }

    /**
     * 获取剩余次数
     * 
     * @param activity 活动聚合（用于获取最新的限制次数）
     * @return 剩余次数
     */
    public Integer getRemainingCount(Activity activity) {
        Integer limit = activity.getParticipationLimit();
        if (limit == null || limit == 0) {
            return Integer.MAX_VALUE; // 未设置限制
        }
        return limit - this.participationCount;
    }

    /**
     * 断言用户还有可用的参与次数（守卫方法）
     * 
     * <p>
     * 用于交易前的用户参与限制校验，确保用户未超过活动的参与次数限制。
     * 这是一个守卫方法（Guard Method），如果参与次数已达上限会抛出业务异常。
     * 
     * <p>
     * 设计说明：
     * <ul>
     * <li>从 Activity 获取最新的 participationLimit，避免数据冗余和不一致</li>
     * <li>如果活动未设置限制（null 或 0），则允许无限参与</li>
     * <li>使用 Account 中的 participationCount 作为已使用次数</li>
     * </ul>
     * 
     * @param activity 活动聚合（从外部传入，获取最新的限制配置）
     * @throws BizException 如果参与次数已达上限
     */
    public void assertHasAvailableCount(Activity activity) {
        // 从 Activity 获取最新的限制次数（避免使用冗余的 this.participationLimit）
        Integer limit = activity.getParticipationLimit();

        // 如果活动未设置参与限制，允许无限参与
        if (limit == null || limit == 0) {
            log.debug("【Account聚合】活动未设置参与限制，跳过校验, activityId: {}", this.activityId);
            return;
        }

        // 校验是否超过限制
        if (this.participationCount >= limit) {
            throw new BizException(
                    ErrorCode.ACCOUNT_PARTICIPATION_LIMIT_REACHED,
                    this.participationCount,
                    limit);
        }

        log.debug("【Account聚合】参与次数校验通过, userId: {}, used: {}, limit: {}",
                this.userId, this.participationCount, limit);
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