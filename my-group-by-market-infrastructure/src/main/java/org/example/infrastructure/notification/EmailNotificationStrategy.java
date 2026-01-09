package org.example.infrastructure.notification;

import lombok.extern.slf4j.Slf4j;
import org.example.domain.model.trade.message.RefundNotificationMessage;
import org.example.domain.service.notification.NotificationStrategy;
import org.springframework.stereotype.Component;

/**
 * 邮件通知策略
 *
 * <p>
 * 职责：通过邮件渠道发送退款通知
 *
 * <p>
 * TODO: 集成Spring Mail或第三方邮件服务
 *
 * @author 开发团队
 * @since 2026-01-09
 */
@Slf4j
@Component
public class EmailNotificationStrategy implements NotificationStrategy {

    @Override
    public void send(RefundNotificationMessage message) {
        log.info("【邮件通知】发送退款通知, userId: {}, tradeOrderId: {}, status: {}",
                message.getUserId(), message.getTradeOrderId(), message.getStatus());

        // TODO: 调用邮件发送服务
        // 示例：
        // String subject = "退款通知";
        // String content = String.format(
        // "尊敬的用户，您的订单%s退款%s。<br>金额：%.2f元<br>原因：%s",
        // message.getTradeOrderId(),
        // "SUCCESS".equals(message.getStatus()) ? "成功" : "失败",
        // message.getRefundAmount() / 100.0,
        // message.getReason()
        // );
        // mailService.send(message.getUserId(), subject, content);

        log.info("【邮件通知】发送成功（模拟）");
    }

    @Override
    public String getType() {
        return "EMAIL";
    }
}
