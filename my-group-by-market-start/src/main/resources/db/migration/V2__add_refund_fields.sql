-- 添加退款相关字段到 trade_order 表
ALTER TABLE trade_order
ADD COLUMN refund_reason VARCHAR(500) COMMENT '退款原因',
ADD COLUMN refund_time DATETIME COMMENT '退款时间';

-- 为退款时间添加索引（用于退款统计查询）
CREATE INDEX idx_refund_time ON trade_order(refund_time);
