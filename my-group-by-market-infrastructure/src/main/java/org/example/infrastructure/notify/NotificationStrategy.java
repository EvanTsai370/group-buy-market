package org.example.infrastructure.notify;

import org.example.domain.model.notification.NotificationTask;

/**
 * 通知策略接口
 *
 * <p>使用策略模式处理不同类型的通知：
 * <ul>
 *   <li>HTTP - HTTP回调通知</li>
 *   <li>MQ - 消息队列通知</li>
 * </ul>
 *
 * @author 开发团队
 * @since 2026-01-06
 */
public interface NotificationStrategy {

    /**
     * 执行通知
     *
     * @param task 通知任务
     * @throws Exception 通知失败时抛出异常
     */
    void execute(NotificationTask task) throws Exception;

    /**
     * 判断是否支持此类型的通知
     *
     * @param task 通知任务
     * @return true表示支持
     */
    boolean supports(NotificationTask task);
}
