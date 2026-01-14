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
        // MyBatis-Plus 会根据主键是否存在自动判断 INSERT/UPDATE
        skuMapper.insertOrUpdate(po);
    }

    @Override
    public void update(Sku sku) {
        SkuPO po = skuConverter.toPO(sku);
        // 直接使用业务ID更新
        skuMapper.updateById(po);
    }

    @Override
    public Optional<Sku> findBySkuId(String skuId) {
        SkuPO po = skuMapper.selectBySkuId(skuId);
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
    public int freezeStock(String skuId, int quantity) {
        int rows = skuMapper.freezeStock(skuId, quantity);
        if (rows > 0) {
            SkuPO updated = skuMapper.selectBySkuId(skuId);
            log.info("【SKU仓储】库存冻结成功, skuId: {}, 冻结量: {}, 当前冻结: {}",
                    skuId, quantity, updated.getFrozenStock());
            return updated.getFrozenStock();
        }
        log.warn("【SKU仓储】库存冻结失败, skuId: {}, 冻结量: {}", skuId, quantity);
        return -1;
    }

    @Override
    public int unfreezeStock(String skuId, int quantity) {
        int rows = skuMapper.unfreezeStock(skuId, quantity);
        if (rows > 0) {
            SkuPO updated = skuMapper.selectBySkuId(skuId);
            log.info("【SKU仓储】库存释放成功, skuId: {}, 释放量: {}, 当前冻结: {}",
                    skuId, quantity, updated.getFrozenStock());
            return updated.getFrozenStock();
        }
        log.warn("【SKU仓储】库存释放失败, skuId: {}, 释放量: {}", skuId, quantity);
        return -1;
    }

    @Override
    public int deductStock(String skuId, int quantity) {
        int rows = skuMapper.deductStock(skuId, quantity);
        if (rows > 0) {
            SkuPO updated = skuMapper.selectBySkuId(skuId);
            log.info("【SKU仓储】库存扣减成功, skuId: {}, 扣减量: {}, 剩余库存: {}",
                    skuId, quantity, updated.getStock());
            return updated.getStock();
        }
        log.warn("【SKU仓储】库存扣减失败, skuId: {}, 扣减量: {}", skuId, quantity);
        return -1;
    }

    @Override
    public void deleteBySkuId(String skuId) {
        // 直接使用业务ID删除
        skuMapper.deleteById(skuId);
    }
}
