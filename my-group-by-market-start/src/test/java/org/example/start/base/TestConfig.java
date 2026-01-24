package org.example.start.base;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * 测试配置类
 * <p>
 * 排除测试环境不需要的配置类，避免启动失败
 *
 */
@TestConfiguration
@ComponentScan(
    basePackages = "org.example",
    excludeFilters = {
        // 排除 RabbitMQ 相关配置（测试环境不需要消息队列）
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = "org\\.example\\.infrastructure\\.config\\..*Rabbit.*"
        ),
        // 排除延迟队列配置
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = "org\\.example\\.infrastructure\\.config\\..*Delay.*"
        )
    }
)
public class TestConfig {
    // 测试配置类
}
