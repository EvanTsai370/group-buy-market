package org.example.infrastructure.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.example.common.exception.BizException;
import org.example.common.ratelimit.RateLimit;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

/**
 * 限流AOP拦截器
 *
 * <p>
 * 拦截带有@RateLimit注解的方法，执行限流检查
 *
 * @author 开发团队
 * @since 2026-01-08
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RedisRateLimiter rateLimiter;
    private final ExpressionParser parser = new SpelExpressionParser();

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint point, RateLimit rateLimit) throws Throwable {
        // 1. 解析限流键
        String key = parseKey(rateLimit.key(), point);

        log.debug("【限流拦截器】检查限流, key={}, maxRequests={}, windowSeconds={}",
                key, rateLimit.maxRequests(), rateLimit.windowSeconds());

        // 2. 尝试获取令牌
        boolean acquired = rateLimiter.tryAcquire(
                key,
                rateLimit.maxRequests(),
                rateLimit.windowSeconds());

        if (!acquired) {
            log.warn("【限流拦截器】请求被限流, key={}", key);
            throw new BizException(rateLimit.message());
        }

        // 3. 执行方法
        return point.proceed();
    }

    /**
     * 解析SpEL表达式获取限流键
     */
    private String parseKey(String keyExpression, ProceedingJoinPoint point) {
        try {
            MethodSignature signature = (MethodSignature) point.getSignature();
            String[] paramNames = signature.getParameterNames();
            Object[] args = point.getArgs();

            EvaluationContext context = new StandardEvaluationContext();
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }

            return parser.parseExpression(keyExpression).getValue(context, String.class);

        } catch (Exception e) {
            log.error("【限流拦截器】解析限流键失败, expression={}", keyExpression, e);
            // 解析失败时使用原始表达式
            return keyExpression;
        }
    }
}
