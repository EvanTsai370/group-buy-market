package org.example.infrastructure.security;

import lombok.RequiredArgsConstructor;
import org.example.domain.shared.PasswordEncoderService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * BCrypt 密码编码器实现
 * 
 */
@Service
@RequiredArgsConstructor
public class BCryptPasswordEncoderService implements PasswordEncoderService {

    private final PasswordEncoder passwordEncoder;

    @Override
    public String encode(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}
