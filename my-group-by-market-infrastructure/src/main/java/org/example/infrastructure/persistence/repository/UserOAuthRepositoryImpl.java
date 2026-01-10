package org.example.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import org.example.domain.model.user.UserOAuth;
import org.example.domain.model.user.repository.UserOAuthRepository;
import org.example.domain.model.user.valueobject.OAuthProvider;
import org.example.infrastructure.persistence.converter.UserOAuthConverter;
import org.example.infrastructure.persistence.mapper.UserOAuthMapper;
import org.example.infrastructure.persistence.po.UserOAuthPO;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * OAuth绑定仓储实现
 */
@Repository
@RequiredArgsConstructor
public class UserOAuthRepositoryImpl implements UserOAuthRepository {

    private final UserOAuthMapper userOAuthMapper;
    private final UserOAuthConverter userOAuthConverter;

    @Override
    public void save(UserOAuth userOAuth) {
        UserOAuthPO po = userOAuthConverter.toPO(userOAuth);
        userOAuthMapper.insert(po);
    }

    @Override
    public void update(UserOAuth userOAuth) {
        UserOAuthPO existing = userOAuthMapper.selectByProviderAndProviderUserId(
                userOAuth.getProvider().name(),
                userOAuth.getProviderUserId());
        if (existing != null) {
            UserOAuthPO po = userOAuthConverter.toPO(userOAuth);
            po.setId(existing.getId());
            userOAuthMapper.updateById(po);
        }
    }

    @Override
    public Optional<UserOAuth> findByProviderAndProviderUserId(OAuthProvider provider, String providerUserId) {
        UserOAuthPO po = userOAuthMapper.selectByProviderAndProviderUserId(provider.name(), providerUserId);
        return Optional.ofNullable(po).map(userOAuthConverter::toDomain);
    }

    @Override
    public List<UserOAuth> findByUserId(String userId) {
        List<UserOAuthPO> poList = userOAuthMapper.selectByUserId(userId);
        return userOAuthConverter.toDomainList(poList);
    }

    @Override
    public Optional<UserOAuth> findByUserIdAndProvider(String userId, OAuthProvider provider) {
        UserOAuthPO po = userOAuthMapper.selectByUserIdAndProvider(userId, provider.name());
        return Optional.ofNullable(po).map(userOAuthConverter::toDomain);
    }

    @Override
    public boolean existsByProviderAndProviderUserId(OAuthProvider provider, String providerUserId) {
        return userOAuthMapper.existsByProviderAndProviderUserId(provider.name(), providerUserId);
    }

    @Override
    public void deleteByUserIdAndProvider(String userId, OAuthProvider provider) {
        userOAuthMapper.deleteByUserIdAndProvider(userId, provider.name());
    }
}
