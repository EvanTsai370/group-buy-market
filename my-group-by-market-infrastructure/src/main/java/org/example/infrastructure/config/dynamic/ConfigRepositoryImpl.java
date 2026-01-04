package org.example.infrastructure.config.dynamic;

import org.example.domain.repository.IConfigRepository;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;

/**
 * 配置仓储实现
 * 桥接 DynamicConfigService 和领域层接口
 */
@Repository
public class ConfigRepositoryImpl implements IConfigRepository {

    private final DynamicConfigService dynamicConfigService;
    private final Environment environment;

    public ConfigRepositoryImpl(DynamicConfigService dynamicConfigService,
                                 Environment environment) {
        this.dynamicConfigService = dynamicConfigService;
        this.environment = environment;
    }

    @Override
    public Optional<String> findByKey(String key) {
        String value = environment.getProperty(key);
        return Optional.ofNullable(value);
    }

    @Override
    public <T> T findByKey(String key, Class<T> targetType, T defaultValue) {
        return environment.getProperty(key, targetType, defaultValue);
    }

    @Override
    public void save(String key, String value) {
        dynamicConfigService.updateConfig(key, value);
    }

    @Override
    public void batchSave(Map<String, String> configs) {
        dynamicConfigService.updateConfigs(configs);
    }

    @Override
    public void remove(String key) {
        dynamicConfigService.deleteConfig(key);
    }

    @Override
    public Map<String, String> findAll() {
        return dynamicConfigService.getAllConfigs();
    }
}
