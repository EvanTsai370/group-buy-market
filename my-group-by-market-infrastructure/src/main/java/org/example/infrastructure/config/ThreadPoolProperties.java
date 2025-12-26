package org.example.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 线程池配置属性类
 * 对应 application.yml 中的 app.thread-pool 前缀
 */
@Data
@ConfigurationProperties(prefix = "app.thread-pool")
public class ThreadPoolProperties {

    /** 核心线程数 (默认 cpu * 2) */
    private Integer coreSize = 4;

    /** 最大线程数 (默认 cpu * 4) */
    private Integer maxSize = 8;

    /** 队列容量 */
    private Integer queueCapacity = 100;

    /** 线程空闲保留时间 (秒) */
    private Integer keepAliveSeconds = 60;

    /** 线程名前缀 (方便查日志) */
    private String threadNamePrefix = "custom-async-";
}