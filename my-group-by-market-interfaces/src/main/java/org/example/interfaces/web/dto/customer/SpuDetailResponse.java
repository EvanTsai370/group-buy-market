package org.example.interfaces.web.dto.customer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SPU详情响应
 *
 * @author 开发团队
 * @since 2026-01-12
 */
@Data
@Schema(description = "SPU详情响应")
public class SpuDetailResponse {

    @Schema(description = "SPU ID")
    private String spuId;

    @Schema(description = "SPU 名称")
    private String spuName;

    @Schema(description = "商品描述")
    private String description;

    @Schema(description = "主图")
    private String mainImage;

    @Schema(description = "详情图列表")
    private String detailImages;

    @Schema(description = "包含的 SKU 列表")
    private List<CustomerGoodsDetailResponse> skuList;

    // ========== 活动信息 ==========

    @Schema(description = "是否有拼团活动")
    private Boolean hasActivity;

    @Schema(description = "活动ID")
    private String activityId;

    @Schema(description = "活动名称")
    private String activityName;

    @Schema(description = "成团目标人数")
    private Integer targetCount;

    @Schema(description = "活动截止时间")
    private LocalDateTime activityEndTime;

    @Schema(description = "拼单有效时长（秒）")
    private Integer validTime;
}
