package org.example.application.service.payment.result;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 支付查询结果对象
 *
 * <p>
 * Application 层用例输出对象
 *
 * @author 开发团队
 * @since 2026-01-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "支付查询结果")
public class PaymentQueryResultObj {

    @Schema(description = "是否成功")
    private boolean success;

    @Schema(description = "交易号")
    private String tradeNo;

    @Schema(description = "商户订单号")
    private String outTradeNo;

    @Schema(description = "交易状态")
    private String tradeStatus;

    @Schema(description = "订单金额")
    private BigDecimal totalAmount;

    @Schema(description = "买家实付金额")
    private BigDecimal buyerPayAmount;

    @Schema(description = "错误信息")
    private String errorMsg;
}
