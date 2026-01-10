package org.example.infrastructure.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.shared.TokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

/**
 * JWT 令牌服务
 * 
 * <p>
 * 负责 JWT Token 的生成、解析和验证
 * </p>
 * 
 * @author 开发团队
 * @since 2026-01-10
 */
@Slf4j
@Service
public class JwtTokenService implements TokenService {

    @Value("${jwt.secret:myGroupBuyMarketSecretKeyForJwtTokenGenerationMustBe32BytesLong}")
    private String secret;

    @Value("${jwt.expiration:86400000}") // 默认24小时
    private long expiration;

    @Value("${jwt.refresh-expiration:604800000}") // 默认7天
    private long refreshExpiration;

    /**
     * 生成访问令牌
     */
    public String generateToken(String userId, String username, String role) {
        return Jwts.builder()
                .subject(userId)
                .claims(Map.of(
                        "username", username != null ? username : "",
                        "role", role != null ? role : "USER"))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 生成刷新令牌
     */
    public String generateRefreshToken(String userId) {
        return Jwts.builder()
                .subject(userId)
                .claims(Map.of("type", "refresh"))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 从令牌中获取用户ID
     */
    public String getUserIdFromToken(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * 从令牌中获取用户名
     */
    public String getUsernameFromToken(String token) {
        return getClaims(token).get("username", String.class);
    }

    /**
     * 从令牌中获取角色
     */
    public String getRoleFromToken(String token) {
        return getClaims(token).get("role", String.class);
    }

    /**
     * 验证令牌是否有效
     */
    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("【JWT】令牌已过期: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("【JWT】令牌格式错误: {}", e.getMessage());
        } catch (SecurityException e) {
            log.warn("【JWT】签名验证失败: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("【JWT】令牌验证失败: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 判断令牌是否过期
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = getClaims(token).getExpiration();
            return expiration.before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    /**
     * 判断是否是刷新令牌
     */
    public boolean isRefreshToken(String token) {
        try {
            String type = getClaims(token).get("type", String.class);
            return "refresh".equals(type);
        } catch (Exception e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        // 确保密钥至少32字节（256位）
        String paddedSecret = secret;
        while (paddedSecret.length() < 32) {
            paddedSecret = paddedSecret + paddedSecret;
        }
        paddedSecret = paddedSecret.substring(0, 64);
        return Keys.hmacShaKeyFor(paddedSecret.getBytes());
    }
}
