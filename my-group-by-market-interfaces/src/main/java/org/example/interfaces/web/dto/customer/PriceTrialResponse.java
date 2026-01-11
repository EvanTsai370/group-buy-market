package org.example.interfaces.web.dto.customer;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 价格试算响应
 * 
 * @author 开发团队
 * @since 2026-01-11
 */
@Data
public class PriceTrialResponse {

    /** 商品ID */
    private String goodsId;

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
}
