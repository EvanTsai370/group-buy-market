package org.example.interfaces.web.dto.customer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * SPU列表响应
 *
 */
@Data
@Schema(description = "SPU列表响应")
public class SpuListResponse {

    @Schema(description = "SPU ID")
    private String spuId;

    @Schema(description = "SPU 名称")
    private String spuName;

    @Schema(description = "主图")
    private String mainImage;

    @Schema(description = "最低原价（展示用）")
    private BigDecimal minOriginalPrice;

    @Schema(description = "最低拼团价（展示用）")
    private BigDecimal minGroupPrice;

    @Schema(description = "是否有拼团活动")
    private Boolean hasActivity;

    @Schema(description = "活动ID")
    private String activityId;
}
