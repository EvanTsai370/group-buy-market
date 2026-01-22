package org.example.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.example.common.model.PageResult;
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
        // MyBatis-Plus 会根据主键是否存在自动判断 INSERT/UPDATE
        spuMapper.insertOrUpdate(po);
    }

    @Override
    public void update(Spu spu) {
        SpuPO po = spuConverter.toPO(spu);
        // 直接使用业务ID更新
        spuMapper.updateById(po);
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
    public PageResult<Spu> findAll(int page, int size) {
        Page<SpuPO> pageResult = spuMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<SpuPO>().orderByDesc(SpuPO::getSortOrder));

        return PageResult.of(
                spuConverter.toDomainList(pageResult.getRecords()),
                pageResult.getTotal(),
                page,
                size);
    }

    @Override
    public Optional<Spu> findBySpuName(String spuName) {
        LambdaQueryWrapper<SpuPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SpuPO::getSpuName, spuName);
        SpuPO po = spuMapper.selectOne(wrapper);
        return Optional.ofNullable(po).map(spuConverter::toDomain);
    }

    @Override
    public void deleteBySpuId(String spuId) {
        // 直接使用业务ID删除
        spuMapper.deleteById(spuId);
    }
}
