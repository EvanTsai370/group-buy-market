package org.example.interfaces.web.config;

import lombok.RequiredArgsConstructor;
import org.example.interfaces.web.interceptor.PaymentCallbackInterceptor;
import org.example.interfaces.web.interceptor.TraceIdInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final TraceIdInterceptor traceIdInterceptor;
    private final PaymentCallbackInterceptor paymentCallbackInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 TraceID 拦截器，匹配所有路径
        registry.addInterceptor(traceIdInterceptor)
                .addPathPatterns("/**");

        // 注册支付回调 IP 白名单拦截器（仅拦截支付回调接口）
        registry.addInterceptor(paymentCallbackInterceptor)
                .addPathPatterns("/api/trade/payment/success/**");
    }
}