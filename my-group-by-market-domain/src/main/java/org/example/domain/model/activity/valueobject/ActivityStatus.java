package org.example.domain.model.activity.valueobject;

import lombok.Getter;

/**
 * 活动状态（值对象）
 */
@Getter
public enum ActivityStatus {

    /** 草稿 */
    DRAFT("草稿"),

    /** 生效中 */
    ACTIVE("生效中"),

    /** 已关闭 */
    CLOSED("已关闭");

    private final String desc;

    ActivityStatus(String desc) {
        this.desc = desc;
    }

}