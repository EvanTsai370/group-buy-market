package org.example.application.service.customer.result;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * SPU详情结果
 *
 */
@Data
public class SpuDetailResult {

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

    /** 包含的 SKU 列表 */
    private List<GoodsDetailResult> skuList;

    // ========== 活动信息（SPU维度） ==========

    /** 是否有拼团活动 */
    private Boolean hasActivity;

    /** 活动ID */
    private String activityId;

    /** 活动名称 */
    private String activityName;

    /** 成团目标人数 */
    private Integer targetCount;

    /** 活动截止时间 */
    private LocalDateTime activityEndTime;

    /** 拼单有效时长（秒） */
    private Integer validTime;
}
