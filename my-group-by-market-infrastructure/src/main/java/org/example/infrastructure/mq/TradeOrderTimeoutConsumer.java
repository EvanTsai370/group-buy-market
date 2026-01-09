package org.example.infrastructure.mq;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.model.trade.TradeOrder;
import org.example.domain.model.trade.message.TradeOrderTimeoutMessage;
import org.example.domain.model.trade.repository.TradeOrderRepository;
import org.example.domain.model.trade.valueobject.TradeStatus;
import org.example.domain.service.RefundService;
import org.example.infrastructure.config.RabbitMQDelayConfig;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * TradeOrder超时消息消费者
 *
 * <p>
 * 职责：
 * <ul>
 * <li>接收延迟消息（30分钟后到期）</li>
 * <li>检查TradeOrder当前状态</li>
 * <li>如果仍未支付（status = CREATE），执行退单</li>
 * </ul>
 *
 * <p>
 * 幂等性保证：
 * <ul>
 * <li>通过状态检查防止重复退单</li>
 * <li>已支付/已退单的订单会被忽略</li>
 * </ul>
 *
 * @author 开发团队
 * @since 2026-01-08
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TradeOrderTimeoutConsumer {

    private final TradeOrderRepository tradeOrderRepository;
    private final RefundService refundService;

    /**
     * 处理超时消息
     *
     * <p>
     * 业务逻辑：
     * <ol>
     * <li>查询TradeOrder当前状态</li>
     * <li>如果状态仍为CREATE（未支付），执行退单</li>
     * <li>如果已支付或已退单，忽略消息</li>
     * </ol>
     *
     * <p>
     * 异常处理：
     * <ul>
     * <li>处理成功：ACK消息</li>
     * <li>处理失败：NACK消息，不重新入队（避免死循环）</li>
     * </ul>
     */
    @RabbitListener(queues = RabbitMQDelayConfig.TIMEOUT_QUEUE)
    public void handleTimeout(TradeOrderTimeoutMessage message, Message mqMessage, Channel channel) {
        long deliveryTag = mqMessage.getMessageProperties().getDeliveryTag();

        try {
            log.info("【TradeOrder超时】收到超时消息, tradeOrderId={}", message.getTradeOrderId());

            // 1. 查询TradeOrder当前状态
            Optional<TradeOrder> tradeOrderOpt = tradeOrderRepository
                    .findByTradeOrderId(message.getTradeOrderId());

            if (tradeOrderOpt.isEmpty()) {
                log.warn("【TradeOrder超时】交易订单不存在, tradeOrderId={}",
                        message.getTradeOrderId());
                channel.basicAck(deliveryTag, false);
                return;
            }

            TradeOrder tradeOrder = tradeOrderOpt.get();

            // 2. 检查状态
            if (tradeOrder.getStatus() != TradeStatus.CREATE) {
                log.info("【TradeOrder超时】订单状态已变更，无需处理, tradeOrderId={}, status={}",
                        message.getTradeOrderId(), tradeOrder.getStatus());
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 3. 执行退单
            log.info("【TradeOrder超时】开始执行退单, tradeOrderId={}", message.getTradeOrderId());
            refundService.refundTradeOrder(message.getTradeOrderId(), "超时未支付自动退单");

            log.info("【TradeOrder超时】退单成功, tradeOrderId={}", message.getTradeOrderId());

            // 4. 确认消息
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("【TradeOrder超时】处理失败, tradeOrderId={}",
                    message.getTradeOrderId(), e);

            try {
                // 拒绝消息，不重新入队（避免死循环）
                channel.basicNack(deliveryTag, false, false);
            } catch (Exception ex) {
                log.error("【TradeOrder超时】拒绝消息失败", ex);
            }
        }
    }
}
