package org.example.domain.gateway;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 支付网关接口
 * 
 * <p>
 * 定义在 Domain 层，由 Infrastructure 层实现
 * 支持多种支付渠道（支付宝、微信等）
 * </p>
 * 
 */
public interface PaymentGateway {

    /**
     * 创建支付页面（电脑网站支付）
     * 
     * @param outTradeNo 商户订单号
     * @param amount     金额（元）
     * @param subject    订单标题
     * @param returnUrl  同步跳转地址
     * @param notifyUrl  异步通知地址
     * @return 支付表单HTML或跳转URL
     */
    String createPaymentPage(String outTradeNo, BigDecimal amount,
            String subject, String returnUrl, String notifyUrl);

    /**
     * 查询支付状态
     * 
     * @param outTradeNo 商户订单号
     * @param tradeNo    交易号（可选）
     * @return 支付结果
     */
    PaymentQueryResult queryPayment(String outTradeNo, String tradeNo);

    /**
     * 退款
     * 
     * @param outTradeNo   商户订单号
     * @param tradeNo      交易号（可选）
     * @param refundAmount 退款金额
     * @param refundReason 退款原因
     * @param outRequestNo 退款请求号
     * @return 退款结果
     */
    RefundResult refund(String outTradeNo, String tradeNo,
            BigDecimal refundAmount, String refundReason, String outRequestNo);

    /**
     * 查询退款
     */
    RefundQueryResult queryRefund(String outTradeNo, String outRequestNo);

    /**
     * 关闭订单
     */
    boolean closeOrder(String outTradeNo, String tradeNo);

    /**
     * 验证回调签名
     */
    boolean verifyCallback(Map<String, String> params);

    /**
     * 获取商户 AppId（用于回调校验）
     */
    String getAppId();

    /**
     * 获取商户 SellerId（用于回调校验）
     */
    String getSellerId();

    /**
     * 支付查询结果
     */
    record PaymentQueryResult(
            boolean success,
            String tradeNo,
            String outTradeNo,
            String tradeStatus,
            BigDecimal totalAmount,
            BigDecimal buyerPayAmount,
            String errorMsg) {
    }

    /**
     * 退款结果
     */
    record RefundResult(
            boolean success,
            String tradeNo,
            String outTradeNo,
            BigDecimal refundFee,
            boolean fundChange,
            String errorMsg) {
    }

    /**
     * 退款查询结果
     */
    record RefundQueryResult(
            boolean success,
            String tradeNo,
            String outTradeNo,
            String outRequestNo,
            BigDecimal refundAmount,
            String refundStatus,
            String errorMsg) {
    }
}
