package org.example.infrastructure.config.dynamic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.PropertySource;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 Redis 的动态配置 PropertySource
 * 线程安全：使用 ConcurrentHashMap 存储配置
 */
public class DynamicRedisPropertySource extends PropertySource<Map<String, ConfigValue>> {

    private static final Logger log = LoggerFactory.getLogger(DynamicRedisPropertySource.class);

    /**
     * PropertySource 名称常量
     */
    public static final String PROPERTY_SOURCE_NAME = "dynamicRedisPropertySource";

    public DynamicRedisPropertySource() {
        super(PROPERTY_SOURCE_NAME, new ConcurrentHashMap<>());
    }

    public DynamicRedisPropertySource(Map<String, ConfigValue> initialConfig) {
        super(PROPERTY_SOURCE_NAME, new ConcurrentHashMap<>(initialConfig));
    }

    @Override
    public Object getProperty(String name) {
        ConfigValue configValue = source.get(name);
        if (configValue == null) {
            return null;
        }

        try {
            return configValue.getValue();
        } catch (Exception e) {
            log.error("获取配置值失败: key={}", name, e);
            return configValue.getRawValue();
        }
    }

    /**
     * 更新配置值（线程安全）
     *
     * @param key            配置键
     * @param value          新值
     * @param configSource   配置来源
     * @param targetType     目标类型
     * @return 旧值（如果存在）
     */
    public ConfigValue updateProperty(String key, String value, ConfigValue.ConfigSource configSource, Class<?> targetType) {
        ConfigValue newConfigValue = new ConfigValue(key, value, targetType, configSource);
        ConfigValue oldValue = source.put(key, newConfigValue);

        if (oldValue == null) {
            log.info("新增动态配置: key={}, value={}, type={}, source={}",
                    key, value, targetType.getSimpleName(), configSource);
        } else {
            log.info("更新动态配置: key={}, oldValue={}, newValue={}, source={}",
                    key, oldValue.getRawValue(), value, configSource);
        }

        return oldValue;
    }

    /**
     * 批量更新配置
     */
    public void updateProperties(Map<String, ConfigValue> newConfig) {
        newConfig.forEach((key, value) ->
                updateProperty(key, value.getRawValue(), value.getSource(), value.getTargetType())
        );
    }

    /**
     * 移除配置
     */
    public ConfigValue removeProperty(String key) {
        ConfigValue removed = source.remove(key);
        if (removed != null) {
            log.info("移除动态配置: key={}, value={}", key, removed.getRawValue());
        }
        return removed;
    }

    /**
     * 检查配置是否存在
     */
    public boolean containsProperty(String key) {
        return source.containsKey(key);
    }

    /**
     * 获取配置值对象（包含元数据）
     */
    public ConfigValue getConfigValue(String key) {
        return source.get(key);
    }

    /**
     * 获取所有配置键
     */
    public String[] getPropertyNames() {
        return source.keySet().toArray(new String[0]);
    }

    /**
     * 清空所有配置
     */
    public void clear() {
        source.clear();
        log.info("清空所有动态配置");
    }

    /**
     * 获取配置数量
     */
    public int size() {
        return source.size();
    }
}
