package org.example.application.service.customer.result;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 价格试算结果
 * 
 * @author 开发团队
 * @since 2026-01-11
 */
@Data
public class PriceTrialResult {

    /** 商品ID */
    private String skuId;

    /** 原价 */
    private BigDecimal originalPrice;

    /** 折扣价（拼团价） */
    private BigDecimal discountPrice;

    /** 折扣描述 */
    private String discountDesc;

    /** 活动ID */
    private String activityId;

    /** 活动名称 */
    private String activityName;

    /** 是否命中活动 */
    private Boolean hitActivity;

    /** 是否可参与拼团 (供前端置灰拼团按钮使用) */
    private Boolean canParticipate;

    /** 不可参与的原因 */
    private String reason;
}
