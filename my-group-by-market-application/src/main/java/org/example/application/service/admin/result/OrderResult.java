package org.example.application.service.admin.result;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 拼团订单结果对象
 *
 * <p>
 * Application 层用例输出对象
 *
 * @author 开发团队
 * @since 2026-01-11
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "拼团订单结果")
public class OrderResult {

    @Schema(description = "拼团订单ID")
    private String orderId;

    @Schema(description = "拼团队伍ID")
    private String teamId;

    @Schema(description = "活动ID")
    private String activityId;

    @Schema(description = "商品ID")
    private String skuId;

    @Schema(description = "团长用户ID")
    private String leaderUserId;

    @Schema(description = "目标人数")
    private Integer targetCount;

    @Schema(description = "完成人数")
    private Integer completeCount;

    @Schema(description = "锁单量")
    private Integer lockCount;

    @Schema(description = "订单状态")
    private String status;

    @Schema(description = "原始价格")
    private BigDecimal originalPrice;

    @Schema(description = "折扣价格")
    private BigDecimal deductionPrice;

    @Schema(description = "拼团开始时间")
    private LocalDateTime startTime;

    @Schema(description = "参团截止时间")
    private LocalDateTime deadlineTime;

    @Schema(description = "实际成团时间")
    private LocalDateTime completedTime;

    @Schema(description = "来源")
    private String source;

    @Schema(description = "渠道")
    private String channel;
}
