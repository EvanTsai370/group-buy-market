package org.example.domain.model.trade.valueobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通知配置值对象
 *
 * <p>封装回调通知的配置信息，支持HTTP和MQ两种通知方式
 *
 * @author 开发团队
 * @since 2026-01-04
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotifyConfig {

    /** 通知类型（HTTP/MQ） */
    private NotifyType notifyType;

    /** HTTP回调地址（notifyType=HTTP时必填） */
    private String notifyUrl;

    /** MQ主题（notifyType=MQ时必填） */
    private String notifyMq;

    /** 通知状态 */
    @Builder.Default
    private NotifyStatus notifyStatus = NotifyStatus.INIT;

    /**
     * 校验通知配置的完整性
     *
     * @return true=配置有效, false=配置无效
     */
    public boolean isValid() {
        if (notifyType == null) {
            return true; // 允许不配置通知
        }

        switch (notifyType) {
            case HTTP:
                return notifyUrl != null && !notifyUrl.trim().isEmpty();
            case MQ:
                return notifyMq != null && !notifyMq.trim().isEmpty();
            default:
                return false;
        }
    }

    /**
     * 标记通知成功
     */
    public void markSuccess() {
        this.notifyStatus = NotifyStatus.SUCCESS;
    }

    /**
     * 标记通知失败
     */
    public void markFailed() {
        this.notifyStatus = NotifyStatus.FAILED;
    }

    /**
     * 判断是否需要通知
     *
     * @return true=需要通知, false=不需要通知
     */
    public boolean needNotify() {
        return notifyType != null && isValid();
    }

    /**
     * 获取通知目标（URL或MQ主题）
     *
     * @return 通知目标字符串
     */
    public String getNotifyTarget() {
        if (notifyType == null) {
            return null;
        }

        switch (notifyType) {
            case HTTP:
                return notifyUrl;
            case MQ:
                return notifyMq;
            default:
                return null;
        }
    }
}
