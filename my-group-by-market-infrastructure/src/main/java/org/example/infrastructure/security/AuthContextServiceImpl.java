package org.example.infrastructure.security;

import lombok.RequiredArgsConstructor;
import org.example.common.exception.BizException;
import org.example.domain.shared.AuthContextService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 认证上下文服务实现
 * 
 * <p>
 * 基于 Spring Security 的 SecurityContextHolder 实现
 * </p>
 * 
 * @author 开发团队
 * @since 2026-01-12
 */
@Service
@RequiredArgsConstructor
public class AuthContextServiceImpl implements AuthContextService {

    @Override
    public String getCurrentUserId() {
        return getCurrentUserIdOptional()
                .orElseThrow(() -> new BizException("用户未登录"));
    }

    @Override
    public Optional<String> getCurrentUserIdOptional() {
        JwtAuthenticationFilter.JwtUserPrincipal principal = getPrincipal();
        return Optional.ofNullable(principal)
                .map(JwtAuthenticationFilter.JwtUserPrincipal::userId);
    }

    @Override
    public String getCurrentUsername() {
        JwtAuthenticationFilter.JwtUserPrincipal principal = getPrincipal();
        if (principal == null) {
            throw new BizException("用户未登录");
        }
        return principal.username();
    }

    @Override
    public String getCurrentUserRole() {
        JwtAuthenticationFilter.JwtUserPrincipal principal = getPrincipal();
        if (principal == null) {
            throw new BizException("用户未登录");
        }
        return principal.role();
    }

    @Override
    public boolean isAuthenticated() {
        return getPrincipal() != null;
    }

    @Override
    public boolean isAdmin() {
        JwtAuthenticationFilter.JwtUserPrincipal principal = getPrincipal();
        return principal != null && "ADMIN".equals(principal.role());
    }

    /**
     * 从 SecurityContextHolder 获取认证主体
     */
    private JwtAuthenticationFilter.JwtUserPrincipal getPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof JwtAuthenticationFilter.JwtUserPrincipal jwtUserPrincipal) {
            return jwtUserPrincipal;
        }
        return null;
    }
}
