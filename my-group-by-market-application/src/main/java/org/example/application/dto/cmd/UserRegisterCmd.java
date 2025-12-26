package org.example.application.dto.cmd;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户注册指令 (Command)
 * 职责：接收前端参数，并进行基础校验
 */
@Data
public class UserRegisterCmd {

    // 使用 {key} 语法，大括号是必须的！
    @NotBlank(message = "{error.param.username.blank}")
    @Size(min = 4, max = 20, message = "{error.param.username.length}")
    private String username;

    @NotBlank(message = "{error.param.password.blank}")
    @Size(min = 6, message = "{error.param.password.length}")
    private String password;

    @NotBlank(message = "{error.param.email.blank}")
    @Email(message = "{error.param.email.invalid}")
    private String email;

    // 假设前端直接传扁平的地址信息
    private String city;
    private String street;
}