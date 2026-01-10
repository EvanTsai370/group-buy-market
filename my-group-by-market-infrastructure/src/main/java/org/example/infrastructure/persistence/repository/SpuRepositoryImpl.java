package org.example.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.example.domain.model.goods.Spu;
import org.example.domain.model.goods.repository.SpuRepository;
import org.example.domain.model.goods.valueobject.GoodsStatus;
import org.example.infrastructure.persistence.converter.SpuConverter;
import org.example.infrastructure.persistence.mapper.SpuMapper;
import org.example.infrastructure.persistence.po.SpuPO;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * SPU 仓储实现
 */
@Repository
@RequiredArgsConstructor
public class SpuRepositoryImpl implements SpuRepository {

    private final SpuMapper spuMapper;
    private final SpuConverter spuConverter;

    @Override
    public void save(Spu spu) {
        SpuPO po = spuConverter.toPO(spu);
        spuMapper.insert(po);
    }

    @Override
    public void update(Spu spu) {
        SpuPO existing = spuMapper.selectBySpuId(spu.getSpuId());
        if (existing != null) {
            SpuPO po = spuConverter.toPO(spu);
            po.setId(existing.getId());
            spuMapper.updateById(po);
        }
    }

    @Override
    public Optional<Spu> findBySpuId(String spuId) {
        SpuPO po = spuMapper.selectBySpuId(spuId);
        return Optional.ofNullable(po).map(spuConverter::toDomain);
    }

    @Override
    public List<Spu> findByCategoryId(String categoryId) {
        List<SpuPO> poList = spuMapper.selectByCategoryId(categoryId);
        return spuConverter.toDomainList(poList);
    }

    @Override
    public List<Spu> findByStatus(GoodsStatus status) {
        List<SpuPO> poList = spuMapper.selectByStatus(status.name());
        return spuConverter.toDomainList(poList);
    }

    @Override
    public List<Spu> findAllOnSale() {
        List<SpuPO> poList = spuMapper.selectAllOnSale();
        return spuConverter.toDomainList(poList);
    }

    @Override
    public List<Spu> findAll(int page, int size) {
        Page<SpuPO> pageResult = spuMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<SpuPO>().orderByDesc(SpuPO::getSortOrder));
        return spuConverter.toDomainList(pageResult.getRecords());
    }

    @Override
    public void deleteBySpuId(String spuId) {
        SpuPO existing = spuMapper.selectBySpuId(spuId);
        if (existing != null) {
            spuMapper.deleteById(existing.getId());
        }
    }
}
