package org.example.interfaces.web.dto.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 退款查询响应
 *
 * <p>
 * Interfaces 层协议出参
 *
 * @author 开发团队
 * @since 2026-01-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "退款查询响应")
public class RefundQueryResponse {

    @Schema(description = "是否成功")
    private boolean success;

    @Schema(description = "交易号")
    private String tradeNo;

    @Schema(description = "商户订单号")
    private String outTradeNo;

    @Schema(description = "退款请求号")
    private String outRequestNo;

    @Schema(description = "退款金额")
    private BigDecimal refundAmount;

    @Schema(description = "退款状态")
    private String refundStatus;

    @Schema(description = "错误信息")
    private String errorMsg;
}
