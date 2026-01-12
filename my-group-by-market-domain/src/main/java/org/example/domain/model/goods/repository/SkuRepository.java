package org.example.domain.model.goods.repository;

import org.example.domain.model.goods.Sku;
import org.example.domain.model.goods.valueobject.GoodsStatus;

import java.util.List;
import java.util.Optional;

/**
 * SKU 仓储接口
 * 
 * @author 开发团队
 * @since 2026-01-10
 */
public interface SkuRepository {

    /**
     * 保存 SKU
     */
    void save(Sku sku);

    /**
     * 更新 SKU
     */
    void update(Sku sku);

    /**
     * 根据 goods ID 查询
     */
    Optional<Sku> findBySkuId(String skuId);

    /**
     * 根据 SPU ID 查询所有 SKU
     */
    List<Sku> findBySpuId(String spuId);

    /**
     * 根据状态查询
     */
    List<Sku> findByStatus(GoodsStatus status);

    /**
     * 查询所有在售 SKU
     */
    List<Sku> findAllOnSale();

    /**
     * 分页查询
     */
    List<Sku> findAll(int page, int size);

    /**
     * 原子冻结库存
     * 
     * @return 冻结后的库存量，-1 表示失败
     */
    int freezeStock(String skuId, int quantity);

    /**
     * 原子释放库存
     * 
     * @return 释放后的冻结库存量，-1 表示失败
     */
    int unfreezeStock(String skuId, int quantity);

    /**
     * 原子扣减库存
     * 
     * @return 扣减后的库存量，-1 表示失败
     */
    int deductStock(String skuId, int quantity);

    /**
     * 删除
     */
    void deleteBySkuId(String skuId);

    // ==================== TODO: 发货与退货库存操作 ====================
    //
    // 以下方法待与 InventoryGateway 微服务集成后实现：
    //
    // 1. shipStock(skuId, quantity) - 发货时调用
    // 操作：frozenStock - quantity, stock - quantity
    // 场景：拼团成功后发货
    //
    // 2. returnStock(skuId, quantity) - 退货时调用
    // 操作：stock + quantity
    // 场景：已发货订单退货
    //
    // 当前 unfreezeStock() 仅适用于未发货订单的退款场景
    // =================================================================
}
