package org.example.interfaces.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.application.dto.cmd.UserRegisterCmd;
import org.example.application.dto.view.UserVO;
import org.example.application.service.UserApplicationService;
import org.example.common.api.Result;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor // Lombok: 自动注入构造函数
public class UserController {

    private final UserApplicationService userAppService;

    @Operation(summary = "注册用户")
    @PostMapping("/register")
    public Result<UserVO> register(@RequestBody @Validated UserRegisterCmd cmd) {
        // 1. 接收 application/dto/cmd 下的参数对象
        // 2. 调用应用层服务
        UserVO userVO = userAppService.registerUser(cmd);
        return Result.success(userVO);
    }
}