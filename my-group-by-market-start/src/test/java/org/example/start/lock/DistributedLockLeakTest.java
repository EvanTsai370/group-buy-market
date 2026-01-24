package org.example.start.lock;

import lombok.extern.slf4j.Slf4j;
import org.example.domain.service.lock.IDistributedLockService;
import org.example.start.base.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import jakarta.annotation.Resource;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test 11: 30天锁死锁问题测试
 *
 * <p>
 * 测试场景：
 * <ul>
 * <li>线程A获取锁后被强制终止（模拟进程崩溃）</li>
 * <li>验证锁是否会永久泄漏</li>
 * <li>验证TTL机制是否生效</li>
 * </ul>
 *
 * <p>
 * 预期Bug（原实现）：
 * <ul>
 * <li>如果leaseTime设置过长（如30天），锁会长时间无法释放</li>
 * <li>后续请求会一直失败</li>
 * </ul>
 *
 * <p>
 * 修复方案：
 * <ul>
 * <li>将leaseTime从30秒改为更合理的值（如10秒）</li>
 * <li>使用Redisson的watchdog机制（leaseTime=-1时自动续期）</li>
 * <li>添加健康检查，定期清理过期锁</li>
 * </ul>
 *
 */
@Slf4j
public class DistributedLockLeakTest extends IntegrationTestBase {

    @Resource
    private IDistributedLockService lockService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String LOCK_KEY = "test:lock:refund:TRD001";

    @BeforeEach
    void setUp() {
        // 清理测试数据
        redisTemplate.delete(LOCK_KEY);
    }

    /**
     * Test 11.1: 验证锁的TTL机制（当前实现：30秒）
     *
     * <p>
     * 场景：线程获取锁后崩溃，验证锁是否会在TTL后自动释放
     */
    @Test
    void testLockTTL_shouldAutoReleaseAfter30Seconds() throws Exception {
        // Given: 线程A获取锁（leaseTime=30秒）
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch lockAcquired = new CountDownLatch(1);

        Future<Void> future = executor.submit(() -> {
            boolean locked = lockService.tryLock(LOCK_KEY, 0, 30, TimeUnit.SECONDS);
            assertThat(locked).isTrue();
            log.info("✅ 线程A获取锁成功");

            lockAcquired.countDown();

            // 模拟进程崩溃：线程休眠后直接退出（不调用unlock）
            Thread.sleep(1000);
            log.warn("⚠️ 模拟进程崩溃：线程A强制退出（未释放锁）");
            return null;
        });

        // 等待锁被获取
        lockAcquired.await();

        // When: 线程B立即尝试获取锁（应该失败）
        boolean lockedByB = lockService.tryLock(LOCK_KEY, 0, 1, TimeUnit.SECONDS);
        assertThat(lockedByB).isFalse();
        log.info("❌ 线程B获取锁失败（预期行为）");

        // 等待线程A退出
        future.get(5, TimeUnit.SECONDS);
        executor.shutdown();

        // Then: 等待TTL过期（30秒）
        log.info("⏳ 等待锁TTL过期（30秒）...");
        Thread.sleep(31_000); // 等待31秒确保锁已释放

        // 验证：锁已自动释放
        boolean lockedAfterTTL = lockService.tryLock(LOCK_KEY, 0, 1, TimeUnit.SECONDS);
        assertThat(lockedAfterTTL).isTrue();
        log.info("✅ 30秒后锁自动释放（TTL机制生效）");

        // 清理
        lockService.unlock(LOCK_KEY);
    }

    /**
     * Test 11.2: 验证30秒TTL是否过长
     *
     * <p>
     * 问题：退款操作通常在1-2秒内完成，30秒的TTL会导致：
     * <ul>
     * <li>进程崩溃后，用户需要等待30秒才能重试</li>
     * <li>降低系统可用性</li>
     * </ul>
     *
     * <p>
     * 建议：
     * <ul>
     * <li>将TTL改为10秒（足够处理退款逻辑，同时快速恢复）</li>
     * <li>或者使用Redisson的watchdog机制（leaseTime=-1）</li>
     * </ul>
     */
    @Test
    void testLockTTL_30SecondsIsTooLong() throws Exception {
        // Given: 线程A获取锁后崩溃
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch lockAcquired = new CountDownLatch(1);

        executor.submit(() -> {
            lockService.tryLock(LOCK_KEY, 0, 30, TimeUnit.SECONDS);
            lockAcquired.countDown();
            Thread.sleep(500); // 模拟崩溃
            return null;
        });

        lockAcquired.await();
        executor.shutdown();

        // When: 用户重试退款（10秒后）
        Thread.sleep(10_000);

        boolean canRetry = lockService.tryLock(LOCK_KEY, 0, 1, TimeUnit.SECONDS);

        // Then: 10秒后仍然无法获取锁（因为TTL=30秒）
        assertThat(canRetry).isFalse();
        log.warn("❌ Bug验证：30秒TTL过长，用户10秒后仍无法重试");

        // 清理
        Thread.sleep(21_000); // 再等21秒
        lockService.delete(LOCK_KEY);
    }

    /**
     * Test 11.3: 验证unlock失败的场景
     *
     * <p>
     * 场景：
     * <ul>
     * <li>线程A获取锁</li>
     * <li>unlock()时发生异常</li>
     * <li>验证锁是否会泄漏</li>
     * </ul>
     */
    @Test
    void testUnlockFailure_shouldNotLeakLock() throws Exception {
        // Given: 获取锁
        boolean locked = lockService.tryLock(LOCK_KEY, 0, 30, TimeUnit.SECONDS);
        assertThat(locked).isTrue();

        // When: 模拟unlock失败（直接返回，不调用unlock）
        log.warn("⚠️ 模拟unlock失败：未调用unlock()");

        // Then: 验证锁会在30秒后自动释放
        Thread.sleep(31_000);

        boolean canAcquire = lockService.tryLock(LOCK_KEY, 0, 1, TimeUnit.SECONDS);
        assertThat(canAcquire).isTrue();
        log.info("✅ unlock失败时，锁仍会在TTL后自动释放");

        // 清理
        lockService.unlock(LOCK_KEY);
    }

    /**
     * Test 11.4: 推荐方案 - 使用10秒TTL
     *
     * <p>
     * 优势：
     * <ul>
     * <li>10秒足够完成退款操作（通常1-2秒）</li>
     * <li>进程崩溃后，用户10秒后可重试</li>
     * <li>平衡了安全性和可用性</li>
     * </ul>
     */
    @Test
    void testRecommendedTTL_10Seconds() throws Exception {
        // Given: 使用10秒TTL
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch lockAcquired = new CountDownLatch(1);

        executor.submit(() -> {
            lockService.tryLock(LOCK_KEY, 0, 10, TimeUnit.SECONDS); // ✅ 改为10秒
            lockAcquired.countDown();
            Thread.sleep(500); // 模拟崩溃
            return null;
        });

        lockAcquired.await();
        executor.shutdown();

        // When: 11秒后尝试获取锁
        Thread.sleep(11_000);

        // Then: 锁已释放
        boolean canAcquire = lockService.tryLock(LOCK_KEY, 0, 1, TimeUnit.SECONDS);
        assertThat(canAcquire).isTrue();
        log.info("✅ 推荐方案验证：10秒TTL在崩溃后快速恢复");

        // 清理
        lockService.unlock(LOCK_KEY);
    }
}
