package org.example.application.service.trade.cmd;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 锁单命令对象
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "锁单请求")
public class LockOrderCmd {

    @Schema(description = "订单ID（加入已有拼团时传入，创建新拼团时为空）")
    private String orderId;

    @Schema(description = "活动ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String activityId;

    /** 实际项目中userId应该从jwt/session中获取 */
    @Schema(description = "用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String userId;

    @Schema(description = "商品ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String skuId;

    @Schema(description = "外部交易单号（幂等性保证）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String outTradeNo;

    @Schema(description = "来源")
    private String source;

    @Schema(description = "渠道")
    private String channel;

    @Schema(description = "通知类型（HTTP/MQ）")
    private String notifyType;

    @Schema(description = "HTTP回调地址")
    private String notifyUrl;

    @Schema(description = "MQ主题")
    private String notifyMq;

    @Schema(description = "商品原价（从试算接口获取）", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal originalPrice;

    @Schema(description = "优惠金额（从试算接口获取）", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal deductionPrice;

    @Schema(description = "实付金额（从试算接口获取）", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal payPrice;
}
