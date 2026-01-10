package org.example.domain.model.user.valueobject;

/**
 * 用户状态
 */
public enum UserStatus {

    ACTIVE("正常"),
    DISABLED("禁用"),
    LOCKED("锁定");

    private final String desc;

    UserStatus(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}
