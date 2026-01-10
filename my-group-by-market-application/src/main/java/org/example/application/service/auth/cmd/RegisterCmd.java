package org.example.application.service.auth.cmd;

import lombok.Data;

/**
 * 注册命令
 */
@Data
public class RegisterCmd {
    private String username;
    private String password;
    private String nickname;
    private String phone;
}
