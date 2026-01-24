package org.example.application.service.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.application.service.admin.result.SystemInfoResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 系统设置服务
 * 
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminSettingsService {

    @Value("${spring.application.name:group-buy-market}")
    private String applicationName;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Value("${server.port:8080}")
    private int serverPort;

    // 模拟配置存储（实际应使用数据库或配置中心）
    private final Map<String, String> configStore = new HashMap<>();

    /**
     * 获取系统信息
     */
    public SystemInfoResult getSystemInfo() {
        log.info("【AdminSettings】获取系统信息");

        return SystemInfoResult.builder()
                .applicationName(applicationName)
                .activeProfile(activeProfile)
                .serverPort(serverPort)
                .javaVersion(System.getProperty("java.version"))
                .osName(System.getProperty("os.name"))
                .osVersion(System.getProperty("os.version"))
                .startTime(LocalDateTime.now()) // 简化：实际应记录应用启动时间
                .maxMemory(Runtime.getRuntime().maxMemory() / 1024 / 1024 + " MB")
                .totalMemory(Runtime.getRuntime().totalMemory() / 1024 / 1024 + " MB")
                .freeMemory(Runtime.getRuntime().freeMemory() / 1024 / 1024 + " MB")
                .build();
    }

    /**
     * 获取所有配置
     */
    public Map<String, String> getAllConfigs() {
        log.info("【AdminSettings】获取所有配置");

        Map<String, String> configs = new HashMap<>(configStore);

        // 添加默认配置
        configs.putIfAbsent("order.timeout.minutes", "30");
        configs.putIfAbsent("payment.callback.secret", "****");
        configs.putIfAbsent("redis.cache.ttl.seconds", "3600");
        configs.putIfAbsent("rabbitmq.retry.max.count", "3");

        return configs;
    }

    /**
     * 更新配置
     */
    public void updateConfig(String key, String value) {
        log.info("【AdminSettings】更新配置, key: {}, value: {}", key, maskSensitive(key, value));

        configStore.put(key, value);

        log.info("【AdminSettings】配置已更新, key: {}", key);
    }

    /**
     * 获取单个配置
     */
    public String getConfig(String key) {
        log.info("【AdminSettings】获取配置, key: {}", key);
        return configStore.get(key);
    }

    /**
     * 删除配置
     */
    public void deleteConfig(String key) {
        log.info("【AdminSettings】删除配置, key: {}", key);
        configStore.remove(key);
    }

    /**
     * 掩盖敏感配置值
     */
    private String maskSensitive(String key, String value) {
        if (key.toLowerCase().contains("secret") ||
                key.toLowerCase().contains("password") ||
                key.toLowerCase().contains("key")) {
            return "****";
        }
        return value;
    }
}
