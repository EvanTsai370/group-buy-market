package org.example.infrastructure.config.dynamic;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 动态配置属性
 */
@ConfigurationProperties(prefix = "dynamic.config")
public class DynamicConfigProperties {

    /**
     * 是否启用动态配置
     */
    private boolean enabled = true;

    /**
     * Redis 配置键前缀
     */
    private String redisKeyPrefix = "config:";

    /**
     * Redis Pub/Sub 频道名称
     */
    private String redisTopic = "config:refresh";

    /**
     * 是否启用本地快照
     */
    private boolean snapshotEnabled = true;

    /**
     * 本地快照文件路径
     */
    private String snapshotPath = "config-snapshot.json";

    /**
     * 启动时是否从 Redis 加载配置
     */
    private boolean loadOnStartup = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getRedisKeyPrefix() {
        return redisKeyPrefix;
    }

    public void setRedisKeyPrefix(String redisKeyPrefix) {
        this.redisKeyPrefix = redisKeyPrefix;
    }

    public String getRedisTopic() {
        return redisTopic;
    }

    public void setRedisTopic(String redisTopic) {
        this.redisTopic = redisTopic;
    }

    public boolean isSnapshotEnabled() {
        return snapshotEnabled;
    }

    public void setSnapshotEnabled(boolean snapshotEnabled) {
        this.snapshotEnabled = snapshotEnabled;
    }

    public String getSnapshotPath() {
        return snapshotPath;
    }

    public void setSnapshotPath(String snapshotPath) {
        this.snapshotPath = snapshotPath;
    }

    public boolean isLoadOnStartup() {
        return loadOnStartup;
    }

    public void setLoadOnStartup(boolean loadOnStartup) {
        this.loadOnStartup = loadOnStartup;
    }
}
