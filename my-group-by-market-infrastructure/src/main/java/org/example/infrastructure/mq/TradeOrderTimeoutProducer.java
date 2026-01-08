package org.example.infrastructure.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.model.trade.event.TradeOrderTimeoutMessage;
import org.example.domain.service.timeout.ITimeoutMessageProducer;
import org.example.infrastructure.config.RabbitMQDelayConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * TradeOrder超时消息生产者
 *
 * <p>
 * 职责：
 * <ul>
 * <li>在用户锁单时发送延迟消息</li>
 * <li>延迟时间默认30分钟（可配置）</li>
 * <li>消息到期后由消费者处理超时退单</li>
 * </ul>
 *
 * @author 开发团队
 * @since 2026-01-08
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TradeOrderTimeoutProducer implements ITimeoutMessageProducer {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void sendDelayMessage(TradeOrderTimeoutMessage message) {
        sendDelayMessage(message, RabbitMQDelayConfig.DEFAULT_PAYMENT_TIMEOUT_SECONDS);
    }

    @Override
    public void sendDelayMessage(TradeOrderTimeoutMessage message, int delaySeconds) {
        try {
            // 设置延迟时间（毫秒）
            rabbitTemplate.convertAndSend(
                    RabbitMQDelayConfig.DELAY_EXCHANGE,
                    RabbitMQDelayConfig.ROUTING_KEY,
                    message,
                    msg -> {
                        msg.getMessageProperties().setDelay(delaySeconds * 1000);
                        return msg;
                    });

            log.info("【TradeOrder超时】发送延迟消息成功, tradeOrderId={}, delaySeconds={}",
                    message.getTradeOrderId(), delaySeconds);

        } catch (Exception e) {
            log.error("【TradeOrder超时】发送延迟消息失败, tradeOrderId={}",
                    message.getTradeOrderId(), e);
            // 不抛异常，避免影响主流程
        }
    }
}
