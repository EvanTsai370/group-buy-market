// ============ 文件: domain/model/order/event/OrderFailedEvent.java ============
package org.example.domain.model.order.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.example.domain.shared.DomainEvent;

import java.time.LocalDateTime;

/**
 * 拼团失败事件
 */
@Data
@AllArgsConstructor
public class OrderFailedEvent implements DomainEvent {

    private String orderId;
    private String reason;
    private LocalDateTime occurredOn;

    public OrderFailedEvent(String orderId, String reason) {
        this.orderId = orderId;
        this.reason = reason;
        this.occurredOn = LocalDateTime.now();
    }

    @Override
    public LocalDateTime occurredOn() {
        return occurredOn;
    }

    @Override
    public String eventType() {
        return "OrderFailed";
    }
}