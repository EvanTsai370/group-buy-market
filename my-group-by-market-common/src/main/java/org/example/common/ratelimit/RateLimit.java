package org.example.common.ratelimit;

import java.lang.annotation.*;

/**
 * 限流注解
 *
 * <p>
 * 使用方式：
 * 
 * <pre>
 * {@code @RateLimit(key = "#userId", maxRequests = 3, windowSeconds = 60)}
 * public void refund(String userId, String tradeOrderId) {
 *     // 业务逻辑
 * }
 * </pre>
 *
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 限流键的SpEL表达式
     * 
     * <p>
     * 示例：
     * <ul>
     * <li>#userId - 使用userId参数</li>
     * <li>#request.tradeOrderId - 使用request对象的tradeOrderId字段</li>
     * <li>'global' - 全局限流</li>
     * </ul>
     */
    String key();

    /**
     * 时间窗口内最大请求数
     */
    int maxRequests() default 3;

    /**
     * 时间窗口（秒）
     */
    long windowSeconds() default 60;

    /**
     * 限流后的错误消息
     */
    String message() default "操作过于频繁，请稍后再试";
}
