package org.example.infrastructure.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.model.trade.event.GroupCompleteEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 拼团成功事件生产者
 * 
 * @author 开发团队
 * @since 2026-01-10
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GroupCompleteEventProducer {

    /** 拼团成功事件交换机 */
    public static final String EXCHANGE_GROUP_COMPLETE = "group.complete.exchange";

    /** 拼团成功路由键 */
    public static final String ROUTING_KEY_GROUP_COMPLETE = "group.complete";

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送拼团成功事件
     */
    public void sendGroupCompleteEvent(GroupCompleteEvent event) {
        log.info("【GroupCompleteProducer】发送拼团成功事件, eventId: {}, teamId: {}",
                event.getEventId(), event.getTeamId());

        try {
            rabbitTemplate.convertAndSend(EXCHANGE_GROUP_COMPLETE, ROUTING_KEY_GROUP_COMPLETE, event);
            log.info("【GroupCompleteProducer】拼团成功事件发送成功, eventId: {}", event.getEventId());
        } catch (Exception e) {
            log.error("【GroupCompleteProducer】拼团成功事件发送失败, eventId: {}", event.getEventId(), e);
            throw e;
        }
    }
}
