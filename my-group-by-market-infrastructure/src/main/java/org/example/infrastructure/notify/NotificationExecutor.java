package org.example.infrastructure.notify;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.model.notification.NotificationTask;
import org.example.domain.model.notification.repository.NotificationTaskRepository;
import org.example.domain.model.notification.service.NotificationService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 通知执行器
 *
 * <p>
 * 职责：
 * <ul>
 * <li>根据通知类型选择合适的策略</li>
 * <li>执行通知并更新任务状态</li>
 * <li>处理异常和重试逻辑</li>
 * </ul>
 *
 * @author 开发团队
 * @since 2026-01-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationExecutor implements NotificationService {

    private final List<CallbackNotificationStrategy> strategies;
    private final NotificationTaskRepository notificationTaskRepository;

    /**
     * 执行通知任务
     *
     * @param task 通知任务
     */
    @Override
    public void execute(NotificationTask task) {
        log.info("【通知执行器】开始执行通知, taskId={}, type={}",
                task.getTaskId(), task.getNotifyConfig().getNotifyType());

        try {
            // 标记为处理中
            task.markAsProcessing();
            notificationTaskRepository.update(task);

            // 查找支持的策略
            CallbackNotificationStrategy strategy = findStrategy(task);
            if (strategy == null) {
                throw new IllegalStateException("未找到支持的通知策略: " + task.getNotifyConfig().getNotifyType());
            }

            // 执行通知
            strategy.execute(task);

            // 标记为成功
            task.markAsSuccess();
            notificationTaskRepository.update(task);

            log.info("【通知执行器】通知执行成功, taskId={}", task.getTaskId());

        } catch (Exception e) {
            // 标记为失败并增加重试次数
            log.error("【通知执行器】通知执行失败, taskId={}, retryCount={}",
                    task.getTaskId(), task.getRetryCount(), e);

            task.markAsFailed(e.getMessage());
            notificationTaskRepository.update(task);

            // 如果还需要重试，记录日志
            if (task.needRetry()) {
                log.info("【通知执行器】任务将重试, taskId={}, retryCount={}/{}",
                        task.getTaskId(), task.getRetryCount(), task.getMaxRetryCount());
            } else {
                log.warn("【通知执行器】任务最终失败, taskId={}, maxRetry={}",
                        task.getTaskId(), task.getMaxRetryCount());
            }
        }
    }

    /**
     * 查找支持的策略
     *
     * @param task 通知任务
     * @return 通知策略
     */
    private CallbackNotificationStrategy findStrategy(NotificationTask task) {
        for (CallbackNotificationStrategy strategy : strategies) {
            if (strategy.supports(task)) {
                return strategy;
            }
        }
        return null;
    }
}
