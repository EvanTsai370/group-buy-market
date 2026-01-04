package org.example.infrastructure.config.dynamic;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * 动态配置自动配置类
 * 负责初始化动态配置相关的 Bean
 */
@Configuration
@ConditionalOnClass({StringRedisTemplate.class, RedisConnectionFactory.class})
@ConditionalOnProperty(prefix = "dynamic.config", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(DynamicConfigProperties.class)
public class DynamicConfigAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(DynamicConfigAutoConfiguration.class);

    private final DynamicConfigProperties properties;
    private final ConfigurableEnvironment environment;

    public DynamicConfigAutoConfiguration(DynamicConfigProperties properties,
                                          ConfigurableEnvironment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    @PostConstruct
    public void init() {
        log.info("动态配置中心初始化完成");
        log.info("  - Redis Key Prefix: {}", properties.getRedisKeyPrefix());
        log.info("  - Redis Topic: {}", properties.getRedisTopic());
        log.info("  - Snapshot Enabled: {}", properties.isSnapshotEnabled());
        log.info("  - Load On Startup: {}", properties.isLoadOnStartup());
    }

    /**
     * 创建动态配置 PropertySource
     */
    @Bean
    public DynamicRedisPropertySource dynamicRedisPropertySource() {
        DynamicRedisPropertySource propertySource = new DynamicRedisPropertySource();

        // 将 PropertySource 注册到 Environment（最高优先级）
        environment.getPropertySources().addFirst(propertySource);

        log.info("DynamicRedisPropertySource 已注册到 Environment");
        return propertySource;
    }

    /**
     * 创建配置刷新管理器
     */
    @Bean
    public PropertyRefreshManager propertyRefreshManager(
            DynamicRedisPropertySource propertySource,
            StringRedisTemplate stringRedisTemplate,
            ApplicationEventPublisher eventPublisher) {

        PropertyRefreshManager manager = new PropertyRefreshManager(
                propertySource,
                stringRedisTemplate,
                environment,
                eventPublisher,
                properties.getRedisKeyPrefix()
        );

        // 启动时加载配置
        if (properties.isLoadOnStartup()) {
            log.info("启动时从 Redis 加载配置...");
            manager.loadConfigFromRedis();
        }

        return manager;
    }

    /**
     * 创建 Redis 消息监听器
     */
    @Bean
    public RedisConfigMessageListener redisConfigMessageListener(PropertyRefreshManager refreshManager) {
        return new RedisConfigMessageListener(refreshManager);
    }

    /**
     * 创建 Redis 消息监听容器
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            RedisConfigMessageListener messageListener) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        // 订阅配置刷新主题
        container.addMessageListener(messageListener, new ChannelTopic(properties.getRedisTopic()));

        log.info("Redis 消息监听容器已启动，订阅主题: {}", properties.getRedisTopic());
        return container;
    }

    /**
     * 创建动态配置 Facade（供业务使用）
     */
    @Bean
    public DynamicConfigService dynamicConfigService(
            PropertyRefreshManager refreshManager,
            StringRedisTemplate stringRedisTemplate) {

        return new DynamicConfigService(
                refreshManager,
                stringRedisTemplate,
                properties.getRedisTopic()
        );
    }
}
