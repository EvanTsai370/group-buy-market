package org.example.domain.model.user;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.model.user.valueobject.OAuthProvider;

import java.time.LocalDateTime;

/**
 * 第三方登录绑定实体
 * 
 * <p>
 * 关联到 User 聚合根，记录用户的第三方登录信息
 * 
 */
@Slf4j
@Data
public class UserOAuth {

    /** 用户ID */
    private String userId;

    /** OAuth 提供商 */
    private OAuthProvider provider;

    /** 第三方用户ID（openid） */
    private String providerUserId;

    /** 跨应用统一ID（微信的unionId） */
    private String unionId;

    /** 访问令牌 */
    private String accessToken;

    /** 刷新令牌 */
    private String refreshToken;

    /** 令牌过期时间 */
    private LocalDateTime tokenExpireTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /**
     * 创建 OAuth 绑定（工厂方法）
     * 
     * @param userId         用户ID
     * @param provider       OAuth提供商
     * @param providerUserId 第三方用户ID
     * @param accessToken    访问令牌
     * @param refreshToken   刷新令牌
     * @param expiresIn      过期时间（秒）
     */
    public static UserOAuth create(
            String userId,
            OAuthProvider provider,
            String providerUserId,
            String accessToken,
            String refreshToken,
            Long expiresIn) {

        UserOAuth oauth = new UserOAuth();
        oauth.userId = userId;
        oauth.provider = provider;
        oauth.providerUserId = providerUserId;
        oauth.accessToken = accessToken;
        oauth.refreshToken = refreshToken;

        if (expiresIn != null && expiresIn > 0) {
            oauth.tokenExpireTime = LocalDateTime.now().plusSeconds(expiresIn);
        }

        oauth.createTime = LocalDateTime.now();
        oauth.updateTime = LocalDateTime.now();

        log.info("【UserOAuth】创建OAuth绑定, userId: {}, provider: {}, providerUserId: {}",
                userId, provider, providerUserId);
        return oauth;
    }

    /**
     * 更新令牌
     */
    public void updateToken(String accessToken, String refreshToken, Long expiresIn) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;

        if (expiresIn != null && expiresIn > 0) {
            this.tokenExpireTime = LocalDateTime.now().plusSeconds(expiresIn);
        }

        this.updateTime = LocalDateTime.now();

        log.info("【UserOAuth】更新令牌, userId: {}, provider: {}", userId, provider);
    }

    /**
     * 判断令牌是否过期
     */
    public boolean isTokenExpired() {
        if (tokenExpireTime == null) {
            return false; // 没有设置过期时间，认为不过期
        }
        return LocalDateTime.now().isAfter(tokenExpireTime);
    }

    /**
     * 设置 unionId
     */
    public void setUnionId(String unionId) {
        this.unionId = unionId;
        this.updateTime = LocalDateTime.now();
    }
}
