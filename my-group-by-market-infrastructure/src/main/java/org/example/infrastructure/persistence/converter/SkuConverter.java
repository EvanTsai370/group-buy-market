package org.example.infrastructure.persistence.converter;

import org.example.domain.model.goods.Sku;
import org.example.domain.model.goods.valueobject.GoodsStatus;
import org.example.infrastructure.persistence.po.SkuPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

/**
 * SKU 转换器
 */
@Mapper(componentModel = "spring")
public interface SkuConverter {

    @Mapping(target = "status", source = "status", qualifiedByName = "stringToStatus")
    Sku toDomain(SkuPO po);

    List<Sku> toDomainList(List<SkuPO> poList);

    @Mapping(target = "status", source = "status", qualifiedByName = "statusToString")
    SkuPO toPO(Sku sku);

    @Named("stringToStatus")
    default GoodsStatus stringToStatus(String status) {
        if (status == null) {
            return GoodsStatus.ON_SALE;
        }
        return GoodsStatus.valueOf(status);
    }

    @Named("statusToString")
    default String statusToString(GoodsStatus status) {
        if (status == null) {
            return GoodsStatus.ON_SALE.name();
        }
        return status.name();
    }
}