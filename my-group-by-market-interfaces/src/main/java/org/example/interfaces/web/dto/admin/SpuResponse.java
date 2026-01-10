package org.example.interfaces.web.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SPU 响应
 *
 * <p>
 * Interfaces 层协议出参
 *
 * @author 开发团队
 * @since 2026-01-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "SPU响应")
public class SpuResponse {

    @Schema(description = "SPU ID")
    private String spuId;

    @Schema(description = "SPU名称")
    private String spuName;

    @Schema(description = "分类ID")
    private String categoryId;

    @Schema(description = "品牌")
    private String brand;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "主图")
    private String mainImage;

    @Schema(description = "详情图")
    private String detailImages;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "SKU列表")
    private List<SkuResponse> skuList;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
