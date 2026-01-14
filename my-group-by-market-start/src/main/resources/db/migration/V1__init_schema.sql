-- ============================================
-- 拼团营销系统数据库表结构（合并版本）
-- 设计原则：
-- 1. 去掉 group_buy 前缀，使用简洁命名
-- 2. 添加乐观锁 version 字段
-- 3. 所有聚合都有状态管理
-- 4. 反映最新的表结构（基于 V1-V6 的演变）
-- ============================================

-- 1. 拼团活动表
CREATE TABLE activity (
    activity_id VARCHAR(50) PRIMARY KEY COMMENT '活动ID（业务主键）',
    activity_name VARCHAR(100) NOT NULL COMMENT '活动名称',
    activity_desc VARCHAR(500) COMMENT '活动描述',

    -- 关联配置
    discount_id VARCHAR(50) NOT NULL COMMENT '默认折扣ID',
    tag_id VARCHAR(50) COMMENT '人群标签ID',
    tag_scope VARCHAR(20) DEFAULT 'STRICT' COMMENT '人群标签作用域：STRICT=严格模式，VISIBLE_ONLY=仅可见，OPEN=开放模式',

    -- 成团规则
    group_type TINYINT NOT NULL DEFAULT 0 COMMENT '成团方式：0=虚拟成团，1=真实成团',
    target INT NOT NULL COMMENT '成团目标人数（3人团、5人团）',
    valid_time INT NOT NULL DEFAULT 1200 COMMENT '拼单有效时长（秒），默认20分钟',
    participation_limit INT NOT NULL DEFAULT 1 COMMENT '用户参团次数限制',

    -- 活动时间
    start_time DATETIME NOT NULL COMMENT '活动开始时间',
    end_time DATETIME NOT NULL COMMENT '活动结束时间',

    -- 状态管理
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '活动状态：DRAFT/ACTIVE/CLOSED',

    -- 审计字段
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_status_time (status, start_time, end_time),
    INDEX idx_tag_id (tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拼团活动表';

-- 2. 活动商品关联表（支持一个活动配置多个商品）
CREATE TABLE activity_goods (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增ID（保留，用于联合主键场景）',
    activity_id VARCHAR(50) NOT NULL COMMENT '活动ID',
    spu_id VARCHAR(32) NOT NULL COMMENT 'SPU ID',

    -- 来源追踪（商品维度）
    source VARCHAR(50) NOT NULL COMMENT '来源（如：s01-小程序、s02-App）',
    channel VARCHAR(50) NOT NULL COMMENT '渠道（如：c01-首页、c02-搜索）',

    -- 可选：商品级别的折扣覆盖
    discount_id VARCHAR(50) COMMENT '折扣ID（为空则使用活动默认折扣）',

    -- 审计字段
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_activity_goods_source_channel (activity_id, spu_id, source, channel),
    INDEX idx_spu_id (spu_id),
    INDEX idx_activity_id (activity_id),
    INDEX idx_source_channel (source, channel)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动商品关联表';

-- 3. 折扣配置表
CREATE TABLE discount (
    discount_id VARCHAR(50) PRIMARY KEY COMMENT '折扣ID（业务主键）',
    discount_name VARCHAR(100) NOT NULL COMMENT '折扣标题',
    discount_desc VARCHAR(500) COMMENT '折扣描述',

    -- 折扣金额（简化设计，直接存金额）
    discount_amount DECIMAL(10,2) NOT NULL COMMENT '折扣金额',

    -- 折扣类型
    discount_type VARCHAR(20) NOT NULL DEFAULT 'BASE' COMMENT '类型：BASE=基础价格，TAG=标签专属',

    -- 营销策略（策略模式，预留扩展）
    market_plan VARCHAR(50) COMMENT '营销计划：ZJ=直减、MJ=满减、NYG=N元购',
    market_expr VARCHAR(500) COMMENT '营销表达式（JSON格式）',

    -- 可选：关联特定人群
    tag_id VARCHAR(50) COMMENT '人群标签ID（如果为空，则对所有人生效）',

    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_tag_id (tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='折扣配置表';

-- 4. 人群标签表
CREATE TABLE crowd_tag (
    tag_id VARCHAR(50) PRIMARY KEY COMMENT '标签ID（业务主键）',
    tag_name VARCHAR(100) NOT NULL COMMENT '标签名称（如：沉睡用户）',
    tag_desc VARCHAR(500) COMMENT '标签描述',
    tag_rule VARCHAR(500) COMMENT '标签规则（JSON表达式）',

    -- 统计信息
    statistics BIGINT DEFAULT 0 COMMENT '符合人数统计',

    -- 状态管理
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT/CALCULATING/COMPLETED/FAILED',

    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人群标签表';

-- 5. 人群标签明细表
CREATE TABLE crowd_tag_detail (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增ID（保留，明细表使用联合唯一约束）',
    tag_id VARCHAR(50) NOT NULL COMMENT '标签ID',
    user_id VARCHAR(50) NOT NULL COMMENT '用户ID',

    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_tag_user (tag_id, user_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人群标签明细表';

-- 6. 拼团账户表
CREATE TABLE account (
    account_id VARCHAR(50) PRIMARY KEY COMMENT '账户ID（业务主键）',
    user_id VARCHAR(50) NOT NULL COMMENT '用户ID',
    activity_id VARCHAR(50) NOT NULL COMMENT '活动ID',

    -- 参团次数控制
    participation_count INT NOT NULL DEFAULT 0 COMMENT '已参与次数',

    -- 并发控制（乐观锁）
    version BIGINT NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',

    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_user_activity (user_id, activity_id),
    INDEX idx_activity_id (activity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拼团账户表';

-- 7. 拼团订单表（主单）
CREATE TABLE `order` (
    order_id VARCHAR(50) PRIMARY KEY COMMENT '拼团订单ID（业务主键）',
    team_id VARCHAR(50) COMMENT '拼团队伍ID（8位随机数，用户友好）',
    activity_id VARCHAR(50) NOT NULL COMMENT '活动ID',

    -- 商品信息（冗余，避免JOIN）
    spu_id VARCHAR(32) NOT NULL COMMENT 'SPU ID（拼团绑定的商品大类）',
    original_price DECIMAL(10,2) NOT NULL COMMENT '原始价格',
    deduction_price DECIMAL(10,2) NOT NULL COMMENT '折扣价格',

    -- 拼团规则
    target_count INT NOT NULL COMMENT '目标人数',
    complete_count INT NOT NULL DEFAULT 0 COMMENT '完成人数（实际成团人数）',
    lock_count INT NOT NULL DEFAULT 0 COMMENT '锁单量（已锁定名额数量，防止超卖）',

    -- 状态管理
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/SUCCESS/FAILED',

    -- 时间管理
    start_time DATETIME NOT NULL COMMENT '拼团开始时间',
    deadline_time DATETIME NOT NULL COMMENT '参团截止时间（超时自动失败）',
    completed_time DATETIME COMMENT '实际成团时间（成功后填充）',

    -- 团长信息
    leader_user_id VARCHAR(50) NOT NULL COMMENT '团长用户ID',

    -- 支付总额
    pay_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '总支付金额',

    -- 来源追踪
    source VARCHAR(50) COMMENT '来源',
    channel VARCHAR(50) COMMENT '渠道',

    -- 回调接口
    notify_url VARCHAR(500) COMMENT '回调接口地址',

    -- 并发控制（乐观锁）
    version BIGINT NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',

    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_team_id (team_id),
    INDEX idx_activity_id (activity_id),
    INDEX idx_status_deadline (status, deadline_time),
    INDEX idx_completed_time (completed_time),
    INDEX idx_leader (leader_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拼团订单表';

-- 8. 用户表（认证主表）
CREATE TABLE `user` (
    user_id VARCHAR(50) PRIMARY KEY COMMENT '用户ID（业务主键）',
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

-- 9. 第三方登录绑定表（OAuth）
CREATE TABLE user_oauth (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增ID（保留，OAuth绑定无业务ID）',
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

-- 10. SPU 商品主表（Standard Product Unit）
CREATE TABLE spu (
    spu_id VARCHAR(50) PRIMARY KEY COMMENT 'SPU ID（业务主键）',
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

-- 11. 商品SKU表
CREATE TABLE sku (
    sku_id VARCHAR(32) PRIMARY KEY COMMENT 'SKU ID（业务主键）',
    spu_id VARCHAR(50) COMMENT 'SPU ID',
    goods_name VARCHAR(200) NOT NULL COMMENT '商品名称',
    spec_info TEXT COMMENT '规格信息（JSON，如：{"颜色":"红色","尺寸":"XL"}）',
    original_price DECIMAL(10,2) NOT NULL COMMENT '原价',
    stock INT NOT NULL DEFAULT 0 COMMENT '库存数量',
    frozen_stock INT NOT NULL DEFAULT 0 COMMENT '冻结库存（锁单预占）',
    sku_image VARCHAR(500) COMMENT 'SKU图片',
    status VARCHAR(20) NOT NULL DEFAULT 'ON_SALE' COMMENT '状态：ON_SALE/OFF_SALE',

    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_spu_id (spu_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品SKU表';

-- 12. 交易订单表（拼团锁单、支付、结算记录）
CREATE TABLE trade_order (
    trade_order_id VARCHAR(50) PRIMARY KEY COMMENT '交易订单ID（业务主键）',

    -- 关联信息
    team_id VARCHAR(50) NOT NULL COMMENT '拼团队伍ID（关联 order 表）',
    order_id VARCHAR(50) NOT NULL COMMENT '拼团订单ID（关联 order 表）',
    activity_id VARCHAR(50) NOT NULL COMMENT '活动ID',
    user_id VARCHAR(50) NOT NULL COMMENT '用户ID',

    -- 商品信息（冗余，避免JOIN）
    sku_id VARCHAR(32) NOT NULL COMMENT 'SKU ID',
    goods_name VARCHAR(200) NOT NULL COMMENT '商品名称',

    -- 金额信息
    original_price DECIMAL(10,2) NOT NULL COMMENT '原始价格（商品原价）',
    deduction_price DECIMAL(10,2) NOT NULL COMMENT '减免金额（优惠金额）',
    pay_price DECIMAL(10,2) NOT NULL COMMENT '实付金额（原价 - 减免）',

    -- 交易状态
    status VARCHAR(20) NOT NULL DEFAULT 'CREATE' COMMENT '交易状态：CREATE=已创建（锁单）, PAID=已支付, SETTLED=已结算, TIMEOUT=已超时, REFUND=已退单',

    -- 外部交易单号（幂等性保证）
    out_trade_no VARCHAR(100) NOT NULL COMMENT '外部交易单号（商城系统传入，用于幂等校验）',

    -- 支付时间
    pay_time DATETIME COMMENT '支付时间（用户支付成功的时间）',
    settlement_time DATETIME COMMENT '结算时间（拼团成功的时间）',

    -- 来源追踪
    source VARCHAR(50) NOT NULL COMMENT '来源（如：s01-小程序、s02-App）',
    channel VARCHAR(50) NOT NULL COMMENT '渠道（如：c01-首页、c02-搜索）',

    -- 回调通知配置
    notify_type VARCHAR(20) COMMENT '通知类型：HTTP=HTTP回调, MQ=消息队列',
    notify_url VARCHAR(500) COMMENT 'HTTP回调地址',
    notify_mq VARCHAR(100) COMMENT 'MQ主题',
    notify_status VARCHAR(20) DEFAULT 'INIT' COMMENT '通知状态：INIT=未发送, SUCCESS=成功, FAILED=失败',

    -- 退款相关字段
    refund_reason VARCHAR(500) COMMENT '退款原因',
    refund_time DATETIME COMMENT '退款时间',

    -- 审计字段
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    -- 索引设计
    UNIQUE KEY uk_out_trade_no (out_trade_no) COMMENT '外部交易单号唯一索引（幂等校验）',
    INDEX idx_team_id (team_id) COMMENT '队伍ID索引（查询某个团的所有交易）',
    INDEX idx_order_id (order_id) COMMENT '订单ID索引',
    INDEX idx_user_id (user_id) COMMENT '用户ID索引（查询用户的所有交易）',
    INDEX idx_activity_id (activity_id) COMMENT '活动ID索引',
    INDEX idx_status (status) COMMENT '状态索引（查询待结算订单）',
    INDEX idx_notify_status (notify_status) COMMENT '通知状态索引（查询待通知任务）',
    INDEX idx_refund_time (refund_time) COMMENT '退款时间索引（用于退款统计查询）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交易订单表（拼团锁单、支付、结算记录）';

-- 13. 回调任务表（可选，用于异步通知）
CREATE TABLE notify_task (
    task_id VARCHAR(50) PRIMARY KEY COMMENT '任务ID（业务主键）',

    -- 关联信息
    activity_id VARCHAR(50) NOT NULL COMMENT '活动ID',
    order_id VARCHAR(50) NOT NULL COMMENT '拼团订单ID',

    -- 回调配置
    notify_url VARCHAR(500) NOT NULL COMMENT '回调接口地址',
    parameter_json TEXT COMMENT '回调参数（JSON格式）',

    -- 重试控制
    notify_count INT NOT NULL DEFAULT 0 COMMENT '回调次数',
    max_retry_count INT NOT NULL DEFAULT 5 COMMENT '最大重试次数',

    -- 状态管理
    notify_status VARCHAR(20) NOT NULL DEFAULT 'INIT' COMMENT '状态：INIT/SUCCESS/RETRY/FAILED',
    error_msg VARCHAR(1000) COMMENT '错误信息',

    -- 时间管理
    next_retry_time DATETIME COMMENT '下次重试时间',
    last_notify_time DATETIME COMMENT '最后回调时间',

    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_order_id (order_id),
    INDEX idx_status_retry (notify_status, next_retry_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='回调任务表';

-- 14. 通知任务表（管理支付成功后的异步回调通知）
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

-- 15. 支付记录表
CREATE TABLE payment_record (
    payment_id VARCHAR(50) PRIMARY KEY COMMENT '支付记录ID（业务主键）',
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

-- 16. 库存服务接口记录表（对接外部库存服务）
CREATE TABLE inventory_record (
    record_id VARCHAR(50) PRIMARY KEY COMMENT '记录ID（业务主键）',
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

-- 17. 插入默认管理员账户（密码: admin123，BCrypt加密）
INSERT INTO `user` (user_id, username, password, nickname, role, status) VALUES
('ADMIN001', 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt7I6Je', '系统管理员', 'ADMIN', 'ACTIVE');
