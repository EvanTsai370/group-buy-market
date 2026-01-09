package org.example.infrastructure.notification;

import lombok.extern.slf4j.Slf4j;
import org.example.domain.model.trade.message.RefundNotificationMessage;
import org.example.domain.service.notification.NotificationStrategy;
import org.springframework.stereotype.Component;

/**
 * 短信通知策略
 *
 * <p>
 * 职责：通过短信渠道发送退款通知
 *
 * <p>
 * TODO: 集成第三方短信服务商（如阿里云、腾讯云）
 *
 * @author 开发团队
 * @since 2026-01-09
 */
@Slf4j
@Component
public class SmsNotificationStrategy implements NotificationStrategy {

    @Override
    public void send(RefundNotificationMessage message) {
        log.info("【短信通知】发送退款通知, userId: {}, tradeOrderId: {}, status: {}",
                message.getUserId(), message.getTradeOrderId(), message.getStatus());

        // TODO: 调用第三方短信API
        // 示例：
        // String content = String.format(
        // "您的订单%s退款%s，金额%.2f元。原因：%s",
        // message.getTradeOrderId(),
        // "SUCCESS".equals(message.getStatus()) ? "成功" : "失败",
        // message.getRefundAmount() / 100.0,
        // message.getReason()
        // );
        // smsClient.send(message.getUserId(), content);

        log.info("【短信通知】发送成功（模拟）");
    }

    @Override
    public String getType() {
        return "SMS";
    }
}
