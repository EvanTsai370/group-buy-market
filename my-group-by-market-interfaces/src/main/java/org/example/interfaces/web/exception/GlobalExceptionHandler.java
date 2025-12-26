package org.example.interfaces.web.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.api.Result;
import org.example.common.exception.BizException;
import org.example.common.exception.ErrorCode;
import org.example.interfaces.web.utils.MessageUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 作用：捕获 Controller 层抛出的所有异常，统一翻译并封装成 JSON 返回
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageUtils messageUtils;

    /**
     * 1. 处理自定义业务异常 (BizException)
     * 场景：Service 层手动 throw new BizException("error.user.not.exist");
     */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException e, HttpServletRequest request) {
        // 1. 获取国际化翻译后的消息
        // e.getMsgKey() 是资源文件里的 key (如 error.points.not.enough)
        // e.getArgs() 是动态参数
        String translatedMsg = messageUtils.get(e.getMsgKey(), e.getArgs());

        // 2. 打印 WARN 日志 (业务异常通常不需要打印堆栈，除非为了调试)
        log.warn("业务异常 [TraceId: {}]: url={}, code={}, msg={}",
                org.slf4j.MDC.get("traceId"), // 显式获取一下TraceId方便看，虽然日志pattern里也有
                request.getRequestURI(),
                e.getCode(),
                translatedMsg);

        // 3. 返回统一结构
        return Result.failure(e.getCode(), translatedMsg);
    }

    /**
     * 2. 处理 JSON 参数校验异常 (@RequestBody + @Valid)
     * 场景：前端传的 JSON 少了必填字段
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        // 获取所有校验失败的字段信息，拼接成字符串
        String errorDetail = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage) // 这里拿到的是注解上的 message
                .collect(Collectors.joining(", "));

        // 使用 PARAM_ERROR 的 key ("error.param.invalid") 进行翻译
        // 假设资源文件里是: error.param.invalid=请求参数非法: {0}
        String translatedMsg = messageUtils.get(ErrorCode.PARAM_ERROR.getMsgKey(), errorDetail);

        log.warn("参数校验失败: {}", translatedMsg);
        return Result.failure(ErrorCode.PARAM_ERROR.getCode(), translatedMsg);
    }

    /**
     * 3. 处理 URL 参数校验异常 (Get请求 + @Valid)
     * 场景：/api/users?age=abc (类型不匹配)
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        String errorDetail = e.getBindingResult().getAllErrors().stream()
                .map(org.springframework.validation.ObjectError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        String translatedMsg = messageUtils.get(ErrorCode.PARAM_ERROR.getMsgKey(), errorDetail);

        log.warn("参数绑定失败: {}", translatedMsg);
        return Result.failure(ErrorCode.PARAM_ERROR.getCode(), translatedMsg);
    }

    /**
     * 4. 处理 404 静态资源未找到异常 (Spring Boot 3.2+ 新增)
     * 防止访问不存在的静态资源时打印大量 ERROR 日志
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public Result<Void> handleNoResourceFoundException(NoResourceFoundException e) {
        // debug 日志保留路径信息，方便排查
        log.debug("资源不存在: {}", e.getResourcePath());

        // 1. 获取国际化 Key: error.resource.not.found
        String msgKey = ErrorCode.RESOURCE_NOT_FOUND.getMsgKey();

        // 2. 翻译: "请求的资源不存在"
        String translatedMsg = messageUtils.get(msgKey);

        // 3. 返回统一结果 (code: A0404)
        return Result.failure(ErrorCode.RESOURCE_NOT_FOUND.getCode(), translatedMsg);
    }

    /**
     * 99. 兜底处理：系统未知异常 (Exception)
     * 场景：空指针 (NPE)、数据库连接断开、数组越界
     * 注意：这里必须打印 ERROR 日志和堆栈信息！
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        // 1. 翻译系统错误的通用文案 (如: "系统繁忙，请稍后再试")
        String translatedMsg = messageUtils.get(ErrorCode.SYSTEM_ERROR.getMsgKey());

        // 2. 打印 ERROR 日志 (包含堆栈 e)
        log.error("系统未知异常 [TraceId: {}]: url={}",
                org.slf4j.MDC.get("traceId"),
                request.getRequestURI(),
                e);

        // 3. 这里的 code 是 B0001
        return Result.failure(ErrorCode.SYSTEM_ERROR.getCode(), translatedMsg);
    }
}