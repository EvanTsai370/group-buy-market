package org.example.infrastructure.config.dynamic;

import org.springframework.context.ApplicationEvent;

/**
 * 配置刷新事件
 * 当配置发生变更时发布此事件
 */
public class ConfigRefreshEvent extends ApplicationEvent {

    /**
     * 配置键
     */
    private final String key;

    /**
     * 旧值
     */
    private final String oldValue;

    /**
     * 新值
     */
    private final String newValue;

    /**
     * 目标类型
     */
    private final Class<?> targetType;

    public ConfigRefreshEvent(Object source, String key, String oldValue, String newValue, Class<?> targetType) {
        super(source);
        this.key = key;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.targetType = targetType;
    }

    public String getKey() {
        return key;
    }

    public String getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public Class<?> getTargetType() {
        return targetType;
    }

    /**
     * 判断配置是否真正发生了变化
     */
    public boolean hasChanged() {
        if (oldValue == null && newValue == null) {
            return false;
        }
        if (oldValue == null || newValue == null) {
            return true;
        }
        return !oldValue.equals(newValue);
    }

    @Override
    public String toString() {
        return "ConfigRefreshEvent{" +
               "key='" + key + '\'' +
               ", oldValue='" + oldValue + '\'' +
               ", newValue='" + newValue + '\'' +
               ", targetType=" + targetType.getSimpleName() +
               '}';
    }
}
