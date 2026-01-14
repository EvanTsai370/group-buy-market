package org.example.infrastructure.mq.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 通知队列配置
 *
 * <p>
 * 用于异步发送退款通知（短信/邮件/推送）
 *
 * @author 开发团队
 * @since 2026-01-09
 */
@Configuration
public class NotificationQueueConfig {

    /** 通知队列名称 */
    public static final String NOTIFICATION_QUEUE = "refund.notification.queue";

    /** 通知交换机名称 */
    public static final String NOTIFICATION_EXCHANGE = "refund.notification.exchange";

    /** 通知路由键 */
    public static final String NOTIFICATION_ROUTING_KEY = "refund.notification.key";

    /**
     * 通知队列
     */
    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE).build();
    }

    /**
     * 通知交换机
     */
    @Bean
    public DirectExchange notificationExchange() {
        return new DirectExchange(NOTIFICATION_EXCHANGE, true, false);
    }

    /**
     * 绑定通知队列到交换机
     */
    @Bean
    public Binding notificationBinding(@Qualifier("notificationQueue") Queue notificationQueue,
                                       @Qualifier("notificationExchange") DirectExchange notificationExchange) {
        return BindingBuilder.bind(notificationQueue)
                .to(notificationExchange)
                .with(NOTIFICATION_ROUTING_KEY);
    }
}
