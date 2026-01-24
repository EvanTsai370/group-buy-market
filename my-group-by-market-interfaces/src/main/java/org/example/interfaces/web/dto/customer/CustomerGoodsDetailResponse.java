package org.example.interfaces.web.dto.customer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * C端商品详情响应
 * 
 */
@Data
public class CustomerGoodsDetailResponse {

    // ========== 商品基本信息 ==========

    @Schema(description = "商品ID")
    private String skuId;

    @Schema(description = "商品名称")
    private String goodsName;

    @Schema(description = "规格信息")
    private String specInfo;

    @Schema(description = "原价")
    private BigDecimal originalPrice;

    @Schema(description = "SKU图片")
    private String skuImage;

    @Schema(description = "可用库存")
    private Integer availableStock;

    // ========== 活动信息 ==========

    @Schema(description = "是否有拼团活动")
    private Boolean hasActivity;

    @Schema(description = "活动ID")
    private String activityId;

    @Schema(description = "活动名称")
    private String activityName;

    @Schema(description = "拼团价")
    private BigDecimal groupPrice;

    @Schema(description = "成团目标人数")
    private Integer targetCount;

    @Schema(description = "活动截止时间")
    private LocalDateTime activityEndTime;

    @Schema(description = "拼单有效时长（秒）")
    private Integer validTime;
}
