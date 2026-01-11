package org.example.interfaces.web.dto.customer;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * C端商品详情响应
 * 
 * @author 开发团队
 * @since 2026-01-11
 */
@Data
public class CustomerGoodsDetailResponse {

    // ========== 商品基本信息 ==========

    /** 商品ID */
    private String goodsId;

    /** 商品名称 */
    private String goodsName;

    /** 规格信息 */
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

    /** 详情图列表 */
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
