// ============ 文件: ActivityConverter.java ============
package org.example.infrastructure.persistence.converter;

import org.example.domain.model.activity.Activity;
import org.example.domain.model.activity.valueobject.ActivityStatus;
import org.example.domain.model.activity.valueobject.GroupType;
import org.example.infrastructure.persistence.po.ActivityPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

/**
 * Activity 转换器（Domain Entity ↔ PO）
 * 使用 MapStruct 自动生成转换代码
 */
@Mapper
public interface ActivityConverter {

    ActivityConverter INSTANCE = Mappers.getMapper(ActivityConverter.class);

    /**
     * PO 转 Domain Entity
     */
    @Mapping(source = "groupType", target = "groupType", qualifiedByName = "intToGroupType")
    @Mapping(source = "status", target = "status", qualifiedByName = "stringToActivityStatus")
    Activity toDomain(ActivityPO po);

    /**
     * Domain Entity 转 PO
     */
    @Mapping(source = "groupType", target = "groupType", qualifiedByName = "groupTypeToInt")
    @Mapping(source = "status", target = "status", qualifiedByName = "activityStatusToString")
    ActivityPO toPO(Activity activity);

    /**
     * Integer 转 GroupType
     */
    @Named("intToGroupType")
    default GroupType intToGroupType(Integer code) {
        return code == null ? null : GroupType.fromCode(code);
    }

    /**
     * GroupType 转 Integer
     */
    @Named("groupTypeToInt")
    default Integer groupTypeToInt(GroupType groupType) {
        return groupType == null ? null : groupType.getCode();
    }

    /**
     * String 转 ActivityStatus
     */
    @Named("stringToActivityStatus")
    default ActivityStatus stringToActivityStatus(String status) {
        return status == null ? null : ActivityStatus.valueOf(status);
    }

    /**
     * ActivityStatus 转 String
     */
    @Named("activityStatusToString")
    default String activityStatusToString(ActivityStatus status) {
        return status == null ? null : status.name();
    }
}