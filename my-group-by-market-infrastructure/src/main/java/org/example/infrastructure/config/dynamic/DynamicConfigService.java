package org.example.infrastructure.config.dynamic;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * 动态配置服务（业务层 Facade）
 * 提供配置的读取和更新接口
 */
public class DynamicConfigService {

    private static final Logger log = LoggerFactory.getLogger(DynamicConfigService.class);

    private final PropertyRefreshManager refreshManager;
    private final StringRedisTemplate redisTemplate;
    private final String redisTopic;

    public DynamicConfigService(PropertyRefreshManager refreshManager,
            StringRedisTemplate redisTemplate,
            String redisTopic) {
        this.refreshManager = refreshManager;
        this.redisTemplate = redisTemplate;
        this.redisTopic = redisTopic;
    }

    /**
     * 更新配置并广播到所有节点
     *
     * @param key        配置键
     * @param value      配置值
     * @param targetType 目标类型
     */
    public void updateConfig(String key, String value, Class<?> targetType) {
        log.info("更新配置: key={}, value={}, type={}", key, value, targetType.getSimpleName());

        // 1. 本地刷新配置
        refreshManager.refreshProperty(key, value, targetType);

        // 2. 广播到其他节点
        broadcastConfigChange(key, value, targetType);
    }

    /**
     * 更新配置（自动推断类型）
     */
    public void updateConfig(String key, String value) {
        Class<?> targetType = inferType(value);
        updateConfig(key, value, targetType);
    }

    /**
     * 批量更新配置
     */
    public void updateConfigs(Map<String, String> configs) {
        log.info("批量更新配置: {} 个配置项", configs.size());
        for (Map.Entry<String, String> entry : configs.entrySet()) {
            updateConfig(entry.getKey(), entry.getValue());
        }
    }

    /**
     * 删除配置
     */
    public void deleteConfig(String key) {
        log.info("删除配置: key={}", key);

        // 广播删除消息
        ConfigChangeMessage message = ConfigChangeMessage.deleteMessage(key);
        String json = JSON.toJSONString(message);
        redisTemplate.convertAndSend(redisTopic, json);
    }

    /**
     * 获取所有配置
     */
    public Map<String, String> getAllConfigs() {
        Map<String, String> configs = new HashMap<>();

        // 从 PropertySource 获取所有配置
        DynamicRedisPropertySource propertySource = refreshManager.getPropertySource();
        String[] propertyNames = propertySource.getPropertyNames();

        for (String key : propertyNames) {
            ConfigValue configValue = propertySource.getConfigValue(key);
            if (configValue != null) {
                configs.put(key, configValue.getRawValue());
            }
        }

        log.debug("获取所有配置: {} 个配置项", configs.size());
        return configs;
    }

    /**
     * 广播配置变更消息
     */
    private void broadcastConfigChange(String key, String value, Class<?> targetType) {
        try {
            ConfigChangeMessage message = ConfigChangeMessage.updateMessage(key, value, targetType);
            String json = JSON.toJSONString(message);

            redisTemplate.convertAndSend(redisTopic, json);
            log.debug("配置变更消息已广播: key={}", key);

        } catch (Exception e) {
            log.error("广播配置变更消息失败: key={}", key, e);
        }
    }

    /**
     * 推断值的类型
     */
    private Class<?> inferType(String value) {
        if (value == null) {
            return String.class;
        }

        // 布尔值
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.class;
        }

        // 整数
        try {
            Integer.parseInt(value);
            return Integer.class;
        } catch (NumberFormatException ignored) {
        }

        // 长整数
        try {
            Long.parseLong(value);
            return Long.class;
        } catch (NumberFormatException ignored) {
        }

        // 浮点数
        try {
            Double.parseDouble(value);
            return Double.class;
        } catch (NumberFormatException ignored) {
        }

        return String.class;
    }
}
