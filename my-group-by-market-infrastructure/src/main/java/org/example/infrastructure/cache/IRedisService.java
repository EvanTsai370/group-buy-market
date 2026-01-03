package org.example.infrastructure.cache;

import org.redisson.api.RBitSet;
import org.redisson.api.RLock;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

/**
 * Redis 服务接口
 * 基于 Redisson 提供高级 Redis 操作
 */
public interface IRedisService {

    /**
     * 获取 BitSet（位图）
     *
     * @param key Redis key
     * @return RBitSet 对象
     */
    RBitSet getBitSet(String key);

    /**
     * 获取分布式锁
     *
     * @param lockKey 锁的 key
     * @return RLock 对象
     */
    RLock getLock(String lockKey);

    /**
     * 尝试获取分布式锁
     *
     * @param lockKey   锁的 key
     * @param waitTime  等待时间
     * @param leaseTime 持有时间
     * @param unit      时间单位
     * @return 是否获取成功
     */
    boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit);

    /**
     * 释放分布式锁
     *
     * @param lockKey 锁的 key
     */
    void unlock(String lockKey);

    /**
     * 设置 key 的过期时间
     *
     * @param key     Redis key
     * @param timeout 过期时间
     * @param unit    时间单位
     */
    void expire(String key, long timeout, TimeUnit unit);

    /**
     * 删除 key
     *
     * @param key Redis key
     * @return 是否删除成功
     */
    boolean delete(String key);

    /**
     * 检查 key 是否存在
     *
     * @param key Redis key
     * @return 是否存在
     */
    boolean exists(String key);

    /**
     * 将 userId 转换为 BitMap 索引
     * 使用 MD5 哈希算法将字符串 userId 映射为整数索引
     *
     * 注意：存在哈希冲突的可能性，对于人群标签场景可接受少量误判
     * 如需精确匹配，建议维护 userId -> index 的映射表
     *
     * @param userId 用户ID
     * @return BitMap 索引（正整数）
     */
    default long getIndexFromUserId(String userId) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(userId.getBytes(StandardCharsets.UTF_8));
            // 将哈希字节数组转换为正整数
            BigInteger bigInt = new BigInteger(1, hashBytes);
            // 取模以确保索引在合理范围内（1000万，约 1.2MB 内存）
            // 可根据实际人群规模调整此值
            return bigInt.mod(BigInteger.valueOf(10_000_000L)).longValue();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }
}
