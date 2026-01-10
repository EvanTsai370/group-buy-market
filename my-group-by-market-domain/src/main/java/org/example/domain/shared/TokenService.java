package org.example.domain.shared;

/**
 * Token 服务接口
 * 
 * <p>
 * 用于生成和验证 JWT 令牌，定义在 Domain 层以满足依赖倒置原则
 * </p>
 * 
 * @author 开发团队
 * @since 2026-01-10
 */
public interface TokenService {

    /**
     * 生成访问令牌
     * 
     * @param userId   用户ID
     * @param username 用户名
     * @param role     角色
     * @return 访问令牌
     */
    String generateToken(String userId, String username, String role);

    /**
     * 生成刷新令牌
     * 
     * @param userId 用户ID
     * @return 刷新令牌
     */
    String generateRefreshToken(String userId);

    /**
     * 从令牌中获取用户ID
     */
    String getUserIdFromToken(String token);

    /**
     * 从令牌中获取用户名
     */
    String getUsernameFromToken(String token);

    /**
     * 从令牌中获取角色
     */
    String getRoleFromToken(String token);

    /**
     * 验证令牌是否有效
     */
    boolean validateToken(String token);

    /**
     * 判断是否是刷新令牌
     */
    boolean isRefreshToken(String token);
}
