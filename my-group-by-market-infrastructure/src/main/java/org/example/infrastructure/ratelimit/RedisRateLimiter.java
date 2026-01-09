package org.example.infrastructure.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.ratelimit.RateLimiter;
import org.example.infrastructure.cache.IRedisService;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis限流器实现
 *
 * <p>
 * 使用Redis计数器实现固定窗口限流
 *
 * <p>
 * 原理：
 * <ul>
 * <li>每个请求对Redis key执行INCR操作</li>
 * <li>如果是第一次请求，设置过期时间</li>
 * <li>如果计数超过阈值，拒绝请求</li>
 * </ul>
 *
 * @author 开发团队
 * @since 2026-01-08
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisRateLimiter implements RateLimiter {

    private final IRedisService redisService;

    // 默认配置：每分钟最多3次
    private static final int DEFAULT_MAX_REQUESTS = 3;
    private static final long DEFAULT_WINDOW_SECONDS = 60;

    @Override
    public boolean tryAcquire(String key) {
        return tryAcquire(key, DEFAULT_MAX_REQUESTS, DEFAULT_WINDOW_SECONDS);
    }

    @Override
    public boolean tryAcquire(String key, int maxRequests, long windowSeconds) {
        String rateLimitKey = "rate_limit:" + key;

        try {
            // 1. 原子递增
            Long count = redisService.incr(rateLimitKey);

            if (count == null) {
                log.warn("【限流器】INCR操作返回null, key={}", key);
                return true; // 限流器异常时，默认放行
            }

            // 2. 如果是第一次请求，设置过期时间
            if (count == 1) {
                redisService.expire(rateLimitKey, windowSeconds, TimeUnit.SECONDS);
            }

            // 3. 检查是否超过限制
            if (count > maxRequests) {
                log.warn("【限流器】触发限流, key={}, count={}, max={}", key, count, maxRequests);
                return false;
            }

            return true;

        } catch (Exception e) {
            log.error("【限流器】执行失败, key={}", key, e);
            // 限流器异常时，默认放行（避免影响业务）
            return true;
        }
    }

    @Override
    public void reset(String key) {
        String rateLimitKey = "rate_limit:" + key;
        redisService.delete(rateLimitKey);
    }
}
