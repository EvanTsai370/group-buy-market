package org.example.domain.model.goods.repository;

import org.example.domain.model.goods.Spu;
import org.example.domain.model.goods.valueobject.GoodsStatus;

import java.util.List;
import java.util.Optional;

/**
 * SPU 仓储接口
 * 
 * @author 开发团队
 * @since 2026-01-10
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
    List<Spu> findAll(int page, int size);

    /**
     * 删除（软删除）
     */
    void deleteBySpuId(String spuId);
}
