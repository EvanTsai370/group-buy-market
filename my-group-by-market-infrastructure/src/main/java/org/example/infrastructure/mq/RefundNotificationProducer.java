package org.example.infrastructure.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.model.trade.message.RefundNotificationMessage;
import org.example.infrastructure.mq.config.NotificationQueueConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 退款通知生产者
 *
 * <p>
 * 职责：发送退款通知消息到RabbitMQ队列
 *
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefundNotificationProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送退款通知消息
     *
     * @param message 通知消息
     */
    public void sendNotification(RefundNotificationMessage message) {
        try {
            rabbitTemplate.convertAndSend(
                    NotificationQueueConfig.NOTIFICATION_EXCHANGE,
                    NotificationQueueConfig.NOTIFICATION_ROUTING_KEY,
                    message);

            log.info("【通知生产者】发送通知消息成功, tradeOrderId: {}, type: {}",
                    message.getTradeOrderId(), message.getNotificationType());

        } catch (Exception e) {
            log.error("【通知生产者】发送通知消息失败, tradeOrderId: {}",
                    message.getTradeOrderId(), e);
            // 通知失败不影响主流程，只记录日志
        }
    }
}
