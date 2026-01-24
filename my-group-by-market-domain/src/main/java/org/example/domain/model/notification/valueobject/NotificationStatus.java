package org.example.domain.model.notification.valueobject;

import lombok.Getter;

/**
 * 通知任务状态枚举
 *
 * <p>状态流转：
 * PENDING（待处理） → PROCESSING（处理中） → SUCCESS（成功）
 *                                        → FAILED（失败，重试超过最大次数）
 *
 */
@Getter
public enum NotificationStatus {

    /**
     * 待处理 - 任务已创建，等待执行
     */
    PENDING("PENDING", "待处理"),

    /**
     * 处理中 - 正在执行回调
     */
    PROCESSING("PROCESSING", "处理中"),

    /**
     * 成功 - 回调成功完成
     */
    SUCCESS("SUCCESS", "成功"),

    /**
     * 失败 - 重试次数超过最大限制，不再重试
     */
    FAILED("FAILED", "失败");

    private final String code;
    private final String description;

    NotificationStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据code获取枚举
     *
     * @param code 状态码
     * @return 通知状态枚举
     */
    public static NotificationStatus fromCode(String code) {
        for (NotificationStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown notification status code: " + code);
    }

    /**
     * 是否为最终状态（不需要再处理）
     *
     * @return true表示最终状态
     */
    public boolean isFinal() {
        return this == SUCCESS || this == FAILED;
    }

    /**
     * 是否可以处理
     *
     * @return true表示可以处理
     */
    public boolean canProcess() {
        return this == PENDING;
    }
}
