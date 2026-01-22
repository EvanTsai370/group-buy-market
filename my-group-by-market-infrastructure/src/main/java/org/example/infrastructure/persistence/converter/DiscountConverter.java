package org.example.infrastructure.persistence.converter;

import org.example.domain.model.activity.Discount;
import org.example.domain.model.activity.valueobject.DiscountType;
import org.example.infrastructure.persistence.po.DiscountPO;
import org.mapstruct.Mapper;

/**
 * Discount 转换器
 * 处理 DiscountType 枚举与 String 之间的转换
 */
@Mapper(componentModel = "spring")
public interface DiscountConverter {

    /**
     * PO 转 Domain Entity
     */
    Discount toDomain(DiscountPO po);

    /**
     * Domain Entity 转 PO
     */
    DiscountPO toPO(Discount discount);

    /**
     * DiscountType 枚举 转 String
     */
    default String discountTypeToString(DiscountType discountType) {
        return discountType == null ? null : discountType.getCode();
    }

    /**
     * String 转 DiscountType 枚举
     */
    default DiscountType stringToDiscountType(String code) {
        return code == null ? null : DiscountType.fromCode(code);
    }
}