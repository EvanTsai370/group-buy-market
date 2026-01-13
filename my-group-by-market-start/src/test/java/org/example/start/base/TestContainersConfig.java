package org.example.start.base;

import com.redis.testcontainers.RedisContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainers 单例配置
 *
 * <p>
 * 所有容器使用单例模式，避免重复启动导致测试缓慢。
 * 容器会在所有测试完成后自动关闭。
 *
 * @author 测试团队
 * @since 2026-01-13
 */
public class TestContainersConfig {

    private static final MySQLContainer<?> MYSQL_CONTAINER;
    private static final RedisContainer REDIS_CONTAINER;
    private static final RabbitMQContainer RABBITMQ_CONTAINER;

    static {
        // MySQL 8.2.0 容器（赋值给 final 字段，shutdown hook 会关闭）
        @SuppressWarnings("resource")
        MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.2.0"))
                .withDatabaseName("test_db")
                .withUsername("test_user")
                .withPassword("test_password")
                .withReuse(true);  // 重用容器，加速测试
        mysql.start();
        MYSQL_CONTAINER = mysql;

        // Redis 7.x 容器
        @SuppressWarnings("resource")
        RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:7-alpine"))
                .withReuse(true);
        redis.start();
        REDIS_CONTAINER = redis;

        // RabbitMQ 3.13 容器
        @SuppressWarnings("resource")
        RabbitMQContainer rabbitmq = new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.13-management-alpine"))
                .withReuse(true);
        rabbitmq.start();
        RABBITMQ_CONTAINER = rabbitmq;

        // 注册 JVM 关闭钩子（确保容器被关闭）
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            MYSQL_CONTAINER.stop();
            REDIS_CONTAINER.stop();
            RABBITMQ_CONTAINER.stop();
        }));
    }

    /**
     * 获取 MySQL 容器
     */
    public static MySQLContainer<?> getMysqlContainer() {
        return MYSQL_CONTAINER;
    }

    /**
     * 获取 Redis 容器
     */
    public static RedisContainer getRedisContainer() {
        return REDIS_CONTAINER;
    }

    /**
     * 获取 RabbitMQ 容器
     */
    public static RabbitMQContainer getRabbitmqContainer() {
        return RABBITMQ_CONTAINER;
    }

    /**
     * 获取 MySQL JDBC URL
     */
    public static String getMysqlJdbcUrl() {
        return MYSQL_CONTAINER.getJdbcUrl();
    }

    /**
     * 获取 MySQL 用户名
     */
    public static String getMysqlUsername() {
        return MYSQL_CONTAINER.getUsername();
    }

    /**
     * 获取 MySQL 密码
     */
    public static String getMysqlPassword() {
        return MYSQL_CONTAINER.getPassword();
    }

    /**
     * 获取 Redis 连接字符串
     */
    public static String getRedisConnectionString() {
        return String.format("redis://%s:%d",
            REDIS_CONTAINER.getHost(),
            REDIS_CONTAINER.getFirstMappedPort());
    }

    /**
     * 获取 RabbitMQ AMQP 端口
     */
    public static Integer getRabbitmqAmqpPort() {
        return RABBITMQ_CONTAINER.getAmqpPort();
    }

    /**
     * 获取 RabbitMQ Host
     */
    public static String getRabbitmqHost() {
        return RABBITMQ_CONTAINER.getHost();
    }
}
