package org.example.start.base;

import org.example.Application;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

/**
 * 集成测试基类
 *
 * <p>
 * 所有 Application 层集成测试继承此类，自动配置 Testcontainers 环境。
 *
 * <p>
 * 架构说明：
 * - application 层没有 @Configuration 类，无法启动 Spring 容器
 * - Bean 注册在 infrastructure 层的 DomainServiceConfiguration
 * - 因此测试必须在 start 层，使用 Application.class 作为入口
 *
 */
@SpringBootTest(classes = Application.class)  // 关键改动：指定启动类
@TestPropertySource(locations = "classpath:application-test.yml")
public abstract class IntegrationTestBase {

    /**
     * 动态配置 Testcontainers 属性
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // MySQL 配置
        registry.add("spring.datasource.url", TestContainersConfig::getMysqlJdbcUrl);
        registry.add("spring.datasource.username", TestContainersConfig::getMysqlUsername);
        registry.add("spring.datasource.password", TestContainersConfig::getMysqlPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");

        // Redis 配置
        registry.add("spring.data.redis.host", () -> TestContainersConfig.getRedisContainer().getHost());
        registry.add("spring.data.redis.port", () -> TestContainersConfig.getRedisContainer().getFirstMappedPort());

        // RabbitMQ 配置
        registry.add("spring.rabbitmq.host", TestContainersConfig::getRabbitmqHost);
        registry.add("spring.rabbitmq.port", TestContainersConfig::getRabbitmqAmqpPort);
        registry.add("spring.rabbitmq.username", () -> "guest");
        registry.add("spring.rabbitmq.password", () -> "guest");

        // 禁用 RabbitMQ 监听器自动启动（测试环境不需要）
        registry.add("spring.rabbitmq.listener.simple.auto-startup", () -> "false");
        registry.add("spring.rabbitmq.listener.direct.auto-startup", () -> "false");
    }
}
