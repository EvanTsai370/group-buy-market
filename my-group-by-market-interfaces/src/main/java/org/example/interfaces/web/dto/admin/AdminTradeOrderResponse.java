package org.example.interfaces.web.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易订单响应（管理后台）
 *
 * <p>
 * Interfaces 层协议出参
 *
 * @author 开发团队
 * @since 2026-01-11
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "交易订单响应")
public class AdminTradeOrderResponse {

    @Schema(description = "交易订单ID")
    private String tradeOrderId;

    @Schema(description = "拼团队伍ID")
    private String teamId;

    @Schema(description = "拼团订单ID")
    private String orderId;

    @Schema(description = "活动ID")
    private String activityId;

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "商品ID")
    private String skuId;

    @Schema(description = "商品名称")
    private String goodsName;

    @Schema(description = "原始价格")
    private BigDecimal originalPrice;

    @Schema(description = "减免金额")
    private BigDecimal deductionPrice;

    @Schema(description = "实付金额")
    private BigDecimal payPrice;

    @Schema(description = "交易状态")
    private String status;

    @Schema(description = "外部交易单号")
    private String outTradeNo;

    @Schema(description = "支付时间")
    private LocalDateTime payTime;

    @Schema(description = "结算时间")
    private LocalDateTime settlementTime;

    @Schema(description = "来源")
    private String source;

    @Schema(description = "渠道")
    private String channel;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "退款原因")
    private String refundReason;

    @Schema(description = "退款时间")
    private LocalDateTime refundTime;
}
