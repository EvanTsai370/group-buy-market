package org.example.domain.model.goods.repository;

import org.example.common.model.PageResult;
import org.example.domain.model.goods.Spu;
import org.example.domain.model.goods.valueobject.GoodsStatus;

import java.util.List;
import java.util.Optional;

/**
 * SPU 仓储接口
 * 
 */
public interface SpuRepository {

    /**
     * 保存 SPU
     */
    void save(Spu spu);

    /**
     * 更新 SPU
     */
    void update(Spu spu);

    /**
     * 根据 SPU ID 查询
     */
    Optional<Spu> findBySpuId(String spuId);

    /**
     * 根据分类查询
     */
    List<Spu> findByCategoryId(String categoryId);

    /**
     * 根据状态查询
     */
    List<Spu> findByStatus(GoodsStatus status);

    /**
     * 查询所有在售商品
     */
    List<Spu> findAllOnSale();

    /**
     * 分页查询
     */
    PageResult<Spu> findAll(int page, int size);

    /**
     * 根据 SPU 名称查询（用于唯一性校验）
     */
    Optional<Spu> findBySpuName(String spuName);

    /**
     * 删除（软删除）
     */
    void deleteBySpuId(String spuId);
}
