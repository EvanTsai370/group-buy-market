package org.example.domain.repository;

import java.util.Map;
import java.util.Optional;

/**
 * 配置仓储接口（领域契约）
 * 定义配置管理的领域能力
 */
public interface IConfigRepository {

    /**
     * 获取配置值
     *
     * @param key 配置键
     * @return 配置值（可能为空）
     */
    Optional<String> findByKey(String key);

    /**
     * 获取配置值（带类型转换）
     *
     * @param key          配置键
     * @param targetType   目标类型
     * @param defaultValue 默认值
     * @param <T>          类型参数
     * @return 配置值
     */
    <T> T findByKey(String key, Class<T> targetType, T defaultValue);

    /**
     * 更新配置
     *
     * @param key   配置键
     * @param value 配置值
     */
    void save(String key, String value);

    /**
     * 批量更新配置
     *
     * @param configs 配置集合
     */
    void batchSave(Map<String, String> configs);

    /**
     * 删除配置
     *
     * @param key 配置键
     */
    void remove(String key);

    /**
     * 获取所有配置
     *
     * @return 配置集合
     */
    Map<String, String> findAll();
}
