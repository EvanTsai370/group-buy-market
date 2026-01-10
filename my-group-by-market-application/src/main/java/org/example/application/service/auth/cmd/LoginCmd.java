package org.example.application.service.auth.cmd;

import lombok.Data;

/**
 * 登录命令
 */
@Data
public class LoginCmd {
    private String username;
    private String password;
}
