package org.example.common.exception;

import lombok.Getter;

@Getter
public class BizException extends RuntimeException {
    
    private final String code;
    private final String msgKey;
    private final Object[] args; // 新增：支持动态参数

    // 场景：直接传 Key
    public BizException(String msgKey, Object... args) {
        super(msgKey);
        this.code = ErrorCode.BUSINESS_ERROR.getCode();
        this.msgKey = msgKey;
        this.args = args;
    }

    // 场景：传枚举
    public BizException(IErrorCode errorCode, Object... args) {
        super(errorCode.getMsgKey()); // 这里拿的是 Key
        this.code = errorCode.getCode();
        this.msgKey = errorCode.getMsgKey();
        this.args = args;
    }
}