package org.example.domain.model.activity.valueobject;

import lombok.Getter;

/**
 * 人群标签作用域（值对象）
 * 定义非目标人群的可见性和参与性规则
 */
@Getter
public enum TagScope {

    /** 严格模式：不在人群标签内不可见不可参与 */
    STRICT("严格模式"),

    /** 可见模式：不在人群标签内仅可见不可参与 */
    VISIBLE_ONLY("仅可见"),

    /** 开放模式：不在人群标签内可见可参与（预留，慎用） */
    OPEN("开放模式");

    private final String desc;

    TagScope(String desc) {
        this.desc = desc;
    }

}
