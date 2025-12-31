// ============ 文件: domain/model/tag/valueobject/TagStatus.java ============
package org.example.domain.model.tag.valueobject;

/**
 * 标签状态（值对象）
 */
public enum TagStatus {

    /** 草稿 */
    DRAFT("草稿"),

    /** 计算中 */
    CALCULATING("计算中"),

    /** 已完成 */
    COMPLETED("已完成"),

    /** 计算失败 */
    FAILED("计算失败");

    private final String desc;

    TagStatus(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}