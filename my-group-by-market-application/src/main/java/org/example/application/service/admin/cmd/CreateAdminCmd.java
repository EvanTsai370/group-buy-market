package org.example.application.service.admin.cmd;

import lombok.Data;

/**
 * 创建管理员命令
 * 
 * @author 开发团队
 * @since 2026-01-12
 */
@Data
public class CreateAdminCmd {

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
