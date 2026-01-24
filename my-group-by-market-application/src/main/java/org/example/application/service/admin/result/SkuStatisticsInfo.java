package org.example.application.service.admin.result;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * SKU 统计信息
 *
 * <p>
 * 用于替代直接返回 Domain 对象 Sku
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "SKU统计信息")
public class SkuStatisticsInfo {

    @Schema(description = "商品ID")
    private String skuId;

    @Schema(description = "商品名称")
    private String goodsName;

    @Schema(description = "总库存")
    private int stock;

    @Schema(description = "冻结库存")
    private int frozenStock;

    @Schema(description = "可用库存")
    private int availableStock;

    @Schema(description = "原价")
    private BigDecimal originalPrice;
}
