package org.example.domain.shared;

import java.util.Optional;

/**
 * 认证上下文服务接口
 * 
 * <p>
 * 定义在 Domain 层，由 Infrastructure 层实现
 * 遵循依赖倒置原则，隔离框架依赖
 * </p>
 * 
 * @author 开发团队
 * @since 2026-01-12
 */
public interface AuthContextService {

    /**
     * 获取当前用户ID
     * 
     * @return 当前用户ID
     * @throws org.example.common.exception.BizException 如果未认证
     */
    String getCurrentUserId();

    /**
     * 获取当前用户ID（可选）
     * 
     * @return 当前用户ID，未认证时返回 empty
     */
    Optional<String> getCurrentUserIdOptional();

    /**
     * 获取当前用户名
     * 
     * @return 当前用户名
     * @throws org.example.common.exception.BizException 如果未认证
     */
    String getCurrentUsername();

    /**
     * 获取当前用户角色
     * 
     * @return 当前用户角色
     * @throws org.example.common.exception.BizException 如果未认证
     */
    String getCurrentUserRole();

    /**
     * 判断是否已认证
     * 
     * @return 是否已认证
     */
    boolean isAuthenticated();

    /**
     * 判断当前用户是否是管理员
     * 
     * @return 是否是管理员
     */
    boolean isAdmin();
}
