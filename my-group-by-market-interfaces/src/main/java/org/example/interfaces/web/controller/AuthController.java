package org.example.interfaces.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.application.service.auth.AuthService;
import org.example.application.service.auth.cmd.LoginCmd;
import org.example.application.service.auth.cmd.RegisterCmd;
import org.example.application.service.auth.result.AuthResult;
import org.example.common.api.Result;
import org.example.interfaces.web.request.LoginRequest;
import org.example.interfaces.web.request.RefreshTokenRequest;
import org.example.interfaces.web.request.RegisterRequest;
import org.example.interfaces.web.dto.AuthResponse;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 * 
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "用户认证", description = "登录、注册、刷新令牌")
public class AuthController {

    private final AuthService authService;

    /**
     * 用户名密码登录
     */
    @PostMapping("/login")
    @Operation(summary = "登录", description = "用户名密码登录")
    public Result<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("【AuthController】登录请求, username: {}", request.getUsername());

        LoginCmd cmd = new LoginCmd();
        cmd.setUsername(request.getUsername());
        cmd.setPassword(request.getPassword());

        AuthResult result = authService.login(cmd);

        return Result.success(toResponse(result));
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    @Operation(summary = "注册", description = "用户名密码注册")
    public Result<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("【AuthController】注册请求, username: {}", request.getUsername());

        RegisterCmd cmd = new RegisterCmd();
        cmd.setUsername(request.getUsername());
        cmd.setPassword(request.getPassword());
        cmd.setNickname(request.getNickname());
        cmd.setPhone(request.getPhone());

        AuthResult result = authService.register(cmd);

        return Result.success(toResponse(result));
    }

    /**
     * 刷新令牌
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新令牌", description = "使用刷新令牌获取新的访问令牌")
    public Result<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("【AuthController】刷新令牌请求");

        AuthResult result = authService.refreshToken(request.getRefreshToken());

        return Result.success(toResponse(result));
    }

    private AuthResponse toResponse(AuthResult result) {
        AuthResponse response = new AuthResponse();
        response.setUserId(result.getUserId());
        response.setUsername(result.getUsername());
        response.setNickname(result.getNickname());
        response.setRole(result.getRole());
        response.setAccessToken(result.getAccessToken());
        response.setRefreshToken(result.getRefreshToken());
        return response;
    }
}
