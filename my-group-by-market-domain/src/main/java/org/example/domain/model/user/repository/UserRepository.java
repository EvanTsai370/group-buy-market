package org.example.domain.model.user.repository;

import org.example.domain.model.user.User;

import java.util.Optional;

/**
 * 用户仓储接口
 * 
 * @author 开发团队
 * @since 2026-01-10
 */
public interface UserRepository {

    /**
     * 保存用户
     */
    void save(User user);

    /**
     * 更新用户
     */
    void update(User user);

    /**
     * 根据用户ID查询
     */
    Optional<User> findByUserId(String userId);

    /**
     * 根据用户名查询
     */
    Optional<User> findByUsername(String username);

    /**
     * 根据手机号查询
     */
    Optional<User> findByPhone(String phone);

    /**
     * 根据邮箱查询
     */
    Optional<User> findByEmail(String email);

    /**
     * 判断用户名是否已存在
     */
    boolean existsByUsername(String username);

    /**
     * 判断手机号是否已存在
     */
    boolean existsByPhone(String phone);

    /**
     * 判断邮箱是否已存在
     */
    boolean existsByEmail(String email);
}
