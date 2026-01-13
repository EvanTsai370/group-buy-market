package org.example.interfaces.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 退款请求
 *
 * <p>
 * 用于接收HTTP层的退款请求参数
 *
 * @author 开发团队
 * @since 2026-01-08
 */
@Data
@Schema(description = "退款申请")
public class RefundRequest {

    /**
     * 交易订单ID
     */
    @Schema(description = "交易订单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "交易订单ID不能为空")
    private String tradeOrderId;

    /**
     * 退款原因
     */
    @Schema(description = "退款原因", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 200)
    @NotBlank(message = "退款原因不能为空")
    @Size(max = 200, message = "退款原因不能超过200字")
    private String reason;
}
