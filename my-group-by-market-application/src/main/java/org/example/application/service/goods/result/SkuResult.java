package org.example.application.service.goods.result;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * SKU 结果对象
 *
 * <p>
 * Application 层用例输出对象
 *
 * @author 开发团队
 * @since 2026-01-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "SKU结果")
public class SkuResult {

    @Schema(description = "商品ID")
    private String skuId;

    @Schema(description = "SPU ID")
    private String spuId;

    @Schema(description = "商品名称")
    private String goodsName;

    @Schema(description = "规格信息")
    private String specInfo;

    @Schema(description = "原价")
    private BigDecimal originalPrice;

    @Schema(description = "库存")
    private int stock;

    @Schema(description = "冻结库存")
    private int frozenStock;

    @Schema(description = "可用库存")
    private int availableStock;

    @Schema(description = "SKU图片")
    private String skuImage;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
