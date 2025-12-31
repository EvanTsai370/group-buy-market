// ============ 文件: domain/model/order/event/OrderCreatedEvent.java ============
package org.example.domain.model.order.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.example.domain.shared.DomainEvent;

import java.time.LocalDateTime;

/**
 * 拼团订单创建事件
 */
@Data
@AllArgsConstructor
public class OrderCreatedEvent implements DomainEvent {

    private String orderId;
    private String leaderUserId;
    private String activityId;
    private LocalDateTime occurredOn;

    public OrderCreatedEvent(String orderId, String leaderUserId, String activityId) {
        this.orderId = orderId;
        this.leaderUserId = leaderUserId;
        this.activityId = activityId;
        this.occurredOn = LocalDateTime.now();
    }

    @Override
    public LocalDateTime occurredOn() {
        return occurredOn;
    }

    @Override
    public String eventType() {
        return "OrderCreated";
    }
}