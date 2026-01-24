package org.example.domain.model.notification.repository;

import org.example.domain.model.notification.NotificationTask;

import java.util.List;
import java.util.Optional;

/**
 * 通知任务仓储接口
 *
 * <p>职责：
 * <ul>
 *   <li>持久化通知任务</li>
 *   <li>查询待处理的任务</li>
 *   <li>更新任务状态</li>
 * </ul>
 *
 */
public interface NotificationTaskRepository {

    /**
     * 保存通知任务
     *
     * @param task 通知任务
     */
    void save(NotificationTask task);

    /**
     * 更新通知任务
     *
     * @param task 通知任务
     */
    void update(NotificationTask task);

    /**
     * 根据任务ID查询
     *
     * @param taskId 任务ID
     * @return 通知任务
     */
    Optional<NotificationTask> findByTaskId(String taskId);

    /**
     * 根据交易订单ID查询
     *
     * @param tradeOrderId 交易订单ID
     * @return 通知任务列表
     */
    List<NotificationTask> findByTradeOrderId(String tradeOrderId);

    /**
     * 查询待处理的任务（状态为PENDING）
     *
     * @param limit 限制数量
     * @return 待处理任务列表
     */
    List<NotificationTask> findPendingTasks(int limit);

    /**
     * 批量查询待处理任务（分页）
     *
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 待处理任务列表
     */
    List<NotificationTask> findPendingTasks(int offset, int limit);
}
