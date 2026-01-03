package org.example.domain.model.goods.repository;

import org.example.domain.model.goods.Sku;

import java.util.Optional;

/**
 * 商品仓储接口
 */
public interface SkuRepository {

    /**
     * 根据商品ID查询商品信息
     *
     * @param goodsId 商品ID
     * @return 商品信息
     */
    Optional<Sku> findByGoodsId(String goodsId);
}
