package org.example.infrastructure.persistence.converter;

import org.example.domain.model.activity.Discount;
import org.example.domain.model.activity.valueobject.DiscountType;
import org.example.infrastructure.persistence.po.DiscountPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * Discount 转换器
 * 处理 DiscountType 枚举与 String 之间的转换
 */
@Mapper
public interface DiscountConverter {

    DiscountConverter INSTANCE = Mappers.getMapper(DiscountConverter.class);

    /**
     * PO 转 Domain Entity
     */
    Discount toDomain(DiscountPO po);

    /**
     * Domain Entity 转 PO
     * 忽略 id 字段（由数据库自动生成）
     */
    @Mapping(target = "id", ignore = true)
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