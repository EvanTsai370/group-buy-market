package org.example.interfaces.web.dto;

import lombok.Data;

/**
 * 认证响应
 */
@Data
public class AuthResponse {
    private String userId;
    private String username;
    private String nickname;
    // 前端根据角色路由：
    //  - USER → 商城主页 (/mall/*)
    //  - ADMIN → 管理后台 (/admin/*)
    private String role;
    private String accessToken;
    private String refreshToken;
}
