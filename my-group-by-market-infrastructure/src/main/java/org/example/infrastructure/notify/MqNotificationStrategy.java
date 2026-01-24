package org.example.infrastructure.notify;

import lombok.extern.slf4j.Slf4j;
import org.example.domain.model.notification.NotificationTask;
import org.example.domain.model.trade.valueobject.NotifyType;
import org.springframework.stereotype.Component;

/**
 * MQ通知策略
 *
 * <p>
 * 通过消息队列发送通知
 * <p>
 * TODO: 集成实际的MQ客户端（RabbitMQ/RocketMQ）
 *
 */
@Slf4j
@Component
public class MqNotificationStrategy implements CallbackNotificationStrategy {

    @Override
    public void execute(NotificationTask task) throws Exception {
        String notifyMq = task.getNotifyConfig().getNotifyMq();
        if (notifyMq == null || notifyMq.isEmpty()) {
            throw new IllegalArgumentException("MQ主题不能为空");
        }

        log.info("【MQ通知】开始执行, taskId={}, topic={}", task.getTaskId(), notifyMq);

        // TODO: 实际集成MQ客户端
        // 示例代码：
        // mqTemplate.convertAndSend(notifyMq, buildMessage(task));

        // 暂时只记录日志
        log.info("【MQ通知】发送消息到主题: {}, tradeOrderId={}", notifyMq, task.getTradeOrderId());

        log.info("【MQ通知】执行成功, taskId={}", task.getTaskId());
    }

    @Override
    public boolean supports(NotificationTask task) {
        return task.getNotifyConfig() != null
                && task.getNotifyConfig().getNotifyType() == NotifyType.MQ;
    }
}
