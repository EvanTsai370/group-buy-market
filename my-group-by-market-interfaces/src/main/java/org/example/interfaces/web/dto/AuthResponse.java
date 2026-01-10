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
    private String role;
    private String accessToken;
    private String refreshToken;
}
