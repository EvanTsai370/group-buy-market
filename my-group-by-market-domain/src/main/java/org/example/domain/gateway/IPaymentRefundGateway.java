package org.example.domain.gateway;

import java.math.BigDecimal;

/**
 * 支付退款网关接口
 * 
 * <p>
 * 定义在 Domain 层，由 Infrastructure 层实现
 * 用于调用外部支付系统的退款服务
 * </p>
 * 
 */
public interface IPaymentRefundGateway {

    /**
     * 发起退款
     * 
     * @param outTradeNo   外部交易单号
     * @param refundAmount 退款金额
     * @param refundReason 退款原因
     * @param outRequestNo 退款请求号（幂等）
     * @return 退款结果
     */
    RefundResult refund(String outTradeNo, BigDecimal refundAmount,
            String refundReason, String outRequestNo);

    /**
     * 退款结果
     */
    record RefundResult(
            boolean success,
            String refundId,
            String errorCode,
            String errorMsg) {

        public static RefundResult success(String refundId) {
            return new RefundResult(true, refundId, null, null);
        }

        public static RefundResult fail(String errorCode, String errorMsg) {
            return new RefundResult(false, null, errorCode, errorMsg);
        }
    }
}
