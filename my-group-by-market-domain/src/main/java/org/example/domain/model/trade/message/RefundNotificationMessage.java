package org.example.domain.model.trade.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 退款通知消息
 *
 * <p>
 * 用于RabbitMQ异步发送退款通知
 *
 * <p>
 * 支持的通知渠道：
 * <ul>
 * <li>短信（SMS）</li>
 * <li>邮件（Email）</li>
 * <li>推送（Push）</li>
 * </ul>
 *
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefundNotificationMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 交易订单ID */
    private String tradeOrderId;

    /** 用户ID */
    private String userId;

    /** 退款金额 */
    private Long refundAmount;

    /** 退款原因 */
    private String reason;

    /** 退款状态（SUCCESS/FAILED） */
    private String status;

    /** 通知类型（SMS/EMAIL/PUSH） */
    private String notificationType;

    /** 创建时间 */
    private Long createTime;

    public RefundNotificationMessage(String tradeOrderId, String userId, Long refundAmount,
            String reason, String status, String notificationType) {
        this.tradeOrderId = tradeOrderId;
        this.userId = userId;
        this.refundAmount = refundAmount;
        this.reason = reason;
        this.status = status;
        this.notificationType = notificationType;
        this.createTime = System.currentTimeMillis();
    }
}
