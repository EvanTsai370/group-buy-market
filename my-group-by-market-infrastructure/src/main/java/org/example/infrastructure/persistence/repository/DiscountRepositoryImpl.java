package org.example.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.model.PageResult;
import org.example.domain.model.activity.Discount;
import org.example.domain.model.activity.repository.DiscountRepository;
import org.example.infrastructure.persistence.converter.DiscountConverter;
import org.example.infrastructure.persistence.mapper.DiscountMapper;
import org.example.infrastructure.persistence.po.DiscountPO;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Discount 仓储实现
 *
 * @author 开发团队
 * @since 2026-01-21
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class DiscountRepositoryImpl implements DiscountRepository {

    private final DiscountMapper discountMapper;
    private final DiscountConverter discountConverter;

    @Override
    public void save(Discount discount) {
        DiscountPO po = discountConverter.toPO(discount);
        discountMapper.insert(po);
        log.info("【DiscountRepository】保存折扣成功, discountId: {}", discount.getDiscountId());
    }

    @Override
    public void update(Discount discount) {
        DiscountPO po = discountConverter.toPO(discount);
        discountMapper.updateById(po);
        log.info("【DiscountRepository】更新折扣成功, discountId: {}", discount.getDiscountId());
    }

    @Override
    public Optional<Discount> findById(String discountId) {
        DiscountPO po = discountMapper.selectById(discountId);
        return Optional.ofNullable(po).map(discountConverter::toDomain);
    }

    @Override
    public Optional<Discount> findByName(String discountName) {
        LambdaQueryWrapper<DiscountPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DiscountPO::getDiscountName, discountName);
        DiscountPO po = discountMapper.selectOne(wrapper);
        return Optional.ofNullable(po).map(discountConverter::toDomain);
    }

    @Override
    public List<Discount> findAll() {
        List<DiscountPO> poList = discountMapper.selectList(null);
        return poList.stream()
                .map(discountConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public PageResult<Discount> findAll(int page, int size) {
        Page<DiscountPO> pageParam = new Page<>(page, size);
        IPage<DiscountPO> pageResult = discountMapper.selectPage(pageParam, null);

        List<Discount> list = pageResult.getRecords().stream()
                .map(discountConverter::toDomain)
                .collect(Collectors.toList());

        return PageResult.of(
                list,
                pageResult.getTotal(),
                page,
                size
        );
    }

    @Override
    public void deleteById(String discountId) {
        discountMapper.deleteById(discountId);
        log.info("【DiscountRepository】删除折扣成功, discountId: {}", discountId);
    }
}
