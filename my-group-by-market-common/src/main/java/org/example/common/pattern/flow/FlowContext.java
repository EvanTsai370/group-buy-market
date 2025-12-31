package org.example.common.pattern.flow;

import java.util.HashMap;
import java.util.Map;

/**
 * 流程上下文基类
 * 提供通用的上下文数据存储能力
 */
public class FlowContext {

    /**
     * 上下文数据存储
     */
    private final Map<String, Object> contextData = new HashMap<>();

    /**
     * 存储数据到上下文
     *
     * @param key 键
     * @param value 值
     */
    public void putData(String key, Object value) {
        contextData.put(key, value);
    }

    /**
     * 从上下文获取数据
     *
     * @param key 键
     * @param <T> 数据类型
     * @return 值
     */
    @SuppressWarnings("unchecked")
    public <T> T getData(String key) {
        return (T) contextData.get(key);
    }

    /**
     * 从上下文获取数据（带默认值）
     *
     * @param key 键
     * @param defaultValue 默认值
     * @param <T> 数据类型
     * @return 值
     */
    @SuppressWarnings("unchecked")
    public <T> T getData(String key, T defaultValue) {
        return (T) contextData.getOrDefault(key, defaultValue);
    }

    /**
     * 清空上下文数据
     */
    public void clear() {
        contextData.clear();
    }
}