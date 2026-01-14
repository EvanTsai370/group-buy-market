-- ============================================
-- 通知任务表
-- 用途：管理支付成功后的异步回调通知
-- ============================================

CREATE TABLE notification_task (
    -- 主键
    task_id VARCHAR(50) PRIMARY KEY COMMENT '任务ID',

    -- 关联订单
    trade_order_id VARCHAR(50) NOT NULL COMMENT '关联的交易订单ID',

    -- 通知配置
    notify_type VARCHAR(10) NOT NULL COMMENT '通知类型：HTTP/MQ',
    notify_url VARCHAR(500) COMMENT 'HTTP回调地址',
    notify_mq VARCHAR(100) COMMENT 'MQ主题名称',

    -- 任务状态
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '任务状态：PENDING/PROCESSING/SUCCESS/FAILED',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '已重试次数',
    max_retry_count INT NOT NULL DEFAULT 3 COMMENT '最大重试次数',

    -- 执行记录
    last_execute_time DATETIME COMMENT '最后一次执行时间',
    failure_reason VARCHAR(1000) COMMENT '失败原因',

    -- 审计字段
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    -- 索引
    INDEX idx_trade_order_id (trade_order_id),
    INDEX idx_status_create_time (status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知任务表';

-- 添加退款相关字段到 trade_order 表
ALTER TABLE trade_order
    ADD COLUMN refund_reason VARCHAR(500) COMMENT '退款原因',
ADD COLUMN refund_time DATETIME COMMENT '退款时间';

-- 为退款时间添加索引（用于退款统计查询）
CREATE INDEX idx_refund_time ON trade_order(refund_time);

