// ============ 文件: SkuConverter.java ============
package org.example.infrastructure.persistence.converter;

import org.example.domain.model.goods.Sku;
import org.example.infrastructure.persistence.po.SkuPO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * SKU 转换器
 */
@Mapper
public interface SkuConverter {

    SkuConverter INSTANCE = Mappers.getMapper(SkuConverter.class);

    /**
     * PO 转 Domain Entity
     */
    Sku toDomain(SkuPO po);

    /**
     * Domain Entity 转 PO
     */
    SkuPO toPO(Sku sku);
}