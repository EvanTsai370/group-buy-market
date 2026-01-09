package org.example.domain.model.trade.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * TradeOrder超时消息
 *
 * <p>
 * 用于RabbitMQ延迟队列，处理用户锁单后超时未支付的场景
 *
 * @author 开发团队
 * @since 2026-01-08
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TradeOrderTimeoutMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 交易订单ID */
    private String tradeOrderId;

    /** 订单ID */
    private String orderId;

    /** 用户ID */
    private String userId;

    /** 活动ID */
    private String activityId;

    /** 创建时间戳（用于日志和监控） */
    private Long createTime;
}
