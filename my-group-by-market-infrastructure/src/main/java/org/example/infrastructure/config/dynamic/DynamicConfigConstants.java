package org.example.infrastructure.config.dynamic;

/**
 * 动态配置常量
 */
public final class DynamicConfigConstants {

    private DynamicConfigConstants() {
        throw new UnsupportedOperationException("常量类不允许实例化");
    }

    /**
     * 默认 Redis 键前缀
     */
    public static final String DEFAULT_REDIS_KEY_PREFIX = "config:";

    /**
     * 默认 Redis Pub/Sub 主题
     */
    public static final String DEFAULT_REDIS_TOPIC = "config:refresh";

    /**
     * 默认快照文件名
     */
    public static final String DEFAULT_SNAPSHOT_FILE = "config-snapshot.json";

    /**
     * PropertySource 名称
     */
    public static final String PROPERTY_SOURCE_NAME = "dynamicRedisPropertySource";

    /**
     * 配置刷新事件名称
     */
    public static final String CONFIG_REFRESH_EVENT = "configRefreshEvent";
}
