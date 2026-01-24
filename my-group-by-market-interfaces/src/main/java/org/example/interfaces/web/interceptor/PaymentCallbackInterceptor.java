package org.example.interfaces.web.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.example.common.exception.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

/**
 * 支付回调 IP 白名单拦截器
 *
 * <p>
 * 职责：
 * <ul>
 * <li>限制支付回调接口只能从指定IP访问</li>
 * <li>防止公网恶意访问</li>
 * </ul>
 *
 * <p>
 * 配置方式：
 * 
 * <pre>
 * payment:
 *   allowed-ips:
 *     - 127.0.0.1
 *     - 123.45.67.89
 * </pre>
 *
 */
@Slf4j
@Component
public class PaymentCallbackInterceptor implements HandlerInterceptor {

    @Value("#{${payment.allowed-ips:{'127.0.0.1'}}}")
    private Set<String> allowedIps;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler) {
        String clientIp = getClientIp(request);

        if (!allowedIps.contains(clientIp)) {
            log.warn("【支付回调】非法IP访问, ip: {}, uri: {}", clientIp, request.getRequestURI());
            throw new BizException("非法访问");
        }

        log.debug("【支付回调】IP白名单验证通过, ip: {}", clientIp);
        return true;
    }

    /**
     * 获取客户端真实IP
     * <p>
     * 考虑代理和负载均衡的情况
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // 多次代理的情况，取第一个IP
            int index = ip.indexOf(',');
            if (index != -1) {
                return ip.substring(0, index);
            }
            return ip;
        }

        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        return request.getRemoteAddr();
    }
}
