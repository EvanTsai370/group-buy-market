-- ============================================
-- 拼团营销系统数据库表结构
-- 设计原则：
-- 1. 去掉 group_buy 前缀，使用简洁命名
-- 2. 添加乐观锁 version 字段
-- 3. 所有聚合都有状态管理
-- ============================================

-- 1. 拼团活动表
CREATE TABLE activity (
                          id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增ID',
                          activity_id VARCHAR(50) NOT NULL UNIQUE COMMENT '活动ID',
                          activity_name VARCHAR(100) NOT NULL COMMENT '活动名称',
                          activity_desc VARCHAR(500) COMMENT '活动描述',

    -- 关联配置
                          goods_id VARCHAR(50) NOT NULL COMMENT '商品ID（外部引用）',
                          discount_id VARCHAR(50) NOT NULL COMMENT '折扣ID',
                          tag_id VARCHAR(50) COMMENT '人群标签ID',

    -- 成团规则
                          group_type TINYINT NOT NULL DEFAULT 0 COMMENT '成团方式：0=虚拟成团，1=真实成团',
                          target INT NOT NULL COMMENT '成团目标人数（3人团、5人团）',
                          valid_time INT NOT NULL DEFAULT 1200 COMMENT '拼单有效时长（秒），默认20分钟',
                          take_limit_count INT NOT NULL DEFAULT 1 COMMENT '用户参团次数限制',

    -- 活动时间
                          start_time DATETIME NOT NULL COMMENT '活动开始时间',
                          end_time DATETIME NOT NULL COMMENT '活动结束时间',

    -- 状态管理
                          status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '活动状态：DRAFT/ACTIVE/CLOSED',

    -- 来源追踪
                          source VARCHAR(50) COMMENT '来源（如：s01-小程序、s02-App）',
                          channel VARCHAR(50) COMMENT '渠道（如：c01-首页、c02-搜索）',

    -- 审计字段
                          create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                          update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

                          INDEX idx_goods_id (goods_id),
                          INDEX idx_status_time (status, start_time, end_time),
                          INDEX idx_source_channel (source, channel),
                          INDEX idx_tag_id (tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拼团活动表';

-- 2. 折扣配置表
CREATE TABLE discount (
                          id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增ID',
                          discount_id VARCHAR(50) NOT NULL UNIQUE COMMENT '折扣ID',
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

-- 3. 人群标签表
CREATE TABLE crowd_tag (
                           id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增ID',
                           tag_id VARCHAR(50) NOT NULL UNIQUE COMMENT '标签ID',
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

-- 4. 人群标签明细表
CREATE TABLE crowd_tag_detail (
                                  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增ID',
                                  tag_id VARCHAR(50) NOT NULL COMMENT '标签ID',
                                  user_id VARCHAR(50) NOT NULL COMMENT '用户ID',

                                  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

                                  UNIQUE KEY uk_tag_user (tag_id, user_id),
                                  INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人群标签明细表';

-- 5. 拼团账户表
CREATE TABLE account (
                         id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增ID',
                         account_id VARCHAR(50) NOT NULL UNIQUE COMMENT '账户ID',
                         user_id VARCHAR(50) NOT NULL COMMENT '用户ID',
                         activity_id VARCHAR(50) NOT NULL COMMENT '活动ID',

    -- 参团次数控制
                         take_limit_count INT NOT NULL COMMENT '总限制次数',
                         take_limit_count_used INT NOT NULL DEFAULT 0 COMMENT '已使用次数',

                         create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                         update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

                         UNIQUE KEY uk_user_activity (user_id, activity_id),
                         INDEX idx_activity_id (activity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拼团账户表';

-- 6. 拼团订单表（主单）
CREATE TABLE `order` (
                         id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增ID',
                         order_id VARCHAR(50) NOT NULL UNIQUE COMMENT '拼团订单ID',
                         activity_id VARCHAR(50) NOT NULL COMMENT '活动ID',

    -- 商品信息（冗余，避免JOIN）
                         goods_id VARCHAR(50) NOT NULL COMMENT '商品ID',
                         original_price DECIMAL(10,2) NOT NULL COMMENT '原始价格',
                         deduction_price DECIMAL(10,2) NOT NULL COMMENT '折扣价格',

    -- 拼团规则
                         target_count INT NOT NULL COMMENT '目标人数',
                         complete_count INT NOT NULL DEFAULT 0 COMMENT '完成人数',

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

                         INDEX idx_activity_id (activity_id),
                         INDEX idx_status_deadline (status, deadline_time),
                         INDEX idx_completed_time (completed_time),
                         INDEX idx_leader (leader_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拼团订单表';

-- 7. 拼团订单明细表
CREATE TABLE order_detail (
                              id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增ID',
                              detail_id VARCHAR(50) NOT NULL UNIQUE COMMENT '明细ID',

    -- 关联信息
                              order_id VARCHAR(50) NOT NULL COMMENT '拼团订单ID',
                              activity_id VARCHAR(50) NOT NULL COMMENT '活动ID',
                              user_id VARCHAR(50) NOT NULL COMMENT '用户ID',

    -- 用户角色
                              user_type VARCHAR(20) NOT NULL COMMENT '用户类型：LEADER=团长，MEMBER=团员',

    -- 商品信息（冗余）
                              goods_id VARCHAR(50) NOT NULL COMMENT '商品ID',

    -- 支付信息
                              pay_amount DECIMAL(10,2) NOT NULL COMMENT '实际支付金额',
                              out_trade_no VARCHAR(100) COMMENT '外部交易单号（幂等校验）',

    -- 来源追踪
                              source VARCHAR(50) COMMENT '来源',
                              channel VARCHAR(50) COMMENT '渠道',

                              create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                              update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

                              INDEX idx_order_id (order_id),
                              INDEX idx_user_id (user_id),
                              INDEX idx_activity_id (activity_id),
                              UNIQUE KEY uk_out_trade_no (out_trade_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拼团订单明细表';

-- 8. 商品SKU表（简化版，用于试算）
CREATE TABLE sku (
                     id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增ID',
                     goods_id VARCHAR(50) NOT NULL UNIQUE COMMENT '商品ID',
                     goods_name VARCHAR(200) NOT NULL COMMENT '商品名称',
                     original_price DECIMAL(10,2) NOT NULL COMMENT '原价',

                     create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                     update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品SKU表';

-- 9. 回调任务表（可选，用于异步通知）
CREATE TABLE notify_task (
                             id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增ID',
                             task_id VARCHAR(50) NOT NULL UNIQUE COMMENT '任务ID',

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

