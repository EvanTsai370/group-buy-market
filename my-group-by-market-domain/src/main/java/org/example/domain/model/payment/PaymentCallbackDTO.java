package org.example.domain.model.payment;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付回调数据传输对象
 *
 * <p>
 * 用途：
 * <ul>
 * <li>封装支付系统回调的核心数据</li>
 * <li>用于签名验证和金额校验</li>
 * </ul>
 *
 * @author 开发团队
 * @since 2026-01-07
 */
@Data
@Builder
public class PaymentCallbackDTO {

    /**
     * 交易订单ID
     */
    private String tradeOrderId;

    /**
     * 实际支付金额
     */
    private BigDecimal amount;

    /**
     * 支付时间
     */
    private LocalDateTime payTime;

    /**
     * 支付渠道（alipay/wechat/unionpay）
     */
    private String channel;

    /**
     * 支付流水号
     */
    private String paymentNo;
}
