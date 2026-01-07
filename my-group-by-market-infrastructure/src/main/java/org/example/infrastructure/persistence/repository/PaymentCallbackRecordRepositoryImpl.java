package org.example.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.model.payment.PaymentCallbackRecord;
import org.example.domain.model.payment.repository.PaymentCallbackRecordRepository;
import org.example.infrastructure.persistence.converter.PaymentCallbackRecordConverter;
import org.example.infrastructure.persistence.mapper.PaymentCallbackRecordMapper;
import org.example.infrastructure.persistence.po.PaymentCallbackRecordPO;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 支付回调记录仓储实现
 *
 * @author 开发团队
 * @since 2026-01-07
 */
@Slf4j
@Repository
public class PaymentCallbackRecordRepositoryImpl implements PaymentCallbackRecordRepository {

    private final PaymentCallbackRecordMapper mapper;
    private final PaymentCallbackRecordConverter converter;

    public PaymentCallbackRecordRepositoryImpl(PaymentCallbackRecordMapper mapper,
            PaymentCallbackRecordConverter converter) {
        this.mapper = mapper;
        this.converter = converter;
    }

    @Override
    public void save(PaymentCallbackRecord record) {
        PaymentCallbackRecordPO po = converter.toPO(record);
        mapper.insert(po);
        log.debug("【PaymentCallbackRecordRepository】保存支付回调记录, recordId: {}, callbackId: {}",
                record.getRecordId(), record.getCallbackId());
    }

    @Override
    public boolean existsByCallbackId(String callbackId) {
        LambdaQueryWrapper<PaymentCallbackRecordPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentCallbackRecordPO::getCallbackId, callbackId);
        Long count = mapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    @Override
    public Optional<PaymentCallbackRecord> findByCallbackId(String callbackId) {
        LambdaQueryWrapper<PaymentCallbackRecordPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentCallbackRecordPO::getCallbackId, callbackId);
        PaymentCallbackRecordPO po = mapper.selectOne(wrapper);
        return Optional.ofNullable(po).map(converter::toDomain);
    }
}
