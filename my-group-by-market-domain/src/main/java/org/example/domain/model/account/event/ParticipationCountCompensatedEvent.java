package org.example.domain.model.account.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.example.domain.shared.DomainEvent;

import java.time.LocalDateTime;

/**
 * 参团次数补偿事件
 */
@Data
@AllArgsConstructor
public class ParticipationCountCompensatedEvent implements DomainEvent {

    private String accountId;
    private String userId;
    private LocalDateTime occurredOn;

    public ParticipationCountCompensatedEvent(String accountId, String userId) {
        this.accountId = accountId;
        this.userId = userId;
        this.occurredOn = LocalDateTime.now();
    }

    @Override
    public LocalDateTime occurredOn() {
        return occurredOn;
    }

    @Override
    public String eventType() {
        return "ParticipationCountCompensated";
    }
}