package org.example.application.service.payment;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.HmacUtils;
import org.example.domain.model.payment.PaymentCallbackDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 支付签名验证服务
 *
 * <p>
 * 职责：
 * <ul>
 * <li>验证支付回调的签名，防止伪造请求</li>
 * <li>支持多支付渠道（支付宝、微信、银联）</li>
 * <li>使用 HMAC-SHA256 算法</li>
 * </ul>
 *
 * <p>
 * 签名规则：
 * <ol>
 * <li>按字段名ASCII排序拼接：amount=100.00&channel=alipay&paymentNo=xxx&payTime=xxx&timestamp=xxx&tradeOrderId=xxx</li>
 * <li>使用渠道密钥计算 HMAC-SHA256</li>
 * <li>比对计算结果与传入的签名</li>
 * </ol>
 *
 * @author 开发团队
 * @since 2026-01-07
 */
@Slf4j
@Service
public class PaymentSignatureValidator {

    @Value("${payment.alipay.secret:default-alipay-secret}")
    private String alipaySecret;

    @Value("${payment.wechat.secret:default-wechat-secret}")
    private String wechatSecret;

    @Value("${payment.unionpay.secret:default-unionpay-secret}")
    private String unionpaySecret;

    /**
     * 验证签名
     *
     * @param callback  回调数据
     * @param sign      签名
     * @param timestamp 时间戳
     * @param channel   支付渠道
     * @return true=验证通过，false=验证失败
     */
    public boolean validate(PaymentCallbackDTO callback, String sign,
                            Long timestamp, String channel) {
        try {
            // 1. 获取密钥
            String secret = getSecretByChannel(channel);
            if (secret == null || secret.startsWith("default-")) {
                log.error("【签名验证】未配置支付渠道密钥, channel: {}", channel);
                return false;
            }

            // 2. 构建签名字符串（按字段名ASCII排序）
            String signStr = buildSignString(callback, timestamp);

            // 3. 计算 HMAC-SHA256
            String expectedSign = HmacUtils.hmacSha256Hex(secret, signStr);

            // 4. 比对签名
            boolean valid = expectedSign.equals(sign);

            if (!valid) {
                log.warn("【签名验证】签名不匹配, channel: {}, expected: {}, actual: {}",
                        channel, expectedSign, sign);
            }

            return valid;

        } catch (Exception e) {
            log.error("【签名验证】签名验证异常, channel: {}", channel, e);
            return false;
        }
    }

    /**
     * 根据渠道获取密钥
     */
    private String getSecretByChannel(String channel) {
        return switch (channel.toLowerCase()) {
            case "alipay" -> alipaySecret;
            case "wechat" -> wechatSecret;
            case "unionpay" -> unionpaySecret;
            default -> {
                log.warn("【签名验证】不支持的支付渠道, channel: {}", channel);
                yield null;
            }
        };
    }

    /**
     * 构建签名字符串
     * <p>
     * 格式：amount=100.00&channel=alipay&paymentNo=xxx&payTime=xxx&timestamp=xxx&tradeOrderId=xxx
     */
    private String buildSignString(PaymentCallbackDTO callback, Long timestamp) {
        // 按字段名ASCII排序拼接
        return "amount=" + callback.getAmount() +
                "&channel=" + callback.getChannel() +
                "&paymentNo=" + callback.getPaymentNo() +
                "&payTime=" + callback.getPayTime() +
                "&timestamp=" + timestamp +
                "&tradeOrderId=" + callback.getTradeOrderId();
    }
}
