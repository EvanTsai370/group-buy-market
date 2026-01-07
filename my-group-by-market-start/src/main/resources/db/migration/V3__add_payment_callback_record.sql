-- ============================================
-- V3: 添加支付回调记录表
-- 用途：支付回调幂等性保护和审计追踪
-- ============================================

CREATE TABLE payment_callback_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增ID',
    record_id VARCHAR(50) NOT NULL UNIQUE COMMENT '记录ID',
    callback_id VARCHAR(100) NOT NULL UNIQUE COMMENT '支付系统回调ID（幂等性关键字段）',
    trade_order_id VARCHAR(50) NOT NULL COMMENT '交易订单ID',

    -- 支付信息
    amount DECIMAL(10,2) NOT NULL COMMENT '支付金额',
    pay_time DATETIME NOT NULL COMMENT '支付时间',
    channel VARCHAR(50) NOT NULL COMMENT '支付渠道（alipay/wechat/unionpay）',
    payment_no VARCHAR(100) COMMENT '支付流水号',

    -- 审计字段
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_trade_order_id (trade_order_id),
    INDEX idx_callback_id (callback_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付回调记录表';
