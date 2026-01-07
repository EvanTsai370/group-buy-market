package org.example.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 通知任务持久化对象
 *
 * @author 开发团队
 * @since 2026-01-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("notification_task")
public class NotificationTaskPO {

    /**
     * 任务ID
     */
    @TableId(type = IdType.INPUT)
    private String taskId;

    /**
     * 关联的交易订单ID
     */
    private String tradeOrderId;

    /**
     * 通知类型（HTTP/MQ）
     */
    private String notifyType;

    /**
     * HTTP回调地址
     */
    private String notifyUrl;

    /**
     * MQ主题
     */
    private String notifyMq;

    /**
     * 任务状态（PENDING/PROCESSING/SUCCESS/FAILED）
     */
    private String status;

    /**
     * 已重试次数
     */
    private Integer retryCount;

    /**
     * 最大重试次数
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
}
