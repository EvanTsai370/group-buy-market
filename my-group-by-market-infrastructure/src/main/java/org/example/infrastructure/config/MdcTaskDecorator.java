package org.example.infrastructure.config;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;
import org.springframework.lang.NonNull;

import java.util.Map;

/**
 * 异步线程上下文装饰器
 * 作用：将主线程的 MDC (TraceId) 传递给异步线程
 */
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    @NonNull
    public Runnable decorate(@NonNull Runnable runnable) {
        // 1. 在主线程：捕获当前的 MDC 上下文
        Map<String, String> contextMap = MDC.getCopyOfContextMap();

        return () -> {
            try {
                // 2. 在子线程：如果主线程有上下文，就恢复到子线程
                if (contextMap != null) {
                    MDC.setContextMap(contextMap);
                }
                // 3. 执行任务
                runnable.run();
            } finally {
                // 4. 清理子线程的 MDC，防止污染线程池
                MDC.clear();
            }
        };
    }
}