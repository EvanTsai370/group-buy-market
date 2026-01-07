package org.example.domain.model.payment;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付回调记录聚合
 *
 * <p>
 * 职责：
 * <ul>
 * <li>记录每次支付回调的详细信息</li>
 * <li>用于幂等性保护，防止重复处理</li>
 * <li>提供审计追踪能力</li>
 * </ul>
 *
 * <p>
 * 设计说明：
 * <ul>
 * <li>callbackId 是支付系统提供的唯一标识，用于幂等性检查</li>
 * <li>记录完整的支付信息，便于问题排查</li>
 * </ul>
 *
 * @author 开发团队
 * @since 2026-01-07
 */
@Data
public class PaymentCallbackRecord {

    /**
     * 记录ID
     */
    private String recordId;

    /**
     * 支付系统的唯一回调ID（幂等性关键字段）
     */
    private String callbackId;

    /**
     * 交易订单ID
     */
    private String tradeOrderId;

    /**
     * 支付金额
     */
    private BigDecimal amount;

    /**
     * 支付时间
     */
    private LocalDateTime payTime;

    /**
     * 支付渠道
     */
    private String channel;

    /**
     * 支付流水号
     */
    private String paymentNo;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 工厂方法：创建支付回调记录
     *
     * @param recordId   记录ID
     * @param callbackId 回调ID
     * @param callback   回调数据
     * @return 支付回调记录
     */
    public static PaymentCallbackRecord create(String recordId, String callbackId,
            PaymentCallbackDTO callback) {
        PaymentCallbackRecord record = new PaymentCallbackRecord();
        record.recordId = recordId;
        record.callbackId = callbackId;
        record.tradeOrderId = callback.getTradeOrderId();
        record.amount = callback.getAmount();
        record.payTime = callback.getPayTime();
        record.channel = callback.getChannel();
        record.paymentNo = callback.getPaymentNo();
        record.createTime = LocalDateTime.now();
        return record;
    }
}
