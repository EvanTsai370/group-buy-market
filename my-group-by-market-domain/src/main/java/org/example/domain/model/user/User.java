package org.example.domain.model.user;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.example.common.exception.BizException;
import org.example.domain.model.user.valueobject.UserRole;
import org.example.domain.model.user.valueobject.UserStatus;

import java.time.LocalDateTime;

/**
 * User 聚合根（用户）
 * 
 * <p>
 * 职责：
 * <ul>
 * <li>管理用户身份和认证信息</li>
 * <li>管理用户角色和权限</li>
 * <li>维护用户状态生命周期</li>
 * </ul>
 * 
 * <p>
 * 与 account 表的关系：
 * <ul>
 * <li>user 表管理用户身份认证</li>
 * <li>account 表管理用户在特定活动中的参团次数</li>
 * <li>两者通过 userId 关联</li>
 * </ul>
 * 
 * @author 开发团队
 * @since 2026-01-10
 */
@Slf4j
@Data
public class User {

    /** 用户ID（业务主键） */
    private String userId;

    /** 用户名 */
    private String username;

    /** 密码（BCrypt加密） */
    private String password;

    /** 昵称 */
    private String nickname;

    /** 头像URL */
    private String avatar;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 用户状态 */
    private UserStatus status;

    /** 用户角色 */
    private UserRole role;

    /** 最后登录时间 */
    private LocalDateTime lastLoginTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /**
     * 创建普通用户（工厂方法）
     * 
     * @param userId   用户ID
     * @param username 用户名
     * @param password 加密后的密码
     * @param nickname 昵称
     */
    public static User createUser(String userId, String username, String password, String nickname) {
        if (userId == null || userId.isEmpty()) {
            throw new BizException("用户ID不能为空");
        }

        User user = new User();
        user.userId = userId;
        user.username = username;
        user.password = password;
        user.nickname = nickname;
        user.status = UserStatus.ACTIVE;
        user.role = UserRole.USER;
        user.createTime = LocalDateTime.now();
        user.updateTime = LocalDateTime.now();

        log.info("【User聚合】创建用户, userId: {}, username: {}", userId, username);
        return user;
    }

    /**
     * 通过OAuth创建用户（工厂方法）
     * 
     * @param userId   用户ID
     * @param nickname 昵称（从第三方获取）
     * @param avatar   头像（从第三方获取）
     */
    public static User createFromOAuth(String userId, String nickname, String avatar) {
        if (userId == null || userId.isEmpty()) {
            throw new BizException("用户ID不能为空");
        }

        User user = new User();
        user.userId = userId;
        user.nickname = nickname;
        user.avatar = avatar;
        user.status = UserStatus.ACTIVE;
        user.role = UserRole.USER;
        user.createTime = LocalDateTime.now();
        user.updateTime = LocalDateTime.now();

        log.info("【User聚合】通过OAuth创建用户, userId: {}, nickname: {}", userId, nickname);
        return user;
    }

    /**
     * 创建管理员（工厂方法）
     */
    public static User createAdmin(String userId, String username, String password) {
        User user = createUser(userId, username, password, "管理员");
        user.role = UserRole.ADMIN;

        log.info("【User聚合】创建管理员, userId: {}, username: {}", userId, username);
        return user;
    }

    /**
     * 记录登录
     */
    public void recordLogin() {
        if (this.status != UserStatus.ACTIVE) {
            throw new BizException("用户状态异常，无法登录");
        }

        this.lastLoginTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();

        log.info("【User聚合】用户登录, userId: {}", userId);
    }

    /**
     * 禁用用户
     */
    public void disable() {
        if (this.status == UserStatus.DISABLED) {
            return; // 幂等
        }

        this.status = UserStatus.DISABLED;
        this.updateTime = LocalDateTime.now();

        log.info("【User聚合】用户已禁用, userId: {}", userId);
    }

    /**
     * 启用用户
     */
    public void enable() {
        if (this.status == UserStatus.ACTIVE) {
            return; // 幂等
        }

        this.status = UserStatus.ACTIVE;
        this.updateTime = LocalDateTime.now();

        log.info("【User聚合】用户已启用, userId: {}", userId);
    }

    /**
     * 锁定用户（连续登录失败等场景）
     */
    public void lock() {
        this.status = UserStatus.LOCKED;
        this.updateTime = LocalDateTime.now();

        log.warn("【User聚合】用户已锁定, userId: {}", userId);
    }

    /**
     * 更新密码
     * 
     * @param newPassword 新密码（已加密）
     */
    public void updatePassword(String newPassword) {
        if (newPassword == null || newPassword.isEmpty()) {
            throw new BizException("新密码不能为空");
        }

        this.password = newPassword;
        this.updateTime = LocalDateTime.now();

        log.info("【User聚合】密码已更新, userId: {}", userId);
    }

    /**
     * 绑定手机号
     */
    public void bindPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            throw new BizException("手机号不能为空");
        }

        this.phone = phone;
        this.updateTime = LocalDateTime.now();

        log.info("【User聚合】手机号已绑定, userId: {}", userId);
    }

    /**
     * 判断是否是管理员
     */
    public boolean isAdmin() {
        return this.role != null && this.role.isAdmin();
    }

    /**
     * 判断用户是否可用
     */
    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }
}
