package org.example.infrastructure.config.dynamic;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 配置刷新管理器
 * 负责配置的加载、刷新、持久化和事件发布
 * 线程安全：使用读写锁保护配置更新操作
 */
public class PropertyRefreshManager {

    private static final Logger log = LoggerFactory.getLogger(PropertyRefreshManager.class);

    /**
     * 本地快照文件路径
     */
    private static final String SNAPSHOT_FILE = "config-snapshot.json";

    private final DynamicRedisPropertySource propertySource;
    private final StringRedisTemplate redisTemplate;
    private final ConfigurableEnvironment environment;
    private final ApplicationEventPublisher eventPublisher;
    private final String redisKeyPrefix;

    /**
     * 读写锁：保护配置更新操作的线程安全
     */
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public PropertyRefreshManager(DynamicRedisPropertySource propertySource,
            StringRedisTemplate redisTemplate,
            ConfigurableEnvironment environment,
            ApplicationEventPublisher eventPublisher,
            String redisKeyPrefix) {
        this.propertySource = propertySource;
        this.redisTemplate = redisTemplate;
        this.environment = environment;
        this.eventPublisher = eventPublisher;
        this.redisKeyPrefix = redisKeyPrefix;
    }

    /**
     * 从 Redis 加载配置（带本地快照 fallback）
     */
    public void loadConfigFromRedis() {
        lock.writeLock().lock();
        try {
            Map<String, String> redisConfig = loadFromRedis();

            if (redisConfig.isEmpty()) {
                log.warn("Redis 中没有配置数据，尝试从本地快照加载");
                Map<String, String> snapshot = loadFromSnapshot();
                if (!snapshot.isEmpty()) {
                    redisConfig = snapshot;
                    // 恢复到 Redis
                    restoreToRedis(redisConfig);
                }
            }

            // 更新 PropertySource
            for (Map.Entry<String, String> entry : redisConfig.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                Class<?> targetType = inferType(value);

                propertySource.updateProperty(key, value, ConfigValue.ConfigSource.REDIS, targetType);
            }

            // 保存快照
            if (!redisConfig.isEmpty()) {
                saveSnapshot(redisConfig);
            }

            log.info("配置加载完成，共 {} 个配置项", redisConfig.size());

        } catch (Exception e) {
            log.error("加载配置失败", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 刷新单个配置项
     *
     * @param key        配置键
     * @param newValue   新值
     * @param targetType 目标类型
     */
    public void refreshProperty(String key, String newValue, Class<?> targetType) {
        lock.writeLock().lock();
        try {
            // 1. 获取旧值
            ConfigValue oldConfigValue = propertySource.getConfigValue(key);
            String oldValue = oldConfigValue != null ? oldConfigValue.getRawValue() : null;

            // 2. 校验新值（如果有校验器）
            validateValue(key, newValue, targetType);

            // 3. 更新 PropertySource
            propertySource.updateProperty(key, newValue, ConfigValue.ConfigSource.REDIS, targetType);

            // 4. 更新 Redis
            String redisKey = buildRedisKey(key);
            redisTemplate.opsForValue().set(redisKey, newValue);

            // 5. 更新本地快照
            updateSnapshot(key, newValue);

            // 6. 发布配置变更事件
            ConfigRefreshEvent event = new ConfigRefreshEvent(this, key, oldValue, newValue, targetType);
            eventPublisher.publishEvent(event);

            log.info("配置刷新成功: key={}, oldValue={}, newValue={}", key, oldValue, newValue);

        } catch (Exception e) {
            log.error("配置刷新失败: key={}, newValue={}", key, newValue, e);
            throw new ConfigRefreshException("配置刷新失败: " + key, e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 批量刷新配置
     */
    public void refreshProperties(Map<String, String> newConfig) {
        lock.writeLock().lock();
        try {
            for (Map.Entry<String, String> entry : newConfig.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                Class<?> targetType = inferType(value);
                refreshProperty(key, value, targetType);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 从 Redis 加载配置
     */
    private Map<String, String> loadFromRedis() {
        Map<String, String> config = new HashMap<>();
        try {
            String pattern = redisKeyPrefix + "*";
            var keys = redisTemplate.keys(pattern);

            if (keys != null && !keys.isEmpty()) {
                for (String redisKey : keys) {
                    String value = redisTemplate.opsForValue().get(redisKey);
                    if (value != null) {
                        String configKey = extractConfigKey(redisKey);
                        config.put(configKey, value);
                    }
                }
            }

            log.info("从 Redis 加载配置: {} 个配置项", config.size());
        } catch (Exception e) {
            log.error("从 Redis 加载配置失败", e);
        }
        return config;
    }

    /**
     * 从本地快照加载配置
     */
    private Map<String, String> loadFromSnapshot() {
        try {
            Path snapshotPath = Paths.get(SNAPSHOT_FILE);
            if (Files.exists(snapshotPath)) {
                String json = Files.readString(snapshotPath);
                Map<String, String> snapshot = JSON.parseObject(json, Map.class);
                log.info("从本地快照加载配置: {} 个配置项", snapshot.size());
                return snapshot;
            }
        } catch (IOException e) {
            log.error("读取本地快照失败", e);
        }
        return new HashMap<>();
    }

    /**
     * 保存配置快照到本地文件
     */
    private void saveSnapshot(Map<String, String> config) {
        try {
            Path snapshotPath = Paths.get(SNAPSHOT_FILE);
            String json = JSON.toJSONString(config, true);
            Files.writeString(snapshotPath, json);
            log.debug("配置快照已保存");
        } catch (IOException e) {
            log.error("保存配置快照失败", e);
        }
    }

    /**
     * 更新本地快照中的单个配置
     */
    private void updateSnapshot(String key, String value) {
        try {
            Map<String, String> snapshot = loadFromSnapshot();
            snapshot.put(key, value);
            saveSnapshot(snapshot);
        } catch (Exception e) {
            log.error("更新本地快照失败: key={}", key, e);
        }
    }

    /**
     * 恢复配置到 Redis
     */
    private void restoreToRedis(Map<String, String> config) {
        for (Map.Entry<String, String> entry : config.entrySet()) {
            String redisKey = buildRedisKey(entry.getKey());
            redisTemplate.opsForValue().set(redisKey, entry.getValue());
        }
        log.info("配置已恢复到 Redis: {} 个配置项", config.size());
    }

    /**
     * 构建 Redis Key
     */
    private String buildRedisKey(String configKey) {
        return redisKeyPrefix + configKey;
    }

    /**
     * 从 Redis Key 提取配置键
     */
    private String extractConfigKey(String redisKey) {
        return redisKey.substring(redisKeyPrefix.length());
    }

    /**
     * 推断值的类型
     */
    private Class<?> inferType(String value) {
        if (value == null) {
            return String.class;
        }

        // 尝试推断类型
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.class;
        }

        try {
            Integer.parseInt(value);
            return Integer.class;
        } catch (NumberFormatException ignored) {
        }

        try {
            Long.parseLong(value);
            return Long.class;
        } catch (NumberFormatException ignored) {
        }

        try {
            Double.parseDouble(value);
            return Double.class;
        } catch (NumberFormatException ignored) {
        }

        return String.class;
    }

    /**
     * 获取 PropertySource（供外部访问所有配置）
     */
    public DynamicRedisPropertySource getPropertySource() {
        return propertySource;
    }

    /**
     * 配置值校验（可扩展）
     */
    private void validateValue(String key, String value, Class<?> targetType) {
        // 基本校验：确保能成功转换
        try {
            TypeConverter.convert(value, targetType);
        } catch (Exception e) {
            throw new ConfigValidationException("配置值校验失败: key=" + key + ", value=" + value, e);
        }

        // 这里可以添加自定义校验逻辑，例如：
        // - 范围校验（数值类型）
        // - 正则校验（字符串类型）
        // - 枚举值校验
    }

    /**
     * 配置刷新异常
     */
    public static class ConfigRefreshException extends RuntimeException {
        public ConfigRefreshException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * 配置校验异常
     */
    public static class ConfigValidationException extends RuntimeException {
        public ConfigValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
