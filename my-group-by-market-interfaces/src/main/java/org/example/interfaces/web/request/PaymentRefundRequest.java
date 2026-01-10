package org.example.interfaces.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 退款请求
 *
 * <p>
 * Interfaces 层协议入参
 *
 * @author 开发团队
 * @since 2026-01-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "退款请求")
public class PaymentRefundRequest {

    @NotBlank(message = "商户订单号不能为空")
    @Schema(description = "商户订单号")
    private String outTradeNo;

    @NotNull(message = "退款金额不能为空")
    @Positive(message = "退款金额必须大于0")
    @Schema(description = "退款金额")
    private BigDecimal refundAmount;

    @Schema(description = "退款原因")
    private String refundReason;

    @Schema(description = "退款请求号")
    private String outRequestNo;
}
