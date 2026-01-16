package org.example.domain.event;

import lombok.Value;
import java.time.LocalDateTime;

/**
 * 支付完成事件
 * 
 * <p>
 * 当TradeOrder支付成功（increment + markAsPaid完成）后发布
 * 
 * <p>
 * 使用场景：
 * <ul>
 * <li>异步触发settlement（确保所有并发事务已提交）</li>
 * <li>解耦支付成功和settlement逻辑</li>
 * <li>支持未来扩展（通知、积分等）</li>
 * </ul>
 */
@Value
public class PaymentCompletedEvent {
    /**
     * 交易订单ID
     */
    String tradeOrderId;

    /**
     * 拼团订单ID
     */
    String orderId;

    /**
     * 用户ID
     */
    String userId;

    /**
     * 新的完成人数（increment后）
     */
    int newCompleteCount;

    /**
     * 目标人数
     */
    int targetCount;

    /**
     * 事件发生时间
     */
    LocalDateTime eventTime;

    /**
     * 判断订单是否已达到目标人数
     * 
     * @return true=已达到目标人数, false=未达到
     */
    public boolean isOrderCompleted() {
        return newCompleteCount >= targetCount;
    }
}
