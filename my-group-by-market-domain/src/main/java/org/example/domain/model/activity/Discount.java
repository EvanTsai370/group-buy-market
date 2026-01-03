package org.example.domain.model.activity;

import lombok.Data;
import org.example.domain.model.activity.valueobject.DiscountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Discount 折扣配置（值对象）
 * 包含折扣计算所需的所有配置信息
 */
@Data
public class Discount {

    /** 折扣ID */
    private String discountId;

    /** 折扣名称 */
    private String discountName;

    /** 折扣描述 */
    private String discountDesc;

    /** 折扣金额（预留字段，暂未使用） */
    private BigDecimal discountAmount;

    /** 折扣类型（base=基础折扣，tag=标签折扣） */
    private DiscountType discountType;

    /** 营销计划 */
    private String marketPlan;

    /** 营销表达式 */
    private String marketExpr;

    /** 标签ID */
    private String tagId;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}