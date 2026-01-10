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
 * 创建支付请求
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
@Schema(description = "创建支付请求")
public class CreatePaymentRequest {

    @NotBlank(message = "商户订单号不能为空")
    @Schema(description = "商户订单号")
    private String outTradeNo;

    @NotNull(message = "金额不能为空")
    @Positive(message = "金额必须大于0")
    @Schema(description = "金额")
    private BigDecimal amount;

    @NotBlank(message = "订单标题不能为空")
    @Schema(description = "订单标题")
    private String subject;
}
