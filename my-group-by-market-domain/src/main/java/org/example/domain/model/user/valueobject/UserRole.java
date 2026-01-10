package org.example.domain.model.user.valueobject;

/**
 * 用户角色
 */
public enum UserRole {

    USER("普通用户"),
    ADMIN("管理员");

    private final String desc;

    UserRole(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 是否是管理员
     */
    public boolean isAdmin() {
        return this == ADMIN;
    }
}
