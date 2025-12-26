package org.example.infrastructure.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Configuration
@EnableAsync
@RequiredArgsConstructor
// 【关键】启用配置属性类，这样 Spring 才会去扫描 ThreadPoolProperties 并注入进来
@EnableConfigurationProperties(ThreadPoolProperties.class)
public class ThreadPoolConfig {

    private final ThreadPoolProperties properties;

    @Bean("commonExecutor")
    public Executor commonExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 使用 properties 中的配置，不再硬编码
        executor.setCorePoolSize(properties.getCoreSize());
        executor.setMaxPoolSize(properties.getMaxSize());
        executor.setQueueCapacity(properties.getQueueCapacity());
        executor.setKeepAliveSeconds(properties.getKeepAliveSeconds());
        executor.setThreadNamePrefix(properties.getThreadNamePrefix());

        executor.setTaskDecorator(new MdcTaskDecorator());

        // 拒绝策略和优雅关闭通常不需要配置化，保持硬编码即可
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();
        log.info("通用线程池初始化完毕: core={}, max={}, prefix={}",
                properties.getCoreSize(), properties.getMaxSize(), properties.getThreadNamePrefix());
        return executor;
    }
}