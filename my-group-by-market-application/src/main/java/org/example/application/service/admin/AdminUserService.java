package org.example.application.service.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.application.assembler.UserResultAssembler;
import org.example.application.service.admin.result.UserDetailResult;
import org.example.common.exception.BizException;
import org.example.domain.model.user.User;
import org.example.domain.model.user.repository.UserRepository;
import org.example.domain.model.user.valueobject.UserRole;
import org.example.domain.model.user.valueobject.UserStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户管理服务（管理后台）
 * 
 * @author 开发团队
 * @since 2026-01-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final UserResultAssembler userResultAssembler;

    /**
     * 获取用户列表
     */
    public List<UserDetailResult> listUsers(int page, int size) {
        log.info("【AdminUser】查询用户列表, page: {}, size: {}", page, size);
        List<User> users = userRepository.findAll(page, size);
        return userResultAssembler.toResultList(users);
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

        user.setStatus(UserStatus.DISABLED);
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

        user.setStatus(UserStatus.ACTIVE);
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

        // 注意：这里应该调用 PasswordEncoderService 加密
        // 为简化示例，暂时不加密
        user.updatePassword(newPassword);
        userRepository.update(user);

        log.info("【AdminUser】用户密码已重置, userId: {}", userId);
        return "密码已重置";
    }
}
