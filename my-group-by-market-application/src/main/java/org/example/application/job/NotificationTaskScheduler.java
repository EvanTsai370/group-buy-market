package org.example.application.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.model.notification.NotificationTask;
import org.example.domain.model.notification.repository.NotificationTaskRepository;
import org.example.domain.model.notification.service.NotificationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 通知任务调度器
 *
 * <p>职责：
 * <ul>
 *   <li>定时扫描待处理的通知任务</li>
 *   <li>异步执行通知（使用线程池）</li>
 *   <li>处理失败重试逻辑</li>
 * </ul>
 *
 * <p>调度策略：
 * <ul>
 *   <li>每30秒执行一次</li>
 *   <li>每次最多处理100个任务</li>
 *   <li>使用固定线程池（5个线程）执行通知</li>
 * </ul>
 *
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationTaskScheduler {

    private final NotificationTaskRepository notificationTaskRepository;
    private final NotificationService notificationService;

    /**
     * 线程池：用于异步执行通知任务
     * 固定5个线程，避免大量通知阻塞主流程
     */
    private final ExecutorService executorService = Executors.newFixedThreadPool(5,
            r -> {
                Thread thread = new Thread(r);
                thread.setName("notification-task-" + System.currentTimeMillis());
                thread.setDaemon(true);
                return thread;
            });

    /**
     * 定时扫描并执行待处理的通知任务
     *
     * <p>执行周期：每30秒一次
     * <p>批量大小：每次最多100个任务
     */
    @Scheduled(fixedRate = 30000, initialDelay = 10000)
    public void schedulePendingTasks() {
        try {
            // 1. 查询待处理任务（最多100个）
            List<NotificationTask> pendingTasks = notificationTaskRepository.findPendingTasks(100);

            if (pendingTasks.isEmpty()) {
                return;
            }

            log.info("【通知调度器】扫描到待处理任务, count={}", pendingTasks.size());

            // 2. 异步执行通知任务
            for (NotificationTask task : pendingTasks) {
                executorService.submit(() -> {
                    try {
                        notificationService.execute(task);
                    } catch (Exception e) {
                        log.error("【通知调度器】任务执行异常, taskId={}", task.getTaskId(), e);
                    }
                });
            }

            log.info("【通知调度器】任务已提交到线程池, count={}", pendingTasks.size());

        } catch (Exception e) {
            log.error("【通知调度器】调度失败", e);
        }
    }

    /**
     * 应用关闭时，优雅关闭线程池
     */
    @PreDestroy
    public void shutdown() {
        log.info("【通知调度器】开始关闭线程池");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, java.util.concurrent.TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("【通知调度器】线程池已关闭");
    }
}
