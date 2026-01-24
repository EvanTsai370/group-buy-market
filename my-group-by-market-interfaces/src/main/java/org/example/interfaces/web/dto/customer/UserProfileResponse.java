package org.example.interfaces.web.dto.customer;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户资料响应
 * 
 */
@Data
public class UserProfileResponse {

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
