package org.example.infrastructure.service;

import lombok.extern.slf4j.Slf4j;
import org.example.domain.service.lock.IDistributedLockService;
import org.example.infrastructure.cache.IRedisService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * Redis分布式锁服务实现
 *
 * <p>
 * 实现Domain层定义的IDistributedLockService接口
 *
 */
@Slf4j
@Service
public class RedisDistributedLockService implements IDistributedLockService {

    @Resource
    private IRedisService redisService;

    @Override
    public boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit) {
        try {
            return redisService.tryLock(lockKey, waitTime, leaseTime, unit);
        } catch (Exception e) {
            log.error("【分布式锁】获取锁失败, lockKey: {}", lockKey, e);
            return false;
        }
    }

    @Override
    public void unlock(String lockKey) {
        try {
            redisService.unlock(lockKey);
            log.debug("【分布式锁】释放锁成功, lockKey: {}", lockKey);
        } catch (Exception e) {
            log.error("【分布式锁】释放锁失败, lockKey: {}", lockKey, e);
        }
    }

    @Override
    public Boolean setNx(String key, long timeout, TimeUnit unit) {
        try {
            return redisService.setNx(key, timeout, unit);
        } catch (Exception e) {
            log.error("【分布式锁】setNx失败, key: {}", key, e);
            return false;
        }
    }

    @Override
    public boolean delete(String key) {
        try {
            return redisService.delete(key);
        } catch (Exception e) {
            log.error("【分布式锁】删除key失败, key: {}", key, e);
            return false;
        }
    }
}
