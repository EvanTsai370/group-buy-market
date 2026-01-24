package org.example.common.ratelimit;

/**
 * 限流器接口
 *
 * <p>
 * 用于控制API调用频率，防止恶意刷单和系统过载
 *
 * <p>
 * 使用场景：
 * <ul>
 * <li>退款接口：防止用户频繁退款</li>
 * <li>锁单接口：防止恶意占用库存</li>
 * <li>查询接口：防止爬虫和恶意查询</li>
 * </ul>
 *
 */
public interface RateLimiter {

    /**
     * 尝试获取令牌（使用默认配置）
     *
     * @param key 限流键（如：userId、IP等）
     * @return true-获取成功，false-被限流
     */
    boolean tryAcquire(String key);

    /**
     * 尝试获取令牌（自定义配置）
     *
     * @param key           限流键
     * @param maxRequests   时间窗口内最大请求数
     * @param windowSeconds 时间窗口（秒）
     * @return true-获取成功，false-被限流
     */
    boolean tryAcquire(String key, int maxRequests, long windowSeconds);

    /**
     * 重置限流计数
     *
     * @param key 限流键
     */
    void reset(String key);
}
