package org.example.domain.model.trade.valueobject;

/**
 * 通知类型枚举
 *
 * <p>支持多种通知方式，灵活适配不同的业务场景
 *
 * @author 开发团队
 * @since 2026-01-04
 */
public enum NotifyType {

    /**
     * HTTP回调
     * <p>适用场景：对接外部商城系统，通过HTTP接口回调通知
     */
    HTTP("HTTP", "HTTP回调"),

    /**
     * 消息队列
     * <p>适用场景：内部系统解耦，通过MQ异步通知
     */
    MQ("MQ", "消息队列");

    private final String code;
    private final String desc;

    NotifyType(String code, String desc) {
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
     * @param code 类型码
     * @return 通知类型枚举
     */
    public static NotifyType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (NotifyType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的通知类型: " + code);
    }
}
