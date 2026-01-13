package org.example.interfaces.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

}
