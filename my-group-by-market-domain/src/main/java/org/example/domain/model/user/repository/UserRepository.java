package org.example.domain.model.user.repository;

import org.example.domain.model.user.User;

import java.util.List;
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

    /**
     * 统计用户数量
     */
    long count();

    /**
     * 分页查询用户
     */
    List<User> findAll(int page, int size);

    /**
     * 统计指定时间范围内的注册用户数量
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 用户数量
     */
    long countByCreateTimeBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);

    /**
     * 分页查询用户
     * 
     * @param page 页码
     * @param size 每页大小
     * @return 分页结果
     */
    org.example.common.model.PageResult<User> findByPage(int page, int size);
}
