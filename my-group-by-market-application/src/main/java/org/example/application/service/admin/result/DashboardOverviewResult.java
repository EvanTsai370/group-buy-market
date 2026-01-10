package org.example.application.service.admin.result;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 仪表盘概览结果对象
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
@Schema(description = "仪表盘概览")
public class DashboardOverviewResult {

    @Schema(description = "用户总数")
    private long totalUsers;

    @Schema(description = "SPU总数")
    private int totalSpus;

    @Schema(description = "在售SPU数")
    private int onSaleSpus;

    @Schema(description = "SKU总数")
    private int totalSkus;

    @Schema(description = "在售SKU数")
    private int onSaleSkus;

    @Schema(description = "低库存SKU数")
    private long lowStockCount;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
