package org.example.infrastructure.mq;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.model.trade.message.RefundNotificationMessage;
import org.example.domain.service.notification.MessageNotificationStrategy;
import org.example.infrastructure.mq.config.NotificationQueueConfig;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 退款通知消费者
 *
 * <p>
 * 职责：
 * <ul>
 * <li>消费通知队列中的消息</li>
 * <li>根据通知类型选择对应的策略</li>
 * <li>发送通知（短信/邮件/推送）</li>
 * </ul>
 *
 * @author 开发团队
 * @since 2026-01-09
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefundNotificationConsumer {

    private final List<MessageNotificationStrategy> notificationStrategies;

    /**
     * 处理通知消息
     *
     * @param notificationMessage 通知消息
     * @param message             RabbitMQ消息
     * @param channel             RabbitMQ通道
     */
    @RabbitListener(queues = NotificationQueueConfig.NOTIFICATION_QUEUE)
    public void handleNotification(RefundNotificationMessage notificationMessage,
            Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            log.info("【通知消费者】收到通知消息, tradeOrderId: {}, type: {}",
                    notificationMessage.getTradeOrderId(), notificationMessage.getNotificationType());

            // 根据通知类型选择策略
            MessageNotificationStrategy strategy = notificationStrategies.stream()
                    .filter(s -> s.supports(notificationMessage.getNotificationType()))
                    .findFirst()
                    .orElse(null);

            if (strategy == null) {
                log.warn("【通知消费者】未找到对应的通知策略, type: {}",
                        notificationMessage.getNotificationType());
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 发送通知
            strategy.send(notificationMessage);

            log.info("【通知消费者】通知发送成功, tradeOrderId: {}",
                    notificationMessage.getTradeOrderId());

            // 确认消息
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("【通知消费者】通知发送失败, tradeOrderId: {}",
                    notificationMessage.getTradeOrderId(), e);

            try {
                // 通知失败不重试，直接确认消息
                channel.basicAck(deliveryTag, false);
            } catch (Exception ex) {
                log.error("【通知消费者】确认消息失败", ex);
            }
        }
    }
}
