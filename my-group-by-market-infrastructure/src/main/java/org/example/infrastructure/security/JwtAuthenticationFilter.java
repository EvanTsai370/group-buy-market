package org.example.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器
 * 
 * <p>
 * 拦截请求，从请求头中提取 JWT 令牌并验证
 * </p>
 * 
 * @author 开发团队
 * @since 2026-01-10
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            String token = extractToken(request);

            if (StringUtils.hasText(token) && jwtTokenService.validateToken(token)) {
                String userId = jwtTokenService.getUserIdFromToken(token);
                String username = jwtTokenService.getUsernameFromToken(token);
                String role = jwtTokenService.getRoleFromToken(token);

                // 创建认证对象
                List<SimpleGrantedAuthority> authorities = List.of(
                        new SimpleGrantedAuthority("ROLE_" + role));

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        new JwtUserPrincipal(userId, username, role),
                        null,
                        authorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);

                // 设置请求属性，方便后续使用
                request.setAttribute("userId", userId);
                request.setAttribute("username", username);
                request.setAttribute("role", role);

                log.debug("【JWT过滤器】用户认证成功, userId: {}, role: {}", userId, role);
            }
        } catch (Exception e) {
            log.warn("【JWT过滤器】认证失败: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    /**
     * JWT 用户主体
     */
    public record JwtUserPrincipal(String userId, String username, String role) {
    }
}
