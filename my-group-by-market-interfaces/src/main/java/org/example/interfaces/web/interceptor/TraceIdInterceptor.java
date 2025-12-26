package org.example.interfaces.web.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.lang.NonNull; // 1. 引入 Spring 的 NonNull
import org.springframework.lang.Nullable; // 2. 引入 Spring 的 Nullable
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * 全链路追踪拦截器
 * 作用：在请求开始时生成 TraceID，结束时清理，防止内存泄漏
 */
@Component
public class TraceIdInterceptor implements HandlerInterceptor {

    private static final String TRACE_ID_KEY = "traceId";

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,   // 3. 加上 @NonNull
                             @NonNull HttpServletResponse response, // 3. 加上 @NonNull
                             @NonNull Object handler) {             // 3. 加上 @NonNull

        String traceId = UUID.randomUUID().toString().replace("-", "");
        MDC.put(TRACE_ID_KEY, traceId);
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, // 3. 加上 @NonNull
                                @NonNull HttpServletResponse response,
                                @NonNull Object handler,
                                @Nullable Exception ex) {            // 4. 注意：Exception 可能是 null（请求成功时），所以要加 @Nullable

        MDC.remove(TRACE_ID_KEY);
    }
}