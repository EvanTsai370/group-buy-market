package org.example.interfaces.web.advice;

import lombok.RequiredArgsConstructor;
import org.example.common.api.Result;
import org.example.interfaces.web.utils.MessageUtils;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 统一响应翻译处理器
 * 作用：拦截所有 Controller 返回的 Result 对象，自动将 msg 字段翻译成对应语言
 */
@RestControllerAdvice
@RequiredArgsConstructor
public class ResponseI18nAdvice implements ResponseBodyAdvice<Result<?>> {

    private final MessageUtils messageUtils;

    @Override
    public boolean supports(MethodParameter returnType, @NonNull Class<? extends HttpMessageConverter<?>> converterType) {
        // 只拦截返回类型为 Result 的接口
        return Result.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public Result<?> beforeBodyWrite(Result<?> body, @NonNull MethodParameter returnType, @NonNull MediaType selectedContentType,
                                     @NonNull Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                     @NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response) {
        if (body != null) {
            // 【核心逻辑】拿出 body 里的 msg (此时是 Key)，尝试翻译
            String originalKey = body.getMsg();
            String translatedMsg = messageUtils.get(originalKey);
            
            // 覆盖回去
            body.setMsg(translatedMsg);
        }
        return body;
    }
}