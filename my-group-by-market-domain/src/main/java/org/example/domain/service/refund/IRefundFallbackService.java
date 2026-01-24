package org.example.domain.service.refund;

import org.example.domain.model.trade.message.RefundMessage;

/**
 * 退款降级服务接口
 *
 * <p>
 * 定义退款降级策略的统一接口
 *
 * <p>
 * 实现类：RefundProducer（Infrastructure层）
 *
 * <p>
 * 职责：当同步退款失败时，将退款请求发送到MQ进行异步重试
 *
 */
public interface IRefundFallbackService {

    /**
     * 发送退款消息到MQ
     *
     * @param message 退款消息
     */
    void sendToFallbackQueue(RefundMessage message);
}
