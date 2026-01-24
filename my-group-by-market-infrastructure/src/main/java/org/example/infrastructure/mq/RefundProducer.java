package org.example.infrastructure.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.model.trade.message.RefundMessage;
import org.example.domain.service.refund.IRefundFallbackService;
import org.example.infrastructure.mq.config.RefundQueueConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 退款消息生产者
 *
 * <p>
 * 职责：发送退款消息到RabbitMQ队列
 *
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefundProducer implements IRefundFallbackService {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送消息到降级队列（实现IRefundFallbackService接口）
     *
     * @param message 退款消息
     */
    @Override
    public void sendToFallbackQueue(RefundMessage message) {
        sendRefundMessage(message);
    }

    /**
     * 发送退款消息
     *
     * @param message 退款消息
     */
    public void sendRefundMessage(RefundMessage message) {
        try {
            rabbitTemplate.convertAndSend(
                    RefundQueueConfig.REFUND_EXCHANGE,
                    RefundQueueConfig.REFUND_ROUTING_KEY,
                    message);

            log.info("【退款生产者】发送退款消息成功, tradeOrderId: {}, retryCount: {}",
                    message.getTradeOrderId(), message.getRetryCount());

        } catch (Exception e) {
            log.error("【退款生产者】发送退款消息失败, tradeOrderId: {}",
                    message.getTradeOrderId(), e);
            throw new RuntimeException("发送退款消息失败", e);
        }
    }

    /**
     * 发送消息到死信队列（用于重试）
     *
     * @param message 退款消息
     */
    public void sendToDlq(RefundMessage message) {
        try {
            message.incrementRetryCount();

            rabbitTemplate.convertAndSend(
                    RefundQueueConfig.REFUND_DLX,
                    RefundQueueConfig.REFUND_DLQ_ROUTING_KEY,
                    message);

            log.warn("【退款生产者】发送消息到死信队列, tradeOrderId: {}, retryCount: {}",
                    message.getTradeOrderId(), message.getRetryCount());

        } catch (Exception e) {
            log.error("【退款生产者】发送到死信队列失败, tradeOrderId: {}",
                    message.getTradeOrderId(), e);
        }
    }
}
