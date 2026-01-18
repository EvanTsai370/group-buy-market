package org.example.infrastructure.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBitSet;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Redis 服务实现
 * 基于 Redisson 客户端实现高级 Redis 操作
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService implements IRedisService {

    private final RedissonClient redissonClient;

    @Override
    public RBitSet getBitSet(String key) {
        return redissonClient.getBitSet(key);
    }

    @Override
    public RLock getLock(String lockKey) {
        return redissonClient.getLock(lockKey);
    }

    @Override
    public boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            return lock.tryLock(waitTime, leaseTime, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("【Redis服务】获取分布式锁被中断, lockKey: {}", lockKey, e);
            return false;
        }
    }

    @Override
    public void unlock(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    @Override
    public void expire(String key, long timeout, TimeUnit unit) {
        RBitSet bitSet = redissonClient.getBitSet(key);
        bitSet.expire(java.time.Duration.of(timeout, toChronoUnit(unit)));
    }

    @Override
    public boolean delete(String key) {
        return redissonClient.getBucket(key).delete();
    }

    @Override
    public boolean exists(String key) {
        return redissonClient.getBucket(key).isExists();
    }

    @Override
    public long incr(String key) {
        return redissonClient.getAtomicLong(key).incrementAndGet();
    }

    @Override
    public long decr(String key) {
        return redissonClient.getAtomicLong(key).decrementAndGet();
    }

    @Override
    public Boolean setNx(String key, long timeout, TimeUnit unit) {
        return redissonClient.getBucket(key).setIfAbsent("1", Duration.of(timeout, toChronoUnit(unit)));
    }

    @Override
    public Boolean setNx(String key, Object value, long timeout, TimeUnit unit) {
        // 关键点：使用 StringCodec.INSTANCE
        // 确保存入 Redis 的是 "5" 这样的字符串，而不是二进制对象
        // 这样后续的 DECR 操作才能正常识别它是数字
        return redissonClient.getBucket(key, StringCodec.INSTANCE)
                .setIfAbsent(value, Duration.of(timeout, toChronoUnit(unit)));
    }

    @Override
    public Long getAtomicLong(String key) {
        long value = redissonClient.getAtomicLong(key).get();
        // 如果key不存在，Redisson返回0，我们返回null以区分"不存在"和"值为0"
        return redissonClient.getBucket(key).isExists() ? value : null;
    }

    @Override
    public void setLong(String key, Long value, long timeout, TimeUnit unit) {
        redissonClient.getAtomicLong(key).set(value);
        redissonClient.getAtomicLong(key).expire(java.time.Duration.of(timeout, toChronoUnit(unit)));
    }

    @Override
    public void remove(String key) {
        redissonClient.getBucket(key).delete();
    }

    /**
     * 将 TimeUnit 转换为 ChronoUnit
     */
    private java.time.temporal.ChronoUnit toChronoUnit(TimeUnit unit) {
        return switch (unit) {
            case NANOSECONDS -> java.time.temporal.ChronoUnit.NANOS;
            case MICROSECONDS -> java.time.temporal.ChronoUnit.MICROS;
            case MILLISECONDS -> java.time.temporal.ChronoUnit.MILLIS;
            case SECONDS -> java.time.temporal.ChronoUnit.SECONDS;
            case MINUTES -> java.time.temporal.ChronoUnit.MINUTES;
            case HOURS -> java.time.temporal.ChronoUnit.HOURS;
            case DAYS -> java.time.temporal.ChronoUnit.DAYS;
        };
    }
}
