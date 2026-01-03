package org.example.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.model.goods.Sku;
import org.example.domain.model.goods.repository.SkuRepository;
import org.example.infrastructure.persistence.converter.SkuConverter;
import org.example.infrastructure.persistence.mapper.SkuMapper;
import org.example.infrastructure.persistence.po.SkuPO;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * SKU 仓储实现
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class SkuRepositoryImpl implements SkuRepository {

    private final SkuMapper skuMapper;

    @Override
    public Optional<Sku> findByGoodsId(String goodsId) {
        LambdaQueryWrapper<SkuPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SkuPO::getGoodsId, goodsId);

        SkuPO po = skuMapper.selectOne(wrapper);
        if (po == null) {
            log.warn("【SkuRepository】商品不存在，goodsId: {}", goodsId);
            return Optional.empty();
        }

        Sku sku = SkuConverter.INSTANCE.toDomain(po);
        log.info("【SkuRepository】查询商品，goodsId: {}, goodsName: {}", goodsId, sku.getGoodsName());
        return Optional.of(sku);
    }
}
