package org.example.domain.model.trade.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 退款消息
 *
 * <p>
 * 用于RabbitMQ异步退款处理和降级策略
 *
 * <p>
 * 使用场景：
 * <ul>
 * <li>退款失败时进入死信队列重试</li>
 * <li>最多重试3次</li>
 * <li>超过重试次数后人工介入</li>
 * </ul>
 *
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefundMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 交易订单ID */
    private String tradeOrderId;

    /** 退款原因 */
    private String reason;

    /** 操作人 */
    private String operator;

    /** 重试次数 */
    private Integer retryCount;

    /** 创建时间 */
    private Long createTime;

    public RefundMessage(String tradeOrderId, String reason, String operator) {
        this.tradeOrderId = tradeOrderId;
        this.reason = reason;
        this.operator = operator;
        this.retryCount = 0;
        this.createTime = System.currentTimeMillis();
    }

    /**
     * 增加重试次数
     */
    public void incrementRetryCount() {
        this.retryCount = (this.retryCount == null ? 0 : this.retryCount) + 1;
    }

    /**
     * 是否超过最大重试次数
     *
     * @param maxRetries 最大重试次数
     * @return true=超过, false=未超过
     */
    public boolean exceedsMaxRetries(int maxRetries) {
        return this.retryCount != null && this.retryCount >= maxRetries;
    }
}
