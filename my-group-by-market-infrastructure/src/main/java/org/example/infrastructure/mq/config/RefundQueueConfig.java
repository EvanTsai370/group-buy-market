package org.example.infrastructure.mq.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * 退款队列配置
 *
 * <p>
 * 降级策略：使用死信队列实现失败重试
 *
 * <p>
 * 队列结构：
 * <ul>
 * <li>主队列：refund.queue</li>
 * <li>死信队列：refund.dlq（Dead Letter Queue）</li>
 * <li>最大重试次数：3次</li>
 * </ul>
 *
 */
@Configuration
public class RefundQueueConfig {

    /** 退款队列名称 */
    public static final String REFUND_QUEUE = "refund.queue";

    /** 退款交换机名称 */
    public static final String REFUND_EXCHANGE = "refund.exchange";

    /** 退款路由键 */
    public static final String REFUND_ROUTING_KEY = "refund.key";

    /** 死信队列名称 */
    public static final String REFUND_DLQ = "refund.dlq";

    /** 死信交换机名称 */
    public static final String REFUND_DLX = "refund.dlx";

    /** 死信路由键 */
    public static final String REFUND_DLQ_ROUTING_KEY = "refund.dlq.key";

    /** 最大重试次数 */
    public static final int MAX_RETRY_COUNT = 3;

    /** 重试延迟时间（毫秒） */
    public static final long RETRY_DELAY_MS = 5000; // 5秒

    /**
     * 退款队列
     * 
     * <p>
     * 配置死信交换机，失败消息会进入死信队列
     */
    @Bean
    public Queue refundQueue() {
        Map<String, Object> args = new HashMap<>();
        // 配置死信交换机
        args.put("x-dead-letter-exchange", REFUND_DLX);
        args.put("x-dead-letter-routing-key", REFUND_DLQ_ROUTING_KEY);

        return QueueBuilder.durable(REFUND_QUEUE)
                .withArguments(args)
                .build();
    }

    /**
     * 退款交换机
     */
    @Bean
    public DirectExchange refundExchange() {
        return new DirectExchange(REFUND_EXCHANGE, true, false);
    }

    /**
     * 绑定退款队列到交换机
     */
    @Bean
    public Binding refundBinding(@Qualifier("refundQueue") Queue refundQueue,
                                  @Qualifier("refundExchange") DirectExchange refundExchange) {
        return BindingBuilder.bind(refundQueue)
                .to(refundExchange)
                .with(REFUND_ROUTING_KEY);
    }

    /**
     * 死信队列
     * 
     * <p>
     * 用于存储失败的退款消息，支持重试
     */
    @Bean
    public Queue refundDlq() {
        Map<String, Object> args = new HashMap<>();
        // 设置消息TTL，5秒后重新投递到主队列
        args.put("x-message-ttl", RETRY_DELAY_MS);
        // 重新投递到主队列
        args.put("x-dead-letter-exchange", REFUND_EXCHANGE);
        args.put("x-dead-letter-routing-key", REFUND_ROUTING_KEY);

        return QueueBuilder.durable(REFUND_DLQ)
                .withArguments(args)
                .build();
    }

    /**
     * 死信交换机
     */
    @Bean
    public DirectExchange refundDlx() {
        return new DirectExchange(REFUND_DLX, true, false);
    }

    /**
     * 绑定死信队列到死信交换机
     */
    @Bean
    public Binding refundDlqBinding(@org.springframework.beans.factory.annotation.Qualifier("refundDlq") Queue refundDlq,
                                     @org.springframework.beans.factory.annotation.Qualifier("refundDlx") DirectExchange refundDlx) {
        return BindingBuilder.bind(refundDlq)
                .to(refundDlx)
                .with(REFUND_DLQ_ROUTING_KEY);
    }
}
