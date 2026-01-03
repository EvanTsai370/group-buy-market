package org.example.domain.model.activity.valueobject;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 折扣类型枚举
 */
@Getter
@AllArgsConstructor
public enum DiscountType {

    /**
     * 基础折扣（所有用户可享受）
     */
    BASE("base", "基础折扣"),

    /**
     * 标签折扣（仅限特定人群）
     */
    TAG("tag", "标签折扣");

    /**
     * 折扣类型编码
     */
    private final String code;

    /**
     * 折扣类型描述
     */
    private final String desc;

    /**
     * 根据编码获取枚举
     */
    public static DiscountType fromCode(String code) {
        for (DiscountType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
