package org.example.infrastructure.persistence.converter;

import org.example.domain.model.goods.Spu;
import org.example.domain.model.goods.valueobject.GoodsStatus;
import org.example.infrastructure.persistence.po.SpuPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

/**
 * SPU 转换器
 */
@Mapper(componentModel = "spring")
public interface SpuConverter {

    @Mapping(target = "status", source = "status", qualifiedByName = "stringToStatus")
    Spu toDomain(SpuPO po);

    List<Spu> toDomainList(List<SpuPO> poList);

    @Mapping(target = "status", source = "status", qualifiedByName = "statusToString")
    SpuPO toPO(Spu spu);

    @Named("stringToStatus")
    default GoodsStatus stringToStatus(String status) {
        if (status == null) {
            return GoodsStatus.OFF_SALE;
        }
        return GoodsStatus.valueOf(status);
    }

    @Named("statusToString")
    default String statusToString(GoodsStatus status) {
        if (status == null) {
            return GoodsStatus.OFF_SALE.name();
        }
        return status.name();
    }
}
