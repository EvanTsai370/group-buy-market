package org.example.domain.model.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.example.domain.model.notification.valueobject.NotificationStatus;
import org.example.domain.model.trade.valueobject.NotifyConfig;

import java.time.LocalDateTime;

/**
 * 通知任务聚合根
 *
 * <p>职责：
 * <ul>
 *   <li>管理回调通知任务的生命周期</li>
 *   <li>记录重试次数和失败原因</li>
 *   <li>控制状态转换（PENDING → PROCESSING → SUCCESS/FAILED）</li>
 * </ul>
 *
 * @author 开发团队
 * @since 2026-01-06
 */
@Getter
@Builder
@AllArgsConstructor
public class NotificationTask {

    /**
     * 任务ID（主键）
     */
    private String taskId;

    /**
     * 关联的交易订单ID
     */
    private String tradeOrderId;

    /**
     * 通知配置（包含类型、URL、MQ等）
     */
    private NotifyConfig notifyConfig;

    /**
     * 任务状态
     */
    private NotificationStatus status;

    /**
     * 已重试次数
     */
    private Integer retryCount;

    /**
     * 最大重试次数（默认3次）
     */
    private Integer maxRetryCount;

    /**
     * 最后一次执行时间
     */
    private LocalDateTime lastExecuteTime;

    /**
     * 失败原因
     */
    private String failureReason;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 工厂方法：创建新的通知任务
     *
     * @param taskId 任务ID
     * @param tradeOrderId 交易订单ID
     * @param notifyConfig 通知配置
     * @return 通知任务实例
     */
    public static NotificationTask create(String taskId, String tradeOrderId, NotifyConfig notifyConfig) {
        LocalDateTime now = LocalDateTime.now();
        return NotificationTask.builder()
                .taskId(taskId)
                .tradeOrderId(tradeOrderId)
                .notifyConfig(notifyConfig)
                .status(NotificationStatus.PENDING)
                .retryCount(0)
                .maxRetryCount(3)
                .createTime(now)
                .updateTime(now)
                .build();
    }

    /**
     * 标记为处理中
     */
    public void markAsProcessing() {
        if (!this.status.canProcess()) {
            throw new IllegalStateException("任务状态不允许处理: " + this.status);
        }
        this.status = NotificationStatus.PROCESSING;
        this.lastExecuteTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 标记为成功
     */
    public void markAsSuccess() {
        this.status = NotificationStatus.SUCCESS;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 标记为失败并增加重试次数
     *
     * @param failureReason 失败原因
     */
    public void markAsFailed(String failureReason) {
        this.retryCount++;
        this.failureReason = failureReason;
        this.updateTime = LocalDateTime.now();

        // 超过最大重试次数，标记为最终失败
        if (this.retryCount >= this.maxRetryCount) {
            this.status = NotificationStatus.FAILED;
        } else {
            // 重置为PENDING，等待下次重试
            this.status = NotificationStatus.PENDING;
        }
    }

    /**
     * 是否需要重试
     *
     * @return true表示需要重试
     */
    public boolean needRetry() {
        return this.status == NotificationStatus.PENDING && this.retryCount < this.maxRetryCount;
    }

    /**
     * 是否已经完成（成功或最终失败）
     *
     * @return true表示已完成
     */
    public boolean isCompleted() {
        return this.status.isFinal();
    }
}
