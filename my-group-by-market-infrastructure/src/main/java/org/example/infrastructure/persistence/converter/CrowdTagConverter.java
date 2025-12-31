// ============ 文件: CrowdTagConverter.java ============
package org.example.infrastructure.persistence.converter;

import org.example.domain.model.tag.CrowdTag;
import org.example.domain.model.tag.valueobject.TagStatus;
import org.example.infrastructure.persistence.po.CrowdTagPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

/**
 * CrowdTag 转换器
 */
@Mapper
public interface CrowdTagConverter {

    CrowdTagConverter INSTANCE = Mappers.getMapper(CrowdTagConverter.class);

    /**
     * PO 转 Domain Entity
     */
    @Mapping(source = "status", target = "status", qualifiedByName = "stringToTagStatus")
    CrowdTag toDomain(CrowdTagPO po);

    /**
     * Domain Entity 转 PO
     */
    @Mapping(source = "status", target = "status", qualifiedByName = "tagStatusToString")
    CrowdTagPO toPO(CrowdTag tag);

    /**
     * String 转 TagStatus
     */
    @Named("stringToTagStatus")
    default TagStatus stringToTagStatus(String status) {
        return status == null ? null : TagStatus.valueOf(status);
    }

    /**
     * TagStatus 转 String
     */
    @Named("tagStatusToString")
    default String tagStatusToString(TagStatus status) {
        return status == null ? null : status.name();
    }
}