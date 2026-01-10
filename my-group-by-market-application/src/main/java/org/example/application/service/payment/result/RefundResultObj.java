package org.example.application.service.payment.result;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 退款结果对象
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
@Schema(description = "退款结果")
public class RefundResultObj {

    @Schema(description = "是否成功")
    private boolean success;

    @Schema(description = "交易号")
    private String tradeNo;

    @Schema(description = "商户订单号")
    private String outTradeNo;

    @Schema(description = "退款金额")
    private BigDecimal refundFee;

    @Schema(description = "是否发生资金变动")
    private boolean fundChange;

    @Schema(description = "错误信息")
    private String errorMsg;
}
