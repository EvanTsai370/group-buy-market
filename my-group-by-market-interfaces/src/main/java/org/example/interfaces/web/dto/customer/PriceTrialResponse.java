package org.example.interfaces.web.dto.customer;

import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(description = "商品ID")
    private String skuId;

    @Schema(description = "原价")
    private BigDecimal originalPrice;

    @Schema(description = "折扣价（拼团价）")
    private BigDecimal discountPrice;

    @Schema(description = "折扣描述")
    private String discountDesc;

    @Schema(description = "活动ID")
    private String activityId;

    @Schema(description = "活动名称")
    private String activityName;

    @Schema(description = "是否命中活动")
    private Boolean hitActivity;
}
