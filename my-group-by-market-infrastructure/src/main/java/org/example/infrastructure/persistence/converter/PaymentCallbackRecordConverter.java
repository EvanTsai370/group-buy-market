package org.example.infrastructure.persistence.converter;

import org.example.domain.model.payment.PaymentCallbackRecord;
import org.example.infrastructure.persistence.po.PaymentCallbackRecordPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 支付回调记录转换器
 *
 * @author 开发团队
 * @since 2026-01-07
 */
@Mapper(componentModel = "spring")
public interface PaymentCallbackRecordConverter {

    /**
     * PO转Domain
     */
    PaymentCallbackRecord toDomain(PaymentCallbackRecordPO po);

    /**
     * Domain转PO
     */
    @Mapping(target = "id", ignore = true)
    PaymentCallbackRecordPO toPO(PaymentCallbackRecord domain);
}
