package org.example.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode implements IErrorCode {

    // --- 成功 ---
    // Key: response.success
    SUCCESS("00000", "response.success"),

    // --- A类: 用户端错误 ---
    // Key: error.client.base
    USER_ERROR("A0001", "error.client.base"),
    
    // Key: error.param.invalid
    PARAM_ERROR("A0400", "error.param.invalid"),
    
    // Key: error.unauthorized
    UNAUTHORIZED("A0401", "error.unauthorized"),

    // Key: error.resource.not.found
    RESOURCE_NOT_FOUND("A0404", "error.resource.not.found"),
    
    // Key: error.user.not.exist
    USER_NOT_EXIST("A0201", "error.user.not.exist"),

    // --- B类: 业务执行错误 ---
    // Key: error.system.error
    SYSTEM_ERROR("B0001", "error.system.error"),
    
    // Key: error.business.base
    BUSINESS_ERROR("B0002", "error.business.base"),

    // --- C类: 第三方调用错误 ---
    // Key: error.third.party
    THIRD_PARTY_ERROR("C0001", "error.third.party");

    private final String code;
    
    // 这里不再是具体的 msg，而是国际化资源文件里的 Key
    private final String msgKey;
}