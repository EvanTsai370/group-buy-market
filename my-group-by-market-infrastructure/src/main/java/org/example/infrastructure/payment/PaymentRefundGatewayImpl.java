package org.example.infrastructure.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.gateway.IPaymentRefundGateway;
import org.example.domain.gateway.PaymentGateway;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 支付退款网关实现
 * 
 * <p>
 * 实现 Domain 层的 IPaymentRefundGateway 接口
 * 委托给 PaymentGateway 执行实际退款
 * </p>
 * 
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentRefundGatewayImpl implements IPaymentRefundGateway {

    private final PaymentGateway paymentGateway;

    @Override
    public RefundResult refund(String outTradeNo, BigDecimal refundAmount,
            String refundReason, String outRequestNo) {
        log.info("【PaymentRefundGateway】发起退款, outTradeNo: {}, amount: {}", outTradeNo, refundAmount);

        try {
            PaymentGateway.RefundResult gatewayResult = paymentGateway.refund(
                    outTradeNo, null, refundAmount, refundReason, outRequestNo);

            if (gatewayResult.success()) {
                log.info("【PaymentRefundGateway】退款成功, tradeNo: {}", gatewayResult.tradeNo());
                return RefundResult.success(gatewayResult.tradeNo());
            } else {
                log.warn("【PaymentRefundGateway】退款失败, errorMsg: {}", gatewayResult.errorMsg());
                return RefundResult.fail("REFUND_FAILED", gatewayResult.errorMsg());
            }
        } catch (Exception e) {
            log.error("【PaymentRefundGateway】退款异常, outTradeNo: {}", outTradeNo, e);
            return RefundResult.fail("SYSTEM_ERROR", e.getMessage());
        }
    }
}
