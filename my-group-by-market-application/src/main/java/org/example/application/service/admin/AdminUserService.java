package org.example.application.service.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.application.assembler.UserResultAssembler;
import org.example.application.service.admin.cmd.CreateAdminCmd;
import org.example.application.service.admin.result.UserDetailResult;
import org.example.common.exception.BizException;
import org.example.domain.model.user.User;
import org.example.domain.model.user.repository.UserRepository;
import org.example.domain.model.user.valueobject.UserRole;
import org.example.domain.shared.PasswordEncoderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 用户管理服务（管理后台）
 * 
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final UserResultAssembler userResultAssembler;
    private final PasswordEncoderService passwordEncoderService;

    /**
     * 创建管理员
     */
    @Transactional
    public UserDetailResult createAdmin(CreateAdminCmd cmd) {
        log.info("【AdminUser】创建管理员, username: {}", cmd.getUsername());

        // 校验用户名
        if (cmd.getUsername() == null || cmd.getUsername().trim().isEmpty()) {
            throw new BizException("用户名不能为空");
        }

        // 校验密码
        if (cmd.getPassword() == null || cmd.getPassword().trim().isEmpty()) {
            throw new BizException("密码不能为空");
        }

        // 校验用户名唯一性
        if (userRepository.existsByUsername(cmd.getUsername())) {
            throw new BizException("用户名已存在");
        }

        // 生成用户ID
        String userId = UUID.randomUUID().toString();

        // 加密密码
        String encodedPassword = passwordEncoderService.encode(cmd.getPassword());

        // 创建管理员（使用领域层工厂方法）
        User admin = User.createAdmin(userId, cmd.getUsername(), encodedPassword);

        // 设置昵称（如果提供）
        if (cmd.getNickname() != null && !cmd.getNickname().trim().isEmpty()) {
            admin.setNickname(cmd.getNickname());
        }

        // 保存
        userRepository.save(admin);

        log.info("【AdminUser】管理员已创建, userId: {}, username: {}", userId, cmd.getUsername());

        return userResultAssembler.toResult(admin);
    }

    /**
     * 获取用户列表
     */
    public org.example.common.model.PageResult<UserDetailResult> listUsers(int page, int size) {
        log.info("【AdminUser】查询用户列表, page: {}, size: {}", page, size);
        org.example.common.model.PageResult<User> pageResult = userRepository.findByPage(page, size);

        List<UserDetailResult> list = userResultAssembler.toResultList(pageResult.getList());
        return new org.example.common.model.PageResult<>(list, pageResult.getTotal(), page, size);
    }

    /**
     * 获取用户详情
     */
    public UserDetailResult getUserDetail(String userId) {
        log.info("【AdminUser】查询用户详情, userId: {}", userId);
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BizException("用户不存在"));
        return userResultAssembler.toResult(user);
    }

    /**
     * 禁用用户
     */
    @Transactional
    public void disableUser(String userId) {
        log.info("【AdminUser】禁用用户, userId: {}", userId);

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BizException("用户不存在"));

        user.disable();
        userRepository.update(user);

        log.info("【AdminUser】用户已禁用, userId: {}", userId);
    }

    /**
     * 启用用户
     */
    @Transactional
    public void enableUser(String userId) {
        log.info("【AdminUser】启用用户, userId: {}", userId);

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BizException("用户不存在"));

        user.enable();
        userRepository.update(user);

        log.info("【AdminUser】用户已启用, userId: {}", userId);
    }

    /**
     * 设置用户角色
     */
    @Transactional
    public void setUserRole(String userId, UserRole role) {
        log.info("【AdminUser】设置用户角色, userId: {}, role: {}", userId, role);

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BizException("用户不存在"));

        user.setRole(role);
        userRepository.update(user);

        log.info("【AdminUser】用户角色已更新, userId: {}, role: {}", userId, role);
    }

    /**
     * 重置密码
     */
    @Transactional
    public String resetPassword(String userId, String newPassword) {
        log.info("【AdminUser】重置用户密码, userId: {}", userId);

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BizException("用户不存在"));

        user.updatePassword(passwordEncoderService.encode(newPassword));
        userRepository.update(user);

        log.info("【AdminUser】用户密码已重置, userId: {}", userId);
        return "密码已重置";
    }
}
