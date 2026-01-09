package org.example.domain.service.notification;

import org.example.domain.model.trade.message.RefundNotificationMessage;

/**
 * 通知策略接口
 *
 * <p>
 * 定义通知发送的统一接口，支持多种通知渠道
 *
 * <p>
 * 实现类：
 * <ul>
 * <li>SmsNotificationStrategy - 短信通知</li>
 * <li>EmailNotificationStrategy - 邮件通知</li>
 * <li>PushNotificationStrategy - 推送通知</li>
 * </ul>
 *
 * @author 开发团队
 * @since 2026-01-09
 */
public interface NotificationStrategy {

    /**
     * 发送通知
     *
     * @param message 通知消息
     */
    void send(RefundNotificationMessage message);

    /**
     * 获取通知类型
     *
     * @return 通知类型（SMS/EMAIL/PUSH）
     */
    String getType();

    /**
     * 是否支持该通知类型
     *
     * @param type 通知类型
     * @return true=支持, false=不支持
     */
    default boolean supports(String type) {
        return getType().equalsIgnoreCase(type);
    }
}
