package org.example.domain.model.trade.valueobject;

/**
 * 通知状态枚举
 *
 * <p>用于追踪回调通知的执行状态
 *
 */
public enum NotifyStatus {

    /**
     * 初始状态（未发送）
     */
    INIT("INIT", "未发送"),

    /**
     * 通知成功
     */
    SUCCESS("SUCCESS", "成功"),

    /**
     * 通知失败
     */
    FAILED("FAILED", "失败");

    private final String code;
    private final String desc;

    NotifyStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 根据code获取枚举
     *
     * @param code 状态码
     * @return 通知状态枚举
     */
    public static NotifyStatus fromCode(String code) {
        if (code == null) {
            return INIT;
        }
        for (NotifyStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return INIT;
    }
}
