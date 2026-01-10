package org.example.infrastructure.payment;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayConfig;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.*;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.*;
import com.alipay.api.response.*;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.gateway.PaymentGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 支付宝网关实现（沙箱环境）
 * 
 * <p>
 * 沙箱环境网关: https://openapi-sandbox.dl.alipaydev.com/gateway.do
 * 仅支持余额支付，不支持银行卡、花呗等
 * </p>
 * 
 * @author 开发团队
 * @since 2026-01-10
 */
@Slf4j
@Service
public class AlipayGateway implements PaymentGateway {

    @Value("${alipay.app-id:}")
    private String appId;

    @Value("${alipay.private-key:}")
    private String privateKey;

    @Value("${alipay.alipay-public-key:}")
    private String alipayPublicKey;

    @Value("${alipay.gateway-url:https://openapi-sandbox.dl.alipaydev.com/gateway.do}")
    private String gatewayUrl;

    @Value("${alipay.sign-type:RSA2}")
    private String signType;

    @Value("${alipay.charset:UTF-8}")
    private String charset;

    private AlipayClient alipayClient;

    @PostConstruct
    public void init() {
        try {
            AlipayConfig alipayConfig = new AlipayConfig();
            alipayConfig.setServerUrl(gatewayUrl);
            alipayConfig.setAppId(appId);
            alipayConfig.setPrivateKey(privateKey);
            alipayConfig.setFormat("json");
            alipayConfig.setAlipayPublicKey(alipayPublicKey);
            alipayConfig.setCharset(charset);
            alipayConfig.setSignType(signType);

            this.alipayClient = new DefaultAlipayClient(alipayConfig);
            log.info("【AlipayGateway】支付宝客户端初始化完成, gateway: {}", gatewayUrl);
        } catch (AlipayApiException e) {
            log.error("【AlipayGateway】支付宝客户端初始化失败", e);
        }
    }

    @Override
    public String createPaymentPage(String outTradeNo, BigDecimal amount,
            String subject, String returnUrl, String notifyUrl) {
        log.info("【AlipayGateway】创建支付页面, outTradeNo: {}, amount: {}", outTradeNo, amount);

        try {
            AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
            request.setReturnUrl(returnUrl);
            request.setNotifyUrl(notifyUrl);

            AlipayTradePagePayModel model = new AlipayTradePagePayModel();
            model.setOutTradeNo(outTradeNo);
            model.setTotalAmount(amount.setScale(2).toString());
            model.setSubject(subject);
            model.setProductCode("FAST_INSTANT_TRADE_PAY");

            request.setBizModel(model);

            // 使用 POST 方式生成表单
            AlipayTradePagePayResponse response = alipayClient.pageExecute(request, "POST");
            String pageForm = response.getBody();

            log.info("【AlipayGateway】支付页面创建成功, outTradeNo: {}", outTradeNo);
            return pageForm;

        } catch (AlipayApiException e) {
            log.error("【AlipayGateway】创建支付页面失败, outTradeNo: {}", outTradeNo, e);
            throw new RuntimeException("创建支付页面失败: " + e.getErrMsg());
        }
    }

    @Override
    public PaymentQueryResult queryPayment(String outTradeNo, String tradeNo) {
        log.info("【AlipayGateway】查询支付, outTradeNo: {}, tradeNo: {}", outTradeNo, tradeNo);

        try {
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            AlipayTradeQueryModel model = new AlipayTradeQueryModel();

            if (tradeNo != null && !tradeNo.isEmpty()) {
                model.setTradeNo(tradeNo);
            }
            if (outTradeNo != null && !outTradeNo.isEmpty()) {
                model.setOutTradeNo(outTradeNo);
            }

            request.setBizModel(model);
            AlipayTradeQueryResponse response = alipayClient.execute(request);

            if (response.isSuccess()) {
                log.info("【AlipayGateway】查询支付成功, tradeStatus: {}", response.getTradeStatus());
                return new PaymentQueryResult(
                        true,
                        response.getTradeNo(),
                        response.getOutTradeNo(),
                        response.getTradeStatus(),
                        response.getTotalAmount() != null ? new BigDecimal(response.getTotalAmount()) : null,
                        response.getBuyerPayAmount() != null ? new BigDecimal(response.getBuyerPayAmount()) : null,
                        null);
            } else {
                log.warn("【AlipayGateway】查询支付失败, subCode: {}, subMsg: {}",
                        response.getSubCode(), response.getSubMsg());
                return new PaymentQueryResult(
                        false, null, outTradeNo, null, null, null,
                        response.getSubMsg());
            }

        } catch (AlipayApiException e) {
            log.error("【AlipayGateway】查询支付异常, outTradeNo: {}", outTradeNo, e);
            return new PaymentQueryResult(false, null, outTradeNo, null, null, null, e.getErrMsg());
        }
    }

    @Override
    public RefundResult refund(String outTradeNo, String tradeNo,
            BigDecimal refundAmount, String refundReason, String outRequestNo) {
        log.info("【AlipayGateway】发起退款, outTradeNo: {}, refundAmount: {}", outTradeNo, refundAmount);

        try {
            AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
            AlipayTradeRefundModel model = new AlipayTradeRefundModel();

            if (outTradeNo != null) {
                model.setOutTradeNo(outTradeNo);
            }
            if (tradeNo != null) {
                model.setTradeNo(tradeNo);
            }
            model.setRefundAmount(refundAmount.setScale(2).toString());
            model.setRefundReason(refundReason);
            model.setOutRequestNo(outRequestNo);

            request.setBizModel(model);
            AlipayTradeRefundResponse response = alipayClient.execute(request);

            if (response.isSuccess()) {
                boolean fundChange = "Y".equals(response.getFundChange());
                log.info("【AlipayGateway】退款请求成功, fundChange: {}", fundChange);
                return new RefundResult(
                        true,
                        response.getTradeNo(),
                        response.getOutTradeNo(),
                        response.getRefundFee() != null ? new BigDecimal(response.getRefundFee()) : null,
                        fundChange,
                        null);
            } else {
                log.warn("【AlipayGateway】退款请求失败, subCode: {}, subMsg: {}",
                        response.getSubCode(), response.getSubMsg());
                return new RefundResult(
                        false, null, outTradeNo, null, false,
                        response.getSubMsg());
            }

        } catch (AlipayApiException e) {
            log.error("【AlipayGateway】退款异常, outTradeNo: {}", outTradeNo, e);
            return new RefundResult(false, null, outTradeNo, null, false, e.getErrMsg());
        }
    }

    @Override
    public RefundQueryResult queryRefund(String outTradeNo, String outRequestNo) {
        log.info("【AlipayGateway】查询退款, outTradeNo: {}, outRequestNo: {}", outTradeNo, outRequestNo);

        try {
            AlipayTradeFastpayRefundQueryRequest request = new AlipayTradeFastpayRefundQueryRequest();
            AlipayTradeFastpayRefundQueryModel model = new AlipayTradeFastpayRefundQueryModel();

            model.setOutTradeNo(outTradeNo);
            model.setOutRequestNo(outRequestNo);

            request.setBizModel(model);
            AlipayTradeFastpayRefundQueryResponse response = alipayClient.execute(request);

            if (response.isSuccess()) {
                log.info("【AlipayGateway】查询退款成功, refundStatus: {}", response.getRefundStatus());
                return new RefundQueryResult(
                        true,
                        response.getTradeNo(),
                        response.getOutTradeNo(),
                        response.getOutRequestNo(),
                        response.getRefundAmount() != null ? new BigDecimal(response.getRefundAmount()) : null,
                        response.getRefundStatus(),
                        null);
            } else {
                log.warn("【AlipayGateway】查询退款失败, subCode: {}, subMsg: {}",
                        response.getSubCode(), response.getSubMsg());
                return new RefundQueryResult(
                        false, null, outTradeNo, outRequestNo, null, null,
                        response.getSubMsg());
            }

        } catch (AlipayApiException e) {
            log.error("【AlipayGateway】查询退款异常, outTradeNo: {}", outTradeNo, e);
            return new RefundQueryResult(false, null, outTradeNo, outRequestNo, null, null, e.getErrMsg());
        }
    }

    @Override
    public boolean closeOrder(String outTradeNo, String tradeNo) {
        log.info("【AlipayGateway】关闭订单, outTradeNo: {}", outTradeNo);

        try {
            AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();
            AlipayTradeCloseModel model = new AlipayTradeCloseModel();

            if (outTradeNo != null) {
                model.setOutTradeNo(outTradeNo);
            }
            if (tradeNo != null) {
                model.setTradeNo(tradeNo);
            }

            request.setBizModel(model);
            AlipayTradeCloseResponse response = alipayClient.execute(request);

            if (response.isSuccess()) {
                log.info("【AlipayGateway】关闭订单成功, outTradeNo: {}", outTradeNo);
                return true;
            } else {
                log.warn("【AlipayGateway】关闭订单失败, subCode: {}, subMsg: {}",
                        response.getSubCode(), response.getSubMsg());
                return false;
            }

        } catch (AlipayApiException e) {
            log.error("【AlipayGateway】关闭订单异常, outTradeNo: {}", outTradeNo, e);
            return false;
        }
    }

    @Override
    public boolean verifyCallback(Map<String, String> params) {
        log.info("【AlipayGateway】验证回调签名");

        try {
            boolean signVerified = AlipaySignature.rsaCheckV1(
                    params, alipayPublicKey, charset, signType);

            log.info("【AlipayGateway】签名验证结果: {}", signVerified);
            return signVerified;

        } catch (AlipayApiException e) {
            log.error("【AlipayGateway】签名验证异常", e);
            return false;
        }
    }
}
