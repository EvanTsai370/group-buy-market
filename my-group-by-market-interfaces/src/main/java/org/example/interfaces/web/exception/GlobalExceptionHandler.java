package org.example.interfaces.web.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.example.common.api.Result;
import org.example.common.exception.BizException;
import org.example.common.exception.ErrorCode;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Objects;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 1. 处理自定义业务异常 (BizException)
     * 场景：代码中手动 throw new BizException(...)
     */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException e, HttpServletRequest request) {
        // 使用 getFormattedMsg() 获取格式化后的消息（支持参数化）
        String formattedMsg = e.getFormattedMsg();
        log.warn("业务异常: code={}, msg={}, url={}", e.getCode(), formattedMsg, request.getRequestURI());
        // 业务异常通常返回 200 HTTP 状态码，但在 Body 里标明错误
        return Result.failure(e.getCode(), formattedMsg);
    }

    /**
     * 2. 处理参数校验异常 (@RequestBody @Valid)
     * 场景：前端传参不符合 DTO 里的 @NotNull, @Size 规则
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleJsonValidException(MethodArgumentNotValidException e) {
        // 获取第一个校验失败的字段提示信息
        String errorMsg = Objects.requireNonNull(e.getBindingResult().getFieldError()).getDefaultMessage();
        log.warn("参数校验失败: {}", errorMsg);
        return Result.failure(ErrorCode.PARAM_ERROR.getCode(), errorMsg);
    }

    /**
     * 3. 处理 Get 请求参数校验异常 (BindException)
     * 场景：GET /api?age=abc (类型不匹配)
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        String errorMsg = Objects.requireNonNull(e.getBindingResult().getFieldError()).getDefaultMessage();
        log.warn("参数绑定失败: {}", errorMsg);
        return Result.failure(ErrorCode.PARAM_ERROR.getCode(), errorMsg);
    }

    /**
     * 99. 处理所有未知的系统异常 (Exception)
     * 场景：NPE, IndexOutOfBounds, 数据库连接失败等
     * 兜底策略：绝对不能给前端返回堆栈信息！
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("系统未知异常: url={}", request.getRequestURI(), e); // 只有这里才打 error 级别日志并打印堆栈
        return Result.failure(ErrorCode.SYSTEM_ERROR.getCode(), "系统繁忙，请稍后再试");
    }
}