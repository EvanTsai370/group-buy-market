package org.example.infrastructure.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ延迟队列配置
 *
 * <p>
 * 用于TradeOrder超时处理
 *
 * <p>
 * 使用插件：rabbitmq_delayed_message_exchange
 *
 * @author 开发团队
 * @since 2026-01-08
 */
@Configuration
public class RabbitMQDelayConfig {

    /** 延迟交换机 */
    public static final String DELAY_EXCHANGE = "trade.order.timeout.delay.exchange";

    /** 超时队列 */
    public static final String TIMEOUT_QUEUE = "trade.order.timeout.queue";

    /** 路由键 */
    public static final String ROUTING_KEY = "trade.order.timeout";

    /** 默认支付超时时间（秒）- 30分钟 */
    public static final int DEFAULT_PAYMENT_TIMEOUT_SECONDS = 30 * 60;

    /**
     * 延迟交换机
     * 
     * <p>
     * 类型：x-delayed-message
     * <p>
     * 需要安装插件：rabbitmq_delayed_message_exchange
     * 
     * <p>
     * 安装命令：
     * 
     * <pre>
     * rabbitmq-plugins enable rabbitmq_delayed_message_exchange
     * </pre>
     */
    @Bean
    public CustomExchange delayExchange() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-delayed-type", "direct");

        return new CustomExchange(
                DELAY_EXCHANGE,
                "x-delayed-message",
                true, // durable
                false, // autoDelete
                args);
    }

    /**
     * 超时队列
     */
    @Bean
    public Queue timeoutQueue() {
        return QueueBuilder.durable(TIMEOUT_QUEUE).build();
    }

    /**
     * 绑定关系
     */
    @Bean
    public Binding timeoutBinding(Queue timeoutQueue, CustomExchange delayExchange) {
        return BindingBuilder
                .bind(timeoutQueue)
                .to(delayExchange)
                .with(ROUTING_KEY)
                .noargs();
    }
}
