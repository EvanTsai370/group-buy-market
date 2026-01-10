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
    Optional<Sku> findByGoodsId(String goodsId);

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
    int freezeStock(String goodsId, int quantity);

    /**
     * 原子释放库存
     * 
     * @return 释放后的冻结库存量，-1 表示失败
     */
    int unfreezeStock(String goodsId, int quantity);

    /**
     * 原子扣减库存
     * 
     * @return 扣减后的库存量，-1 表示失败
     */
    int deductStock(String goodsId, int quantity);

    /**
     * 删除
     */
    void deleteByGoodsId(String goodsId);
}
