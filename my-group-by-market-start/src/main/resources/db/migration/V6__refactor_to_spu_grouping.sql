-- =====================================================================
-- V6__refactor_to_spu_grouping.sql
-- 
-- 将拼团模式从 SKU 级别（严格匹配规格）重构为 SPU 级别（宽松匹配商品）
-- 涉及 order 表和 activity_goods 表的字段变更
-- 
-- 警告：生产环境执行前需备份数据
-- =====================================================================

-- 1. order 表：sku_id -> spu_id
ALTER TABLE `order` CHANGE COLUMN sku_id spu_id VARCHAR(32) NOT NULL COMMENT 'SPU ID（拼团绑定的商品大类）';

-- 2. activity_goods 表：sku_id -> spu_id
-- 注意：这里假设之前的 activity_goods 是配置在 SKU 上的，现在需要迁移到 SPU 上
-- 如果同一 SPU 下有多个 SKU 配置了活动，可能会有重复，这里简单改名，业务层需保证配置唯一性
ALTER TABLE activity_goods CHANGE COLUMN sku_id spu_id VARCHAR(32) NOT NULL COMMENT 'SPU ID';
