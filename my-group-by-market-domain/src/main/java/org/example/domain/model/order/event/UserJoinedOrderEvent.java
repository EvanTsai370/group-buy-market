// ============ 文件: domain/model/order/event/UserJoinedOrderEvent.java ============
package org.example.domain.model.order.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.example.domain.shared.DomainEvent;

import java.time.LocalDateTime;

/**
 * 用户加入拼团事件
 */
@Data
@AllArgsConstructor
public class UserJoinedOrderEvent implements DomainEvent {

    private String orderId;
    private String userId;
    private Integer completeCount;
    private Integer targetCount;
    private LocalDateTime occurredOn;

    public UserJoinedOrderEvent(String orderId, String userId, Integer completeCount, Integer targetCount) {
        this.orderId = orderId;
        this.userId = userId;
        this.completeCount = completeCount;
        this.targetCount = targetCount;
        this.occurredOn = LocalDateTime.now();
    }

    @Override
    public LocalDateTime occurredOn() {
        return occurredOn;
    }

    @Override
    public String eventType() {
        return "UserJoinedOrder";
    }
}