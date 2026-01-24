package org.example.infrastructure.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBitSet;
import org.redisson.api.RLock;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
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
        bitSet.expire(Duration.of(timeout, toChronoUnit(unit)));
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
        redissonClient.getAtomicLong(key).expire(Duration.of(timeout, toChronoUnit(unit)));
    }

    @Override
    public void remove(String key) {
        redissonClient.getBucket(key).delete();
    }

    @Override
    public Long executeScript(String script, List<Object> keys, Object... args) {
        // 1. 获取脚本对象
        // 【关键点】使用 StringCodec.INSTANCE
        // 这确保了 Redis 中的数据是以普通字符串/数字存储的，而不是 Redisson 特有的二进制编码
        // 这样 Lua 脚本中的 tonumber() 和 decr 才能正常工作
        RScript rScript = redissonClient.getScript(StringCodec.INSTANCE);

        // 2. 执行脚本
        // Mode.READ_WRITE: 读写模式 (因为我们要 set 和 decr)
        // ReturnType.Long: 告诉 Redisson 脚本返回的是个整数 (Long)
        return rScript.eval(
                RScript.Mode.READ_WRITE,
                script,
                RScript.ReturnType.LONG,
                keys,
                args
        );
    }

    /**
     * 将 TimeUnit 转换为 ChronoUnit
     */
    private ChronoUnit toChronoUnit(TimeUnit unit) {
        return switch (unit) {
            case NANOSECONDS -> ChronoUnit.NANOS;
            case MICROSECONDS -> ChronoUnit.MICROS;
            case MILLISECONDS -> ChronoUnit.MILLIS;
            case SECONDS -> ChronoUnit.SECONDS;
            case MINUTES -> ChronoUnit.MINUTES;
            case HOURS -> ChronoUnit.HOURS;
            case DAYS -> ChronoUnit.DAYS;
        };
    }
}
