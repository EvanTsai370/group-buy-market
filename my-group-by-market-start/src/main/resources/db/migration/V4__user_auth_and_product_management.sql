-- ============================================
-- V4: 用户认证系统 + SPU/SKU 商品管理 + 支付记录
-- ============================================

-- 1. 用户表（认证主表）
CREATE TABLE `user` (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增ID',
    user_id VARCHAR(50) NOT NULL UNIQUE COMMENT '用户ID（业务主键）',
    username VARCHAR(50) UNIQUE COMMENT '用户名（用户名密码登录）',
    password VARCHAR(255) COMMENT '加密密码（BCrypt）',
    nickname VARCHAR(100) COMMENT '昵称',
    avatar VARCHAR(500) COMMENT '头像URL',
    phone VARCHAR(20) UNIQUE COMMENT '手机号',
    email VARCHAR(100) UNIQUE COMMENT '邮箱',
    
    -- 用户状态
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/DISABLED/LOCKED',
    role VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色：USER/ADMIN',
    
    -- 审计字段
    last_login_time DATETIME COMMENT '最后登录时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_phone (phone),
    INDEX idx_email (email),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 2. 第三方登录绑定表（OAuth）
CREATE TABLE user_oauth (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增ID',
    user_id VARCHAR(50) NOT NULL COMMENT '用户ID',
    provider VARCHAR(20) NOT NULL COMMENT '提供商：WECHAT/QQ/ALIPAY',
    provider_user_id VARCHAR(100) NOT NULL COMMENT '第三方用户ID（openid）',
    union_id VARCHAR(100) COMMENT '跨应用统一ID（微信的unionId）',
    access_token VARCHAR(500) COMMENT '访问令牌',
    refresh_token VARCHAR(500) COMMENT '刷新令牌',
    token_expire_time DATETIME COMMENT '令牌过期时间',
    
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    UNIQUE KEY uk_provider_user (provider, provider_user_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='第三方登录绑定表';

-- 3. SPU 商品主表（Standard Product Unit）
CREATE TABLE spu (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增ID',
    spu_id VARCHAR(50) NOT NULL UNIQUE COMMENT 'SPU ID',
    spu_name VARCHAR(200) NOT NULL COMMENT '商品名称',
    category_id VARCHAR(50) COMMENT '分类ID',
    brand VARCHAR(100) COMMENT '品牌',
    description TEXT COMMENT '商品描述',
    main_image VARCHAR(500) COMMENT '主图URL',
    detail_images TEXT COMMENT '详情图列表（JSON数组）',
    status VARCHAR(20) NOT NULL DEFAULT 'ON_SALE' COMMENT '状态：ON_SALE/OFF_SALE',
    sort_order INT DEFAULT 0 COMMENT '排序权重',
    
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_category (category_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品SPU表';

-- 4. 扩展 SKU 表
ALTER TABLE sku 
    ADD COLUMN spu_id VARCHAR(50) COMMENT 'SPU ID' AFTER goods_id,
    ADD COLUMN spec_info TEXT COMMENT '规格信息（JSON，如：{"颜色":"红色","尺寸":"XL"}）' AFTER goods_name,
    ADD COLUMN stock INT NOT NULL DEFAULT 0 COMMENT '库存数量' AFTER original_price,
    ADD COLUMN frozen_stock INT NOT NULL DEFAULT 0 COMMENT '冻结库存（锁单预占）' AFTER stock,
    ADD COLUMN sku_image VARCHAR(500) COMMENT 'SKU图片' AFTER frozen_stock,
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ON_SALE' COMMENT '状态：ON_SALE/OFF_SALE' AFTER sku_image,
    ADD INDEX idx_spu_id (spu_id),
    ADD INDEX idx_status (status);

-- 5. 支付记录表
CREATE TABLE payment_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增ID',
    payment_id VARCHAR(50) NOT NULL UNIQUE COMMENT '支付记录ID',
    trade_order_id VARCHAR(50) NOT NULL COMMENT '交易订单ID',
    
    -- 支付渠道信息
    channel VARCHAR(20) NOT NULL COMMENT '支付渠道：ALIPAY/WECHAT/MOCK',
    channel_order_no VARCHAR(100) COMMENT '渠道订单号（预支付订单号）',
    channel_trade_no VARCHAR(100) COMMENT '渠道交易流水号',
    
    -- 金额信息
    amount DECIMAL(10,2) NOT NULL COMMENT '支付金额',
    
    -- 状态
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/SUCCESS/FAILED/REFUND',
    
    -- 回调信息
    callback_time DATETIME COMMENT '回调时间',
    callback_data TEXT COMMENT '回调原始数据（JSON）',
    
    -- 退款信息
    refund_amount DECIMAL(10,2) COMMENT '退款金额',
    refund_time DATETIME COMMENT '退款时间',
    refund_reason VARCHAR(500) COMMENT '退款原因',
    
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_trade_order_id (trade_order_id),
    INDEX idx_channel_order_no (channel_order_no),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付记录表';

-- 6. 库存服务接口记录表（对接外部库存服务）
CREATE TABLE inventory_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增ID',
    record_id VARCHAR(50) NOT NULL UNIQUE COMMENT '记录ID',
    goods_id VARCHAR(50) NOT NULL COMMENT '商品ID',
    order_id VARCHAR(50) NOT NULL COMMENT '拼团订单ID',
    
    -- 操作类型
    operation_type VARCHAR(20) NOT NULL COMMENT '操作类型：FREEZE/DEDUCT/ROLLBACK',
    quantity INT NOT NULL COMMENT '数量',
    
    -- 状态
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/SUCCESS/FAILED',
    error_msg VARCHAR(500) COMMENT '错误信息',
    
    -- 外部服务响应
    external_request TEXT COMMENT '外部服务请求（JSON）',
    external_response TEXT COMMENT '外部服务响应（JSON）',
    
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_goods_id (goods_id),
    INDEX idx_order_id (order_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存操作记录表';

-- 7. 插入默认管理员账户（密码: admin123，BCrypt加密）
INSERT INTO `user` (user_id, username, password, nickname, role, status) VALUES
('ADMIN001', 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt7I6Je', '系统管理员', 'ADMIN', 'ACTIVE');
