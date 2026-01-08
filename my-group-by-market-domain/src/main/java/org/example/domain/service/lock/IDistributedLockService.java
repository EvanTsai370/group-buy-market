package org.example.domain.service.lock;

import java.util.concurrent.TimeUnit;

/**
 * 分布式锁服务接口
 *
 * <p>
 * 定义在Domain层，实现在Infrastructure层
 *
 * <p>
 * 设计说明：
 * <ul>
 * <li>遵循DDD分层架构原则，Domain层不依赖Infrastructure层</li>
 * <li>通过接口隔离，Domain层只依赖抽象，不依赖具体实现</li>
 * <li>便于单元测试（可以mock此接口）</li>
 * </ul>
 *
 * @author 开发团队
 * @since 2026-01-08
 */
public interface IDistributedLockService {

    /**
     * 尝试获取分布式锁
     *
     * <p>
     * 使用场景：需要等待锁释放的场景
     *
     * @param lockKey   锁的key
     * @param waitTime  等待时间
     * @param leaseTime 持有时间（锁的过期时间）
     * @param unit      时间单位
     * @return true=获取成功, false=获取失败
     */
    boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit);

    /**
     * 释放分布式锁
     *
     * @param lockKey 锁的key
     */
    void unlock(String lockKey);

    /**
     * SET if Not eXists（轻量级锁）
     *
     * <p>
     * 使用场景：
     * <ul>
     * <li>不需要等待锁释放</li>
     * <li>如果锁已被占用，直接返回false</li>
     * <li>适用于幂等性保护（如退款、库存恢复）</li>
     * </ul>
     *
     * @param key     Redis key
     * @param timeout 过期时间
     * @param unit    时间单位
     * @return true=设置成功（获取锁成功）, false=key已存在（锁已被占用）
     */
    Boolean setNx(String key, long timeout, TimeUnit unit);

    /**
     * 删除key（释放锁）
     *
     * @param key Redis key
     * @return true=删除成功, false=删除失败
     */
    boolean delete(String key);
}
