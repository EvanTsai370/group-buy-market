package org.example.infrastructure.persistence.converter;

import org.example.domain.model.user.UserOAuth;
import org.example.domain.model.user.valueobject.OAuthProvider;
import org.example.infrastructure.persistence.po.UserOAuthPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

/**
 * OAuth绑定转换器
 */
@Mapper(componentModel = "spring")
public interface UserOAuthConverter {

    @Mapping(target = "provider", source = "provider", qualifiedByName = "stringToProvider")
    UserOAuth toDomain(UserOAuthPO po);

    List<UserOAuth> toDomainList(List<UserOAuthPO> poList);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "provider", source = "provider", qualifiedByName = "providerToString")
    UserOAuthPO toPO(UserOAuth userOAuth);

    @Named("stringToProvider")
    default OAuthProvider stringToProvider(String provider) {
        if (provider == null) {
            return null;
        }
        return OAuthProvider.valueOf(provider);
    }

    @Named("providerToString")
    default String providerToString(OAuthProvider provider) {
        if (provider == null) {
            return null;
        }
        return provider.name();
    }
}
