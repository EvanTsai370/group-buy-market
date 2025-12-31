// ============ 文件: DiscountConverter.java ============
package org.example.infrastructure.persistence.converter;

import org.example.domain.model.activity.Discount;
import org.example.infrastructure.persistence.po.DiscountPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * Discount 转换器
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
     */
    DiscountPO toPO(Discount discount);
}