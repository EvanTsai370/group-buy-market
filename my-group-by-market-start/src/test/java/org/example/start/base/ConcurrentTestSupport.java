package org.example.start.base;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 并发测试支持工具
 *
 * <p>
 * 提供高并发压力测试的工具方法，支持 100-500 线程同时执行。
 *
 */
@Slf4j
public class ConcurrentTestSupport {

    /**
     * 并发执行任务（所有线程同时开始）
     *
     * @param threadCount 线程数量
     * @param task        要执行的任务
     * @throws InterruptedException 如果等待被中断
     */
    public static void executeConcurrently(int threadCount, Runnable task) throws InterruptedException {
        executeConcurrently(threadCount, task, 60); // 默认 60 秒超时
    }

    /**
     * 并发执行任务（所有线程同时开始）
     *
     * @param threadCount   线程数量
     * @param task          要执行的任务
     * @param timeoutSeconds 超时时间（秒）
     * @throws InterruptedException 如果等待被中断
     * @throws RuntimeException 如果超时
     */
    public static void executeConcurrently(int threadCount, Runnable task, int timeoutSeconds) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);  // 控制统一开始
        CountDownLatch doneLatch = new CountDownLatch(threadCount);  // 等待全部完成

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();  // 等待统一开始信号
                    task.run();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("线程被中断", e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        log.info("【并发测试】准备启动 {} 个线程", threadCount);
        startLatch.countDown();  // 同时触发所有线程

        boolean finished = doneLatch.await(timeoutSeconds, TimeUnit.SECONDS);
        executor.shutdown();

        if (!finished) {
            log.error("【并发测试】超时：部分线程未在 {} 秒内完成", timeoutSeconds);
            throw new RuntimeException("并发测试超时");
        }

        log.info("【并发测试】所有线程执行完成");
    }

    /**
     * 并发执行任务并收集结果
     *
     * @param threadCount 线程数量
     * @param task        返回结果的任务
     * @param <T>         结果类型
     * @return 所有线程的执行结果
     * @throws InterruptedException 如果等待被中断
     */
    public static <T> List<T> executeConcurrentlyWithResults(int threadCount, Callable<T> task) throws InterruptedException {
        return executeConcurrentlyWithResults(threadCount, task, 60);
    }

    /**
     * 并发执行任务并收集结果
     *
     * @param threadCount    线程数量
     * @param task           返回结果的任务
     * @param timeoutSeconds 超时时间（秒）
     * @param <T>            结果类型
     * @return 所有线程的执行结果
     * @throws InterruptedException 如果等待被中断
     */
    public static <T> List<T> executeConcurrentlyWithResults(int threadCount, Callable<T> task, int timeoutSeconds) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<T>> futures = new ArrayList<>(threadCount);

        for (int i = 0; i < threadCount; i++) {
            Future<T> future = executor.submit(() -> {
                startLatch.await();  // 等待统一开始
                return task.call();
            });
            futures.add(future);
        }

        log.info("【并发测试】准备启动 {} 个线程（带结果收集）", threadCount);
        startLatch.countDown();

        List<T> results = new ArrayList<>(threadCount);
        for (Future<T> future : futures) {
            try {
                T result = future.get(timeoutSeconds, TimeUnit.SECONDS);
                results.add(result);
            } catch (ExecutionException e) {
                log.error("【并发测试】任务执行失败", e.getCause());
                throw new RuntimeException("并发任务执行失败", e.getCause());
            } catch (TimeoutException e) {
                log.error("【并发测试】任务超时", e);
                throw new RuntimeException("并发任务超时", e);
            }
        }

        executor.shutdown();
        log.info("【并发测试】所有线程执行完成，收集了 {} 个结果", results.size());
        return results;
    }

    /**
     * 并发执行任务并统计成功/失败次数
     *
     * @param threadCount 线程数量
     * @param task        要执行的任务（返回 true 表示成功）
     * @return 执行结果统计
     * @throws InterruptedException 如果等待被中断
     */
    public static ConcurrentExecutionResult executeConcurrentlyWithStats(int threadCount, Callable<Boolean> task) throws InterruptedException {
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<Exception> exceptions = new CopyOnWriteArrayList<>();

        executeConcurrently(threadCount, () -> {
            try {
                Boolean result = task.call();
                if (Boolean.TRUE.equals(result)) {
                    successCount.incrementAndGet();
                } else {
                    failureCount.incrementAndGet();
                }
            } catch (Exception e) {
                failureCount.incrementAndGet();
                exceptions.add(e);
                log.debug("【并发测试】任务执行异常: {}", e.getMessage());
            }
        });

        return new ConcurrentExecutionResult(
            threadCount,
            successCount.get(),
            failureCount.get(),
            exceptions
        );
    }

    /**
     * 并发执行结果统计
     */
    public static class ConcurrentExecutionResult {
        private final int totalThreads;
        private final int successCount;
        private final int failureCount;
        private final List<Exception> exceptions;

        public ConcurrentExecutionResult(int totalThreads, int successCount, int failureCount, List<Exception> exceptions) {
            this.totalThreads = totalThreads;
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.exceptions = exceptions;
        }

        public int getTotalThreads() {
            return totalThreads;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public int getFailureCount() {
            return failureCount;
        }

        public List<Exception> getExceptions() {
            return exceptions;
        }

        @Override
        public String toString() {
            return String.format("ConcurrentExecutionResult{total=%d, success=%d, failure=%d, exceptions=%d}",
                totalThreads, successCount, failureCount, exceptions.size());
        }
    }
}
