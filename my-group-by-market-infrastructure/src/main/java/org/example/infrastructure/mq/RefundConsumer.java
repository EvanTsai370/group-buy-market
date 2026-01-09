package org.example.infrastructure.mq;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.model.trade.message.RefundMessage;
import org.example.domain.service.RefundService;
import org.example.infrastructure.mq.config.RefundQueueConfig;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 退款消息消费者
 *
 * <p>
 * 职责：
 * <ul>
 * <li>消费退款队列中的消息</li>
 * <li>执行退款操作</li>
 * <li>失败时进入死信队列重试（最多3次）</li>
 * </ul>
 *
 * @author 开发团队
 * @since 2026-01-09
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefundConsumer {

    private final RefundService refundService;
    private final RefundProducer refundProducer;

    /**
     * 处理退款消息
     *
     * <p>
     * 重试策略：
     * <ul>
     * <li>第1次失败：5秒后重试</li>
     * <li>第2次失败：5秒后重试</li>
     * <li>第3次失败：5秒后重试</li>
     * <li>超过3次：记录日志，人工介入</li>
     * </ul>
     *
     * @param refundMessage 退款消息
     * @param message       RabbitMQ消息
     * @param channel       RabbitMQ通道
     */
    @RabbitListener(queues = RefundQueueConfig.REFUND_QUEUE)
    public void handleRefund(RefundMessage refundMessage, Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            log.info("【退款消费者】收到退款消息, tradeOrderId: {}, retryCount: {}",
                    refundMessage.getTradeOrderId(), refundMessage.getRetryCount());

            // 检查是否超过最大重试次数
            if (refundMessage.exceedsMaxRetries(RefundQueueConfig.MAX_RETRY_COUNT)) {
                log.error("【退款消费者】超过最大重试次数, tradeOrderId: {}, retryCount: {}, 需要人工介入",
                        refundMessage.getTradeOrderId(), refundMessage.getRetryCount());

                // TODO: 发送告警通知运维人员
                // TODO: 记录到失败表，等待人工处理

                // 确认消息，避免无限循环
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 执行退款
            refundService.refundTradeOrder(
                    refundMessage.getTradeOrderId(),
                    refundMessage.getReason());

            log.info("【退款消费者】退款成功, tradeOrderId: {}", refundMessage.getTradeOrderId());

            // 确认消息
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("【退款消费者】退款失败, tradeOrderId: {}, retryCount: {}, error: {}",
                    refundMessage.getTradeOrderId(), refundMessage.getRetryCount(), e.getMessage());

            try {
                // 拒绝消息，不重新入队（会进入死信队列）
                channel.basicNack(deliveryTag, false, false);

                // 发送到死信队列进行重试
                refundProducer.sendToDlq(refundMessage);

            } catch (Exception ex) {
                log.error("【退款消费者】处理失败消息异常", ex);
            }
        }
    }
}
