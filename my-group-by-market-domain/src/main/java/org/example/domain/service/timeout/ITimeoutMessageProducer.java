package org.example.domain.service.timeout;

import org.example.domain.model.trade.message.TradeOrderTimeoutMessage;

/**
 * 超时消息生产者接口
 *
 * <p>
 * 用于发送TradeOrder超时延迟消息
 *
 * <p>
 * 设计说明：
 * <ul>
 * <li>接口定义在domain层，实现在infrastructure层</li>
 * <li>遵循DDD分层架构，application层通过接口调用</li>
 * <li>默认超时时间30分钟（独立于Activity.validTime）</li>
 * </ul>
 *
 */
public interface ITimeoutMessageProducer {

    /**
     * 发送延迟消息（使用默认超时时间30分钟）
     *
     * @param message 超时消息
     */
    void sendDelayMessage(TradeOrderTimeoutMessage message);

    /**
     * 发送延迟消息（自定义超时时间）
     *
     * @param message      超时消息
     * @param delaySeconds 延迟时间（秒）
     */
    void sendDelayMessage(TradeOrderTimeoutMessage message, int delaySeconds);
}
