package org.example.interfaces.web.dto.admin;

import lombok.Data;

/**
 * 创建管理员请求
 * 
 * @author 开发团队
 * @since 2026-01-12
 */
@Data
public class CreateAdminRequest {

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 昵称（可选）
     */
    private String nickname;
}
