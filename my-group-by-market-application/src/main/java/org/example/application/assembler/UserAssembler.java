package org.example.application.assembler;

import org.example.application.dto.cmd.UserRegisterCmd;
import org.example.application.dto.view.UserVO;
import org.example.domain.model.user.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring") // 让Spring管理Bean
public interface UserAssembler {

    // 1. Cmd -> Entity
    @Mapping(target = "id", ignore = true) // 注册时还没ID
    @Mapping(target = "name", source = "username") // 字段名不同：username -> name
    @Mapping(target = "address.city", source = "city") // 扁平 -> 对象
    @Mapping(target = "address.street", source = "street")
    User toEntity(UserRegisterCmd cmd);

    // 2. Entity -> VO (返回给前端)
    @Mapping(target = "username", source = "name")
    @Mapping(target = "city", source = "address.city") // 扁平 -> 对象
    @Mapping(target = "street", source = "address.street")
    UserVO toVO(User user);
}