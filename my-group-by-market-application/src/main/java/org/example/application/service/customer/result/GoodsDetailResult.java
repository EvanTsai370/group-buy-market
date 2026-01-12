package org.example.application.service.customer.result;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品详情结果
 * 
 * @author 开发团队
 * @since 2026-01-11
 */
@Data
public class GoodsDetailResult {

    // ========== 商品基本信息 ==========

    /** 商品ID */
    private String skuId;

    /** 商品名称 */
    private String goodsName;

    /** 规格信息（JSON） */
    private String specInfo;

    /** 原价 */
    private BigDecimal originalPrice;

    /** SKU图片 */
    private String skuImage;

    /** 可用库存 */
    private Integer availableStock;

    // ========== SPU 信息 ==========

    /** SPU ID */
    private String spuId;

    /** SPU 名称 */
    private String spuName;

    /** 商品描述 */
    private String description;

    /** 主图 */
    private String mainImage;

    /** 详情图列表（逗号分隔） */
    private String detailImages;

    // ========== 活动信息 ==========

    /** 是否有拼团活动 */
    private Boolean hasActivity;

    /** 活动ID */
    private String activityId;

    /** 活动名称 */
    private String activityName;

    /** 拼团价 */
    private BigDecimal groupPrice;

    /** 成团目标人数 */
    private Integer targetCount;

    /** 活动截止时间 */
    private LocalDateTime activityEndTime;

    /** 拼单有效时长（秒） */
    private Integer validTime;
}
