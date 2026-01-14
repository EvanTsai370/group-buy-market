-- 测试数据：用于 TradeOrderServiceIdempotencyTest
-- 这个文件在测试执行时被Flyway加载，为测试提供基础数据

-- 插入测试活动
INSERT INTO activity (activity_id, activity_name, activity_desc, discount_id, tag_id, tag_scope, group_type, target, valid_time, participation_limit, start_time, end_time, status) 
VALUES (
    'ACT001', 
    '测试活动-iPhone拼团', 
    '用于幂等性测试的活动', 
    'DIS001', 
    NULL, 
    'OPEN', 
    0, -- 虚拟成团
    5, -- 5人团
    1800, -- 30分钟
    10, -- 每人最多参与10次
    '2026-01-01 00:00:00', 
    '2026-12-31 23:59:59', 
    'ACTIVE'
);

-- 插入测试折扣（8折）
INSERT INTO discount (discount_id, discount_name, discount_desc, discount_amount, discount_type, market_plan, market_expr, tag_id) 
VALUES (
    'DIS001', 
    '测试折扣-8折', 
    '用于测试', 
    100.00, 
    'BASE', 
    'ZJ', -- 直减
    '100', -- 减100元
    NULL
);

-- 插入测试商品 - SPU 和 SKU
INSERT INTO spu (spu_id, spu_name, category_id, brand, description, status) 
VALUES (
    'SPU001', 
    'iPhone 15 Pro', 
    'CAT001',
    'Apple',
    '最新款iPhone',
    'ON_SALE'
);

INSERT INTO sku (sku_id, spu_id, goods_name, spec_info, original_price, stock, frozen_stock, status) 
VALUES (
    'SKU001', 
    'SPU001',
    'iPhone 15 Pro  256GB',
    '{"color":"黑色","storage":"256GB"}',
    999.00,
    1000,
    0,
    'ON_SALE'
);

-- 插入活动商品关联
INSERT INTO activity_goods (activity_id, spu_id, source, channel)
VALUES ('ACT001', 'SPU001', 's01', 'c01');


-- 插入测试用户账户（用于并发测试）
INSERT INTO account (account_id, user_id, activity_id, participation_count, version) 
VALUES (
    'ACC_TEST_001', 
    'USER_TEST_001', 
    'ACT001', 
    0, -- 初始参团次数为0
    1  -- 初始版本号
);

-- 插入额外的测试用户（用于顺序测试）
INSERT INTO account (account_id, user_id, activity_id, participation_count, version) 
VALUES (
    'ACC_SEQ_001', 
    'USER_SEQ_001', 
    'ACT001', 
    0,
    1
);
