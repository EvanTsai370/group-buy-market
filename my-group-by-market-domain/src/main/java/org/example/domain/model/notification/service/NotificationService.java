package org.example.domain.model.notification.service;

import org.example.domain.model.notification.NotificationTask;

/**
 * 通知服务接口
 *
 * <p>职责：
 * <ul>
 *   <li>执行通知任务</li>
 *   <li>更新任务状态</li>
 *   <li>处理重试逻辑</li>
 * </ul>
 *
 * <p>架构说明：
 * <ul>
 *   <li>Domain层定义接口（框架无关）</li>
 *   <li>Infrastructure层提供实现（使用HTTP、MQ等技术）</li>
 * </ul>
 *
 */
public interface NotificationService {

    /**
     * 执行通知任务
     *
     * @param task 通知任务
     */
    void execute(NotificationTask task);
}
