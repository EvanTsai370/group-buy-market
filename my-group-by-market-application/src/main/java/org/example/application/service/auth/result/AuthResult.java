package org.example.application.service.auth.result;

import lombok.Builder;
import lombok.Data;

/**
 * 认证结果
 */
@Data
@Builder
public class AuthResult {
    private String userId;
    private String username;
    private String nickname;
    private String role;
    private String accessToken;
    private String refreshToken;
}
