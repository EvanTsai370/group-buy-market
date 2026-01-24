package org.example.domain.model.user.repository;

import org.example.domain.model.user.UserOAuth;
import org.example.domain.model.user.valueobject.OAuthProvider;

import java.util.List;
import java.util.Optional;

/**
 * OAuth绑定仓储接口
 * 
 */
public interface UserOAuthRepository {

    /**
     * 保存OAuth绑定
     */
    void save(UserOAuth userOAuth);

    /**
     * 更新OAuth绑定
     */
    void update(UserOAuth userOAuth);

    /**
     * 根据提供商和第三方用户ID查询
     */
    Optional<UserOAuth> findByProviderAndProviderUserId(OAuthProvider provider, String providerUserId);

    /**
     * 根据用户ID查询所有OAuth绑定
     */
    List<UserOAuth> findByUserId(String userId);

    /**
     * 根据用户ID和提供商查询
     */
    Optional<UserOAuth> findByUserIdAndProvider(String userId, OAuthProvider provider);

    /**
     * 判断绑定是否存在
     */
    boolean existsByProviderAndProviderUserId(OAuthProvider provider, String providerUserId);

    /**
     * 删除OAuth绑定
     */
    void deleteByUserIdAndProvider(String userId, OAuthProvider provider);
}
