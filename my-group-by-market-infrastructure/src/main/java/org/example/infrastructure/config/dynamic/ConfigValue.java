package org.example.infrastructure.config.dynamic;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 动态配置值对象
 * 线程安全：使用不可变对象设计
 */
public class ConfigValue {

    /**
     * 配置键
     */
    private final String key;

    /**
     * 配置值（原始字符串）
     */
    private final String rawValue;

    /**
     * 期望的值类型
     */
    private final Class<?> targetType;

    /**
     * 最后更新时间
     */
    private final LocalDateTime lastModifiedTime;

    /**
     * 配置来源
     */
    private final ConfigSource source;

    public ConfigValue(String key, String rawValue, Class<?> targetType, ConfigSource source) {
        this.key = Objects.requireNonNull(key, "配置键不能为空");
        this.rawValue = rawValue;
        this.targetType = targetType != null ? targetType : String.class;
        this.source = Objects.requireNonNull(source, "配置来源不能为空");
        this.lastModifiedTime = LocalDateTime.now();
    }

    /**
     * 获取转换后的配置值
     */
    @SuppressWarnings("unchecked")
    public <T> T getValue() {
        return (T) TypeConverter.convert(rawValue, targetType);
    }

    /**
     * 获取原始字符串值
     */
    public String getRawValue() {
        return rawValue;
    }

    /**
     * 创建新的配置值（用于更新）
     */
    public ConfigValue withNewValue(String newRawValue, ConfigSource newSource) {
        return new ConfigValue(this.key, newRawValue, this.targetType, newSource);
    }

    public String getKey() {
        return key;
    }

    public Class<?> getTargetType() {
        return targetType;
    }

    public LocalDateTime getLastModifiedTime() {
        return lastModifiedTime;
    }

    public ConfigSource getSource() {
        return source;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConfigValue that = (ConfigValue) o;
        return Objects.equals(key, that.key) &&
               Objects.equals(rawValue, that.rawValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, rawValue);
    }

    @Override
    public String toString() {
        return "ConfigValue{" +
               "key='" + key + '\'' +
               ", rawValue='" + rawValue + '\'' +
               ", targetType=" + targetType.getSimpleName() +
               ", source=" + source +
               ", lastModifiedTime=" + lastModifiedTime +
               '}';
    }

    /**
     * 配置来源枚举
     */
    public enum ConfigSource {
        /**
         * Redis 存储
         */
        REDIS,

        /**
         * 本地快照文件
         */
        LOCAL_SNAPSHOT,

        /**
         * 默认值
         */
        DEFAULT,

        /**
         * 应用配置文件（application.yml）
         */
        APPLICATION_PROPERTIES
    }
}
