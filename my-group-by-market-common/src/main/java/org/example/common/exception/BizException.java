package org.example.common.exception;

import lombok.Getter;

@Getter
public class BizException extends RuntimeException {

    private final String code;
    private final String msg;

    // 默认构造：使用通用业务错误
    public BizException(String msg) {
        super(msg);
        this.code = ErrorCode.BUSINESS_ERROR.getCode();
        this.msg = msg;
    }

    // 使用枚举构造
    public BizException(IErrorCode errorCode) {
        super(errorCode.getMsg());
        this.code = errorCode.getCode();
        this.msg = errorCode.getMsg();
    }

    // 自定义错误码和消息
    public BizException(String code, String msg) {
        super(msg);
        this.code = code;
        this.msg = msg;
    }
}