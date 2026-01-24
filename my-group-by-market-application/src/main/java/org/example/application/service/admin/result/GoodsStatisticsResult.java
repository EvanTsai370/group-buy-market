package org.example.application.service.admin.result;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 商品统计结果对象
 *
 * <p>
 * Application 层用例输出对象
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "商品统计")
public class GoodsStatisticsResult {

    @Schema(description = "SPU总数")
    private int totalSpus;

    @Schema(description = "在售SPU数")
    private int onSaleSpus;

    @Schema(description = "下架SPU数")
    private int offSaleSpus;

    @Schema(description = "SKU总数")
    private int totalSkus;

    @Schema(description = "总库存")
    private int totalStock;

    @Schema(description = "冻结库存")
    private int frozenStock;

    @Schema(description = "低库存SKU列表")
    private List<SkuStatisticsInfo> lowStockSkus;
}
