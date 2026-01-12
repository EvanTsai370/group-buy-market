package org.example.interfaces.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 锁单请求对象
 *
 * <p>
 * 协议层入参，表示用户发起锁单的 HTTP 请求
 *
 * <p>
 * 设计说明：
 * <ul>
 * <li>这是 Interfaces 层的 Request，包含 HTTP 协议相关字段</li>
 * <li>与 Application 层的 LockOrderCmd 隔离，避免协议层污染业务层</li>
 * <li>通过 Assembler 转换为 LockOrderCmd</li>
 * <li>userId 字段已移除，从认证上下文 SecurityContextUtils 获取</li>
 * </ul>
 *
 * @author 开发团队
 * @since 2026-01-08
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "锁单请求")
public class LockOrderRequest {

    @Schema(description = "订单ID（加入已有拼团时传入，创建新拼团时为空）")
    private String orderId;

    @Schema(description = "活动ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String activityId;

    @Schema(description = "商品ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String goodsId;

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
