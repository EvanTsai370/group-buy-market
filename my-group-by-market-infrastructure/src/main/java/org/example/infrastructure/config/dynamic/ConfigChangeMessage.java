package org.example.infrastructure.config.dynamic;

/**
 * 配置变更消息
 * 用于 Redis Pub/Sub 传输
 */
public class ConfigChangeMessage {

    /**
     * 配置键
     */
    private String key;

    /**
     * 配置值
     */
    private String value;

    /**
     * 目标类型名称（完全限定名）
     */
    private String targetType;

    /**
     * 操作类型：UPDATE, DELETE
     */
    private OperationType operation;

    public ConfigChangeMessage() {
    }

    public ConfigChangeMessage(String key, String value, String targetType, OperationType operation) {
        this.key = key;
        this.value = value;
        this.targetType = targetType;
        this.operation = operation;
    }

    public static ConfigChangeMessage updateMessage(String key, String value, Class<?> targetType) {
        return new ConfigChangeMessage(key, value, targetType.getName(), OperationType.UPDATE);
    }

    public static ConfigChangeMessage deleteMessage(String key) {
        return new ConfigChangeMessage(key, null, null, OperationType.DELETE);
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public OperationType getOperation() {
        return operation;
    }

    public void setOperation(OperationType operation) {
        this.operation = operation;
    }

    /**
     * 获取目标类型的 Class 对象
     */
    public Class<?> getTargetClass() {
        if (targetType == null || targetType.isEmpty()) {
            return String.class;
        }

        try {
            return Class.forName(targetType);
        } catch (ClassNotFoundException e) {
            return String.class;
        }
    }

    @Override
    public String toString() {
        return "ConfigChangeMessage{" +
               "key='" + key + '\'' +
               ", value='" + value + '\'' +
               ", targetType='" + targetType + '\'' +
               ", operation=" + operation +
               '}';
    }

    /**
     * 操作类型枚举
     */
    public enum OperationType {
        /**
         * 更新配置
         */
        UPDATE,

        /**
         * 删除配置
         */
        DELETE
    }
}
