package org.example.interfaces.web.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 仪表盘概览响应
 *
 * <p>
 * Interfaces 层协议出参
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "仪表盘概览响应")
public class DashboardOverviewResponse {

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

    @Schema(description = "今日订单数")
    private long todayOrders;

    @Schema(description = "今日GMV")
    private BigDecimal todayGMV;

    @Schema(description = "今日新增用户")
    private long todayUsers;

    @Schema(description = "进行中活动")
    private long activeActivities;

    @Schema(description = "最近订单")
    private List<AdminTradeOrderResponse> recentOrders;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
