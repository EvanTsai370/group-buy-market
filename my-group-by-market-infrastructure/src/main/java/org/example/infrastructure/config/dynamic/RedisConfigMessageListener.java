package org.example.infrastructure.config.dynamic;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

import java.nio.charset.StandardCharsets;

/**
 * Redis 配置变更消息监听器
 * 监听 Redis Pub/Sub 消息，触发配置刷新
 */
public class RedisConfigMessageListener implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(RedisConfigMessageListener.class);

    private final PropertyRefreshManager refreshManager;

    public RedisConfigMessageListener(PropertyRefreshManager refreshManager) {
        this.refreshManager = refreshManager;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
            String body = new String(message.getBody(), StandardCharsets.UTF_8);

            log.debug("收到配置变更消息: channel={}, body={}", channel, body);

            // 解析消息
            ConfigChangeMessage changeMessage = parseMessage(body);
            if (changeMessage == null) {
                log.warn("无法解析配置变更消息: {}", body);
                return;
            }

            // 处理配置变更
            handleConfigChange(changeMessage);

        } catch (Exception e) {
            log.error("处理配置变更消息失败", e);
        }
    }

    /**
     * 解析配置变更消息
     */
    private ConfigChangeMessage parseMessage(String body) {
        try {
            return JSON.parseObject(body, ConfigChangeMessage.class);
        } catch (Exception e) {
            log.error("解析配置变更消息失败: {}", body, e);
            return null;
        }
    }

    /**
     * 处理配置变更
     */
    private void handleConfigChange(ConfigChangeMessage message) {
        String key = message.getKey();
        ConfigChangeMessage.OperationType operation = message.getOperation();

        if (operation == ConfigChangeMessage.OperationType.UPDATE) {
            String value = message.getValue();
            Class<?> targetType = message.getTargetClass();

            log.info("处理配置更新: key={}, value={}, type={}", key, value, targetType.getSimpleName());
            refreshManager.refreshProperty(key, value, targetType);

        } else if (operation == ConfigChangeMessage.OperationType.DELETE) {
            log.info("处理配置删除: key={}", key);
            // 删除配置的逻辑可以在这里实现
            // refreshManager.removeProperty(key);
        }
    }
}
