package org.example.infrastructure.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.gateway.InventoryGateway;
import org.example.domain.model.trade.event.GroupCompleteEvent;
import org.example.domain.model.trade.event.GroupCompleteEvent.ParticipantInfo;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 拼团成功事件消费者
 * 
 * <p>
 * 处理拼团成功后的后续操作：
 * 1. 扣减库存
 * 2. 发送通知
 * </p>
 * 
 * @author 开发团队
 * @since 2026-01-10
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GroupCompleteEventConsumer {

    private final InventoryGateway inventoryGateway;

    @RabbitListener(queues = "group.complete.queue")
    public void handleGroupCompleteEvent(GroupCompleteEvent event) {
        log.info("【GroupCompleteConsumer】收到拼团成功事件, eventId: {}, teamId: {}",
                event.getEventId(), event.getTeamId());

        try {
            // 1. 扣减库存
            deductInventory(event);

            // 2. 发送通知（可扩展）
            sendNotifications(event);

            log.info("【GroupCompleteConsumer】拼团成功事件处理完成, eventId: {}", event.getEventId());
        } catch (Exception e) {
            log.error("【GroupCompleteConsumer】拼团成功事件处理失败, eventId: {}", event.getEventId(), e);
            throw e; // 重新抛出以触发重试
        }
    }

    /**
     * 扣减库存
     */
    private void deductInventory(GroupCompleteEvent event) {
        log.info("【GroupCompleteConsumer】开始扣减库存, teamId: {}", event.getTeamId());

        for (ParticipantInfo participant : event.getParticipants()) {
            try {
                boolean success = inventoryGateway.deductStock(
                        participant.getSkuId(),
                        participant.getQuantity());

                if (success) {
                    log.info("【GroupCompleteConsumer】库存扣减成功, skuId: {}, quantity: {}",
                            participant.getSkuId(), participant.getQuantity());
                } else {
                    log.warn("【GroupCompleteConsumer】库存扣减失败, skuId: {}, quantity: {}",
                            participant.getSkuId(), participant.getQuantity());
                }
            } catch (Exception e) {
                log.error("【GroupCompleteConsumer】库存扣减异常, skuId: {}", participant.getSkuId(), e);
            }
        }
    }

    /**
     * 发送通知
     */
    private void sendNotifications(GroupCompleteEvent event) {
        log.info("【GroupCompleteConsumer】发送拼团成功通知, teamId: {}", event.getTeamId());

        // TODO: 实现通知逻辑（短信、推送、邮件等）
        // 可以集成已有的通知服务

        for (ParticipantInfo participant : event.getParticipants()) {
            log.info("【GroupCompleteConsumer】通知用户, userId: {}, orderId: {}",
                    participant.getUserId(), participant.getOrderId());
        }
    }
}
