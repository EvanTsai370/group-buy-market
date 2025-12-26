package org.example.infrastructure.persistence.converter;

import org.example.domain.model.user.User;
import org.example.infrastructure.persistence.po.UserPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserDataConverter {

    // 1. Entity -> PO (入库)
    @Mapping(target = "pwd", source = "password") // 字段映射
    @Mapping(target = "username", source = "name")
    @Mapping(target = "city", source = "address.city") // 对象 -> 扁平
    @Mapping(target = "street", source = "address.street")
    UserPO toPO(User user);

    // 2. PO -> Entity (出库)
    @Mapping(target = "password", source = "pwd")
    @Mapping(target = "name", source = "username")
    @Mapping(target = "address.city", source = "city")
    @Mapping(target = "address.street", source = "street")
    User toEntity(UserPO userPO);
}