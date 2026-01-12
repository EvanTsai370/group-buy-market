package org.example.domain.model.activity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 活动商品关联（值对象）
 * 职责：表示活动与商品的关联关系，包含来源渠道信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityGoods {

    /** 活动ID */
    private String activityId;

    /** 商品SPU ID */
    private String spuId;

    /** 来源（如：s01-小程序、s02-App） */
    private String source;

    /** 渠道（如：c01-首页、c02-搜索） */
    private String channel;

    /** 折扣ID（可选，为空则使用活动默认折扣） */
    private String discountId;

    /**
     * 获取实际生效的折扣ID
     * 如果商品级别配置了折扣，则使用商品折扣；否则使用活动默认折扣
     *
     * @param activityDefaultDiscountId 活动默认折扣ID
     * @return 实际生效的折扣ID
     */
    public String getEffectiveDiscountId(String activityDefaultDiscountId) {
        return discountId != null ? discountId : activityDefaultDiscountId;
    }
}
