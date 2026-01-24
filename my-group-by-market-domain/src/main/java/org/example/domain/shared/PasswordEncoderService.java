package org.example.domain.shared;

/**
 * 密码编码器接口
 * 
 * <p>
 * 用于密码加密和验证，定义在 Domain 层以满足依赖倒置原则
 * </p>
 * 
 */
public interface PasswordEncoderService {

    /**
     * 加密密码
     * 
     * @param rawPassword 原始密码
     * @return 加密后的密码
     */
    String encode(String rawPassword);

    /**
     * 验证密码
     * 
     * @param rawPassword     原始密码
     * @param encodedPassword 加密后的密码
     * @return 是否匹配
     */
    boolean matches(String rawPassword, String encodedPassword);
}
