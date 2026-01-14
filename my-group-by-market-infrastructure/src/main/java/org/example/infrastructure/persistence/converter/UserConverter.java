package org.example.infrastructure.persistence.converter;

import org.example.domain.model.user.User;
import org.example.domain.model.user.valueobject.UserRole;
import org.example.domain.model.user.valueobject.UserStatus;
import org.example.infrastructure.persistence.po.UserPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * 用户转换器
 */
@Mapper(componentModel = "spring")
public interface UserConverter {

    @Mapping(target = "status", source = "status", qualifiedByName = "stringToUserStatus")
    @Mapping(target = "role", source = "role", qualifiedByName = "stringToUserRole")
    User toDomain(UserPO po);

    @Mapping(target = "status", source = "status", qualifiedByName = "userStatusToString")
    @Mapping(target = "role", source = "role", qualifiedByName = "userRoleToString")
    UserPO toPO(User user);

    @Named("stringToUserStatus")
    default UserStatus stringToUserStatus(String status) {
        if (status == null) {
            return UserStatus.ACTIVE;
        }
        return UserStatus.valueOf(status);
    }

    @Named("userStatusToString")
    default String userStatusToString(UserStatus status) {
        if (status == null) {
            return UserStatus.ACTIVE.name();
        }
        return status.name();
    }

    @Named("stringToUserRole")
    default UserRole stringToUserRole(String role) {
        if (role == null) {
            return UserRole.USER;
        }
        return UserRole.valueOf(role);
    }

    @Named("userRoleToString")
    default String userRoleToString(UserRole role) {
        if (role == null) {
            return UserRole.USER.name();
        }
        return role.name();
    }
}
