// ============ 文件: domain/model/order/event/OrderCompletedEvent.java ============
package org.example.domain.model.order.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.example.domain.shared.DomainEvent;

import java.time.LocalDateTime;

/**
 * 拼团完成事件
 */
@Data
@AllArgsConstructor
public class OrderCompletedEvent implements DomainEvent {


    private String orderId;
    private String activityId;
    private LocalDateTime occurredOn;

    public OrderCompletedEvent(String orderId, String activityId) {
        this.orderId = orderId;
        this.activityId = activityId;
        this.occurredOn = LocalDateTime.now();
    }

    @Override
    public LocalDateTime occurredOn() {
        return occurredOn;
    }

    @Override
    public String eventType() {
        return "OrderCompleted";
    }
}