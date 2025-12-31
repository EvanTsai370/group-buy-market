// ============ 文件: domain/model/activity/Discount.java ============
package org.example.domain.model.activity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Discount 折扣配置（聚合根或值对象）
 * 这里简化为值对象，如果需要复杂的折扣计算逻辑，可以升级为聚合根
 */
@Data
public class Discount {

    /** 折扣ID */
    private String discountId;

    /** 折扣名称 */
    private String discountName;

    /** 折扣描述 */
    private String discountDesc;

    /** 折扣金额 */
    private BigDecimal discountAmount;

    /** 折扣类型 */
    private String discountType;

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