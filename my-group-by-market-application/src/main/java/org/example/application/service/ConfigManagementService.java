package org.example.application.service;

import org.example.domain.repository.IConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 配置管理应用服务
 * 负责配置管理的用例编排和业务校验
 */
@Service
public class ConfigManagementService {

    private static final Logger log = LoggerFactory.getLogger(ConfigManagementService.class);

    private final IConfigRepository configRepository;

    public ConfigManagementService(IConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    /**
     * 更新配置
     * 包含业务校验和审计
     */
    public void updateConfig(String key, String value) {
        log.info("更新配置: key={}, value={}", key, value);

        // 业务校验
        validateConfigKey(key);
        validateConfigValue(key, value);

        // 执行更新
        configRepository.save(key, value);

        // 审计日志
        auditConfigChange(key, value);
    }

    /**
     * 批量更新配置
     */
    public void batchUpdateConfig(Map<String, String> configs) {
        log.info("批量更新配置: {} 个配置项", configs.size());

        // 批量校验
        configs.forEach(this::validateConfigKey);

        // 执行批量更新
        configRepository.batchSave(configs);

        // 审计日志
        configs.forEach(this::auditConfigChange);
    }

    /**
     * 删除配置
     */
    public void deleteConfig(String key) {
        log.info("删除配置: key={}", key);
        configRepository.remove(key);
        auditConfigDeletion(key);
    }

    /**
     * 获取配置值
     */
    public String getConfig(String key, String defaultValue) {
        return configRepository.findByKey(key).orElse(defaultValue);
    }

    /**
     * 获取配置值（带类型转换）
     */
    public <T> T getConfig(String key, Class<T> targetType, T defaultValue) {
        return configRepository.findByKey(key, targetType, defaultValue);
    }

    /**
     * 获取所有配置
     */
    public Map<String, String> getAllConfigs() {
        return configRepository.findAll();
    }

    /**
     * 配置键校验
     * 业务规则：配置键必须符合命名规范
     */
    private void validateConfigKey(String key, String value) {
        validateConfigKey(key);
    }

    private void validateConfigKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("配置键不能为空");
        }

        // 业务规则：配置键必须使用小写字母和点号
        if (!key.matches("^[a-z][a-z0-9.]*[a-z0-9]$")) {
            throw new IllegalArgumentException(
                    "配置键格式错误，必须使用小写字母、数字和点号，示例: order.timeout.seconds"
            );
        }
    }

    /**
     * 配置值校验
     * 业务规则：针对特定配置的值进行范围校验
     */
    private void validateConfigValue(String key, String value) {
        if (value == null) {
            throw new IllegalArgumentException("配置值不能为空");
        }

        // 针对特定配置的业务校验
        switch (key) {
            case "order.timeout.seconds":
                validateTimeoutValue(value);
                break;
            case "activity.max.participants":
                validateMaxParticipants(value);
                break;
            // 可以继续添加其他配置的校验规则
        }
    }

    /**
     * 超时时间校验
     */
    private void validateTimeoutValue(String value) {
        try {
            int timeout = Integer.parseInt(value);
            if (timeout < 60 || timeout > 3600) {
                throw new IllegalArgumentException("订单超时时间必须在 60-3600 秒之间");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("订单超时时间必须是整数");
        }
    }

    /**
     * 最大参与人数校验
     */
    private void validateMaxParticipants(String value) {
        try {
            int max = Integer.parseInt(value);
            if (max < 2 || max > 1000) {
                throw new IllegalArgumentException("活动最大参与人数必须在 2-1000 之间");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("活动最大参与人数必须是整数");
        }
    }

    /**
     * 审计配置变更
     */
    private void auditConfigChange(String key, String value) {
        // TODO: 记录配置变更审计日志到数据库或日志系统
        log.info("审计: 配置已更新 - key={}, value={}", key, value);
    }

    /**
     * 审计配置删除
     */
    private void auditConfigDeletion(String key) {
        // TODO: 记录配置删除审计日志
        log.info("审计: 配置已删除 - key={}", key);
    }
}
