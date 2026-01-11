package org.example.application.service.customer.result;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户资料结果
 * 
 * @author 开发团队
 * @since 2026-01-11
 */
@Data
public class UserProfileResult {

    /** 用户ID */
    private String userId;

    /** 用户名 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 头像URL */
    private String avatar;

    /** 手机号（脱敏） */
    private String phone;

    /** 邮箱（脱敏） */
    private String email;

    /** 最后登录时间 */
    private LocalDateTime lastLoginTime;

    /** 创建时间 */
    private LocalDateTime createTime;
}
