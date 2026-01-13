package org.example.common.exception;

import lombok.Getter;

@Getter
public class BizException extends RuntimeException {

    private final String code;
    private final String msg;
    private final Object[] args; // 消息参数，用于格式化错误消息

    // 默认构造：使用通用业务错误
    public BizException(String msg) {
        super(msg);
        this.code = ErrorCode.BUSINESS_ERROR.getCode();
        this.msg = msg;
        this.args = null;
    }

    // 默认构造：使用通用业务错误 + 动态参数（新增）
    public BizException(String msg, Object... args) {
        super(msg);
        this.code = ErrorCode.BUSINESS_ERROR.getCode();
        this.msg = msg;
        this.args = args;
    }

    // 使用枚举构造
    public BizException(IErrorCode errorCode) {
        super(errorCode.getMsg());
        this.code = errorCode.getCode();
        this.msg = errorCode.getMsg();
        this.args = null;
    }

    // 使用枚举构造 + 动态参数（新增）
    public BizException(IErrorCode errorCode, Object... args) {
        super(errorCode.getMsg());
        this.code = errorCode.getCode();
        this.msg = errorCode.getMsg();
        this.args = args;
    }

    // 自定义错误码和消息
    public BizException(String code, String msg) {
        super(msg);
        this.code = code;
        this.msg = msg;
        this.args = null;
    }

    /**
     * 获取格式化后的消息
     * 如果有参数，使用 String.format 格式化；否则返回原始消息
     *
     * @return 格式化后的错误消息
     */
    public String getFormattedMsg() {
        if (args == null || args.length == 0) {
            return msg;
        }
        try {
            return String.format(msg, args);
        } catch (Exception e) {
            // 格式化失败时返回原始消息
            return msg;
        }
    }
}