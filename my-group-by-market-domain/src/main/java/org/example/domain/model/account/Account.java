package org.example.domain.model.account;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.example.common.exception.BizException;
import org.example.domain.model.account.event.ParticipationCountCompensatedEvent;
import org.example.domain.model.account.event.ParticipationCountDeductedEvent;
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

    /** 总限制次数 */
    private Integer takeLimitCount;

    /** 已使用次数 */
    private Integer takeLimitCountUsed;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 领域事件列表 */
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    /**
     * 创建账户（工厂方法）
     */
    public static Account create(String accountId, String userId, String activityId, Integer takeLimitCount) {
        Account account = new Account();
        account.accountId = accountId;
        account.userId = userId;
        account.activityId = activityId;
        account.takeLimitCount = takeLimitCount;
        account.takeLimitCountUsed = 0;
        account.createTime = LocalDateTime.now();
        account.updateTime = LocalDateTime.now();

        log.info("【Account聚合】账户创建成功, accountId: {}, userId: {}, limit: {}",
                accountId, userId, takeLimitCount);
        return account;
    }

    /**
     * 扣减参团次数
     * 前置条件：用户资格已校验
     * 后置条件：发出 ParticipationCountDeducted 事件
     */
    public void deductCount() {
        // 业务规则：检查是否已用尽
        if (this.takeLimitCountUsed >= this.takeLimitCount) {
            throw new BizException("参团次数已用完");
        }

        // 状态变更
        this.takeLimitCountUsed++;
        this.updateTime = LocalDateTime.now();

        // 发出事件
        this.addDomainEvent(new ParticipationCountDeductedEvent(accountId, userId, activityId));

        log.info("【Account聚合】参团次数扣减成功, accountId: {}, used: {}/{}",
                accountId, takeLimitCountUsed, takeLimitCount);
    }

    /**
     * 补偿：恢复参团次数
     * 场景：订单创建失败时调用
     */
    public void compensateCount() {
        if (this.takeLimitCountUsed > 0) {
            this.takeLimitCountUsed--;
            this.updateTime = LocalDateTime.now();

            this.addDomainEvent(new ParticipationCountCompensatedEvent(accountId, userId));

            log.warn("【Account聚合】参团次数已补偿, accountId: {}, used: {}/{}",
                    accountId, takeLimitCountUsed, takeLimitCount);
        }
    }

    /**
     * 检查是否还有可用次数
     */
    public boolean hasAvailableCount() {
        return this.takeLimitCountUsed < this.takeLimitCount;
    }

    /**
     * 获取剩余次数
     */
    public Integer getRemainingCount() {
        return this.takeLimitCount - this.takeLimitCountUsed;
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