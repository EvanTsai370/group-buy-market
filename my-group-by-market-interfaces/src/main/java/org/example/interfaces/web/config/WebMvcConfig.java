package org.example.interfaces.web.config;

import lombok.RequiredArgsConstructor;
import org.example.interfaces.web.interceptor.TraceIdInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final TraceIdInterceptor traceIdInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 TraceID 拦截器，匹配所有路径
        registry.addInterceptor(traceIdInterceptor)
                .addPathPatterns("/**");
    }
}