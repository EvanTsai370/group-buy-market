package org.example.application.service.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.application.service.auth.cmd.LoginCmd;
import org.example.application.service.auth.cmd.RegisterCmd;
import org.example.application.service.auth.result.AuthResult;
import org.example.common.exception.BizException;
import org.example.domain.model.user.User;
import org.example.domain.model.user.repository.UserRepository;
import org.example.domain.shared.IdGenerator;
import org.example.domain.shared.PasswordEncoderService;
import org.example.domain.shared.TokenService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 认证服务
 * 
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoderService passwordEncoderService;
    private final TokenService tokenService;
    private final IdGenerator idGenerator;

    /**
     * 用户名密码登录
     */
    @Transactional
    public AuthResult login(LoginCmd cmd) {
        log.info("【AuthService】用户登录, username: {}", cmd.getUsername());

        // 1. 查找用户
        User user = userRepository.findByUsername(cmd.getUsername())
                .orElseThrow(() -> new BizException("用户名或密码错误"));

        // 2. 验证密码
        if (!passwordEncoderService.matches(cmd.getPassword(), user.getPassword())) {
            log.warn("【AuthService】密码错误, username: {}", cmd.getUsername());
            throw new BizException("用户名或密码错误");
        }

        // 3. 检查用户状态
        if (!user.isActive()) {
            log.warn("【AuthService】用户状态异常, userId: {}, status: {}",
                    user.getUserId(), user.getStatus());
            throw new BizException("用户已被禁用或锁定");
        }

        // 4. 记录登录并更新
        user.recordLogin();
        userRepository.update(user);

        // 5. 生成令牌
        String accessToken = tokenService.generateToken(
                user.getUserId(),
                user.getUsername(),
                user.getRole().name());
        String refreshToken = tokenService.generateRefreshToken(user.getUserId());

        log.info("【AuthService】登录成功, userId: {}", user.getUserId());

        return AuthResult.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .role(user.getRole().name())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    /**
     * 用户注册
     */
    @Transactional
    public AuthResult register(RegisterCmd cmd) {
        log.info("【AuthService】用户注册, username: {}", cmd.getUsername());

        // 1. 检查用户名是否已存在
        if (userRepository.existsByUsername(cmd.getUsername())) {
            throw new BizException("用户名已存在");
        }

        // 2. 检查手机号是否已存在
        if (cmd.getPhone() != null && userRepository.existsByPhone(cmd.getPhone())) {
            throw new BizException("手机号已被注册");
        }

        // 3. 创建用户
        String userId = "U-" + idGenerator.nextId();
        String encodedPassword = passwordEncoderService.encode(cmd.getPassword());

        User user = User.createUser(userId, cmd.getUsername(), encodedPassword, cmd.getNickname());
        if (cmd.getPhone() != null) {
            user.bindPhone(cmd.getPhone());
        }

        userRepository.save(user);

        // 4. 生成令牌
        String accessToken = tokenService.generateToken(
                user.getUserId(),
                user.getUsername(),
                user.getRole().name());
        String refreshToken = tokenService.generateRefreshToken(user.getUserId());

        log.info("【AuthService】注册成功, userId: {}", userId);

        return AuthResult.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .role(user.getRole().name())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    /**
     * 刷新令牌
     */
    public AuthResult refreshToken(String refreshToken) {
        // 1. 验证刷新令牌
        if (!tokenService.validateToken(refreshToken)) {
            throw new BizException("刷新令牌无效");
        }

        if (!tokenService.isRefreshToken(refreshToken)) {
            throw new BizException("非法令牌类型");
        }

        // 2. 获取用户信息
        String userId = tokenService.getUserIdFromToken(refreshToken);
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BizException("用户不存在"));

        if (!user.isActive()) {
            throw new BizException("用户已被禁用或锁定");
        }

        // 3. 生成新令牌
        String newAccessToken = tokenService.generateToken(
                user.getUserId(),
                user.getUsername(),
                user.getRole().name());
        String newRefreshToken = tokenService.generateRefreshToken(user.getUserId());

        log.info("【AuthService】刷新令牌成功, userId: {}", userId);

        return AuthResult.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .role(user.getRole().name())
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }
}
