-- =====================================================================
-- V5__rename_goods_id_to_sku_id.sql
-- 
-- 将 goods_id 字段重命名为 sku_id，提高语义清晰度
-- 
-- 警告：生产环境执行前需备份数据
-- =====================================================================

-- SKU 表
ALTER TABLE sku CHANGE COLUMN goods_id sku_id VARCHAR(32) NOT NULL COMMENT 'SKU ID';

-- 交易订单表
ALTER TABLE trade_order CHANGE COLUMN goods_id sku_id VARCHAR(32) NOT NULL COMMENT 'SKU ID';

-- 拼团订单表
ALTER TABLE `order` CHANGE COLUMN goods_id sku_id VARCHAR(32) NOT NULL COMMENT 'SKU ID';

-- 活动商品关联表
ALTER TABLE activity_goods CHANGE COLUMN goods_id sku_id VARCHAR(32) NOT NULL COMMENT 'SKU ID';
