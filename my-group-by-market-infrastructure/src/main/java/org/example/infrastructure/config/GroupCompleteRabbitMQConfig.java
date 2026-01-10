package org.example.infrastructure.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 拼团成功事件 RabbitMQ 配置
 * 
 * @author 开发团队
 * @since 2026-01-10
 */
@Configuration
public class GroupCompleteRabbitMQConfig {

    public static final String EXCHANGE_GROUP_COMPLETE = "group.complete.exchange";
    public static final String QUEUE_GROUP_COMPLETE = "group.complete.queue";
    public static final String ROUTING_KEY_GROUP_COMPLETE = "group.complete";

    /**
     * 拼团成功交换机
     */
    @Bean
    public DirectExchange groupCompleteExchange() {
        return new DirectExchange(EXCHANGE_GROUP_COMPLETE, true, false);
    }

    /**
     * 拼团成功队列
     */
    @Bean
    public Queue groupCompleteQueue() {
        return QueueBuilder.durable(QUEUE_GROUP_COMPLETE)
                .build();
    }

    /**
     * 绑定队列到交换机
     */
    @Bean
    public Binding groupCompleteBinding(Queue groupCompleteQueue, DirectExchange groupCompleteExchange) {
        return BindingBuilder.bind(groupCompleteQueue)
                .to(groupCompleteExchange)
                .with(ROUTING_KEY_GROUP_COMPLETE);
    }
}
