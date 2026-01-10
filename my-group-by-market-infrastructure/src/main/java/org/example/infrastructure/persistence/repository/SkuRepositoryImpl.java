package org.example.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.model.goods.Sku;
import org.example.domain.model.goods.repository.SkuRepository;
import org.example.domain.model.goods.valueobject.GoodsStatus;
import org.example.infrastructure.persistence.converter.SkuConverter;
import org.example.infrastructure.persistence.mapper.SkuMapper;
import org.example.infrastructure.persistence.po.SkuPO;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * SKU 仓储实现
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class SkuRepositoryImpl implements SkuRepository {

    private final SkuMapper skuMapper;
    private final SkuConverter skuConverter;

    @Override
    public void save(Sku sku) {
        SkuPO po = skuConverter.toPO(sku);
        skuMapper.insert(po);
    }

    @Override
    public void update(Sku sku) {
        SkuPO existing = skuMapper.selectByGoodsId(sku.getGoodsId());
        if (existing != null) {
            SkuPO po = skuConverter.toPO(sku);
            po.setId(existing.getId());
            skuMapper.updateById(po);
        }
    }

    @Override
    public Optional<Sku> findByGoodsId(String goodsId) {
        SkuPO po = skuMapper.selectByGoodsId(goodsId);
        return Optional.ofNullable(po).map(skuConverter::toDomain);
    }

    @Override
    public List<Sku> findBySpuId(String spuId) {
        List<SkuPO> poList = skuMapper.selectBySpuId(spuId);
        return skuConverter.toDomainList(poList);
    }

    @Override
    public List<Sku> findByStatus(GoodsStatus status) {
        List<SkuPO> poList = skuMapper.selectByStatus(status.name());
        return skuConverter.toDomainList(poList);
    }

    @Override
    public List<Sku> findAllOnSale() {
        List<SkuPO> poList = skuMapper.selectAllOnSale();
        return skuConverter.toDomainList(poList);
    }

    @Override
    public List<Sku> findAll(int page, int size) {
        Page<SkuPO> pageResult = skuMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<>());
        return skuConverter.toDomainList(pageResult.getRecords());
    }

    @Override
    public int freezeStock(String goodsId, int quantity) {
        int rows = skuMapper.freezeStock(goodsId, quantity);
        if (rows > 0) {
            SkuPO updated = skuMapper.selectByGoodsId(goodsId);
            log.info("【SKU仓储】库存冻结成功, goodsId: {}, 冻结量: {}, 当前冻结: {}",
                    goodsId, quantity, updated.getFrozenStock());
            return updated.getFrozenStock();
        }
        log.warn("【SKU仓储】库存冻结失败, goodsId: {}, 冻结量: {}", goodsId, quantity);
        return -1;
    }

    @Override
    public int unfreezeStock(String goodsId, int quantity) {
        int rows = skuMapper.unfreezeStock(goodsId, quantity);
        if (rows > 0) {
            SkuPO updated = skuMapper.selectByGoodsId(goodsId);
            log.info("【SKU仓储】库存释放成功, goodsId: {}, 释放量: {}, 当前冻结: {}",
                    goodsId, quantity, updated.getFrozenStock());
            return updated.getFrozenStock();
        }
        log.warn("【SKU仓储】库存释放失败, goodsId: {}, 释放量: {}", goodsId, quantity);
        return -1;
    }

    @Override
    public int deductStock(String goodsId, int quantity) {
        int rows = skuMapper.deductStock(goodsId, quantity);
        if (rows > 0) {
            SkuPO updated = skuMapper.selectByGoodsId(goodsId);
            log.info("【SKU仓储】库存扣减成功, goodsId: {}, 扣减量: {}, 剩余库存: {}",
                    goodsId, quantity, updated.getStock());
            return updated.getStock();
        }
        log.warn("【SKU仓储】库存扣减失败, goodsId: {}, 扣减量: {}", goodsId, quantity);
        return -1;
    }

    @Override
    public void deleteByGoodsId(String goodsId) {
        SkuPO existing = skuMapper.selectByGoodsId(goodsId);
        if (existing != null) {
            skuMapper.deleteById(existing.getId());
        }
    }
}
