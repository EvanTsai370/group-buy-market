package org.example.domain.model.account.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.example.domain.shared.DomainEvent;

import java.time.LocalDateTime;

/**
 * 参团次数扣减事件
 */
@Data
@AllArgsConstructor
public class ParticipationCountDeductedEvent implements DomainEvent {

    private String accountId;
    private String userId;
    private String activityId;
    private LocalDateTime occurredOn;

    public ParticipationCountDeductedEvent(String accountId, String userId, String activityId) {
        this.accountId = accountId;
        this.userId = userId;
        this.activityId = activityId;
        this.occurredOn = LocalDateTime.now();
    }

    @Override
    public LocalDateTime occurredOn() {
        return occurredOn;
    }

    @Override
    public String eventType() {
        return "ParticipationCountDeducted";
    }
}