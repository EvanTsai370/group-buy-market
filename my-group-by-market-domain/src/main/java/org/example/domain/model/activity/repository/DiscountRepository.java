package org.example.domain.model.activity.repository;

import org.example.common.model.PageResult;
import org.example.domain.model.activity.Discount;

import java.util.List;
import java.util.Optional;

/**
 * Discount 仓储接口
 *
 * @author 开发团队
 * @since 2026-01-21
 */
public interface DiscountRepository {

    /**
     * 保存折扣
     */
    void save(Discount discount);

    /**
     * 更新折扣
     */
    void update(Discount discount);

    /**
     * 根据折扣ID查询
     */
    Optional<Discount> findById(String discountId);

    /**
     * 根据折扣名称查询（用于唯一性校验）
     */
    Optional<Discount> findByName(String discountName);

    /**
     * 查询所有折扣（用于下拉列表）
     */
    List<Discount> findAll();

    /**
     * 分页查询折扣
     */
    PageResult<Discount> findAll(int page, int size);

    /**
     * 删除折扣（软删除）
     */
    void deleteById(String discountId);
}
