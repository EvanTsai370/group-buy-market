package org.example.application.service.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.exception.BizException;
import org.example.domain.gateway.PaymentGateway;
import org.example.domain.gateway.PaymentGateway.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 支付服务
 * 
 * @author 开发团队
 * @since 2026-01-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlipayPaymentService {

    private final PaymentGateway paymentGateway;

    @Value("${alipay.return-url:http://localhost:8080/api/payment/return}")
    private String returnUrl;

    @Value("${alipay.notify-url:http://localhost:8080/api/payment/callback/alipay}")
    private String notifyUrl;

    /**
     * 创建支付页面
     * 
     * @param outTradeNo 商户订单号
     * @param amount     金额
     * @param subject    订单标题
     * @return 支付表单HTML
     */
    public String createPaymentPage(String outTradeNo, BigDecimal amount, String subject) {
        log.info("【PaymentService】创建支付页面, outTradeNo: {}, amount: {}", outTradeNo, amount);

        if (outTradeNo == null || outTradeNo.isEmpty()) {
            throw new BizException("订单号不能为空");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("金额必须大于0");
        }

        return paymentGateway.createPaymentPage(outTradeNo, amount, subject, returnUrl, notifyUrl);
    }

    /**
     * 查询支付状态
     */
    public PaymentQueryResult queryPayment(String outTradeNo) {
        log.info("【PaymentService】查询支付状态, outTradeNo: {}", outTradeNo);
        return paymentGateway.queryPayment(outTradeNo, null);
    }

    /**
     * 退款
     */
    public RefundResult refund(String outTradeNo, BigDecimal refundAmount,
            String refundReason, String outRequestNo) {
        log.info("【PaymentService】发起退款, outTradeNo: {}, refundAmount: {}", outTradeNo, refundAmount);

        if (outRequestNo == null || outRequestNo.isEmpty()) {
            outRequestNo = outTradeNo + "-RF-" + System.currentTimeMillis();
        }

        return paymentGateway.refund(outTradeNo, null, refundAmount, refundReason, outRequestNo);
    }

    /**
     * 查询退款
     */
    public RefundQueryResult queryRefund(String outTradeNo, String outRequestNo) {
        log.info("【PaymentService】查询退款, outTradeNo: {}, outRequestNo: {}", outTradeNo, outRequestNo);
        return paymentGateway.queryRefund(outTradeNo, outRequestNo);
    }

    /**
     * 关闭订单
     */
    public boolean closeOrder(String outTradeNo) {
        log.info("【PaymentService】关闭订单, outTradeNo: {}", outTradeNo);
        return paymentGateway.closeOrder(outTradeNo, null);
    }

    /**
     * 验证回调签名
     */
    public boolean verifyCallback(Map<String, String> params) {
        return paymentGateway.verifyCallback(params);
    }

    /**
     * 处理支付回调
     * 
     * @return 是否处理成功
     */
    public boolean handlePaymentCallback(Map<String, String> params) {
        log.info("【PaymentService】处理支付回调");

        // 1. 验证签名
        if (!verifyCallback(params)) {
            log.error("【PaymentService】回调签名验证失败");
            return false;
        }

        // 2. 解析参数
        String tradeNo = params.get("trade_no");
        String outTradeNo = params.get("out_trade_no");
        String tradeStatus = params.get("trade_status");
        String totalAmount = params.get("total_amount");

        log.info("【PaymentService】回调参数: tradeNo={}, outTradeNo={}, tradeStatus={}, totalAmount={}",
                tradeNo, outTradeNo, tradeStatus, totalAmount);

        // 3. 处理交易状态
        if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
            // 支付成功，更新订单状态
            log.info("【PaymentService】支付成功, outTradeNo: {}", outTradeNo);
            // TODO: 调用订单服务更新状态
            return true;
        } else if ("TRADE_CLOSED".equals(tradeStatus)) {
            // 交易关闭
            log.info("【PaymentService】交易关闭, outTradeNo: {}", outTradeNo);
            return true;
        }

        log.warn("【PaymentService】未处理的交易状态: {}", tradeStatus);
        return true;
    }
}
