package org.example.infrastructure.config.dynamic.example;

import org.example.infrastructure.config.dynamic.ConfigRefreshEvent;
import org.example.infrastructure.config.dynamic.DynamicConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 动态配置使用示例
 * 演示三种配置读取方式
 */
@Component
public class DynamicConfigExample {

    private static final Logger log = LoggerFactory.getLogger(DynamicConfigExample.class);

    private final Environment environment;
    private final DynamicConfigService configService;

    /**
     * 方式1: 使用 @Value 注解（不支持动态刷新）
     * 注意：@Value 注入的值在 Bean 初始化后不会自动更新
     */
    @Value("${business.switch:false}")
    private boolean businessSwitch;

    public DynamicConfigExample(Environment environment, DynamicConfigService configService) {
        this.environment = environment;
        this.configService = configService;
    }

    /**
     * 方式2: 使用 Environment 读取（支持动态刷新）
     * 推荐：每次都从 Environment 读取最新值
     */
    public boolean getBusinessSwitch() {
        return environment.getProperty("business.switch", Boolean.class, false);
    }

    /**
     * 方式3: 监听配置变更事件
     * 当配置变更时，自动执行业务逻辑
     */
    @EventListener
    public void onConfigRefresh(ConfigRefreshEvent event) {
        if ("business.switch".equals(event.getKey()) && event.hasChanged()) {
            log.info("业务开关配置发生变更: {} -> {}",
                    event.getOldValue(), event.getNewValue());

            // 执行业务逻辑（例如：刷新缓存、重新初始化组件等）
            handleBusinessSwitchChange(Boolean.parseBoolean(event.getNewValue()));
        }
    }

    /**
     * 示例：更新配置
     */
    public void updateConfigExample() {
        // 更新配置（会广播到所有节点）
        configService.updateConfig("business.switch", "true", Boolean.class);

        // 或使用自动类型推断
        configService.updateConfig("business.timeout", "3000");
    }

    /**
     * 示例：读取配置的最佳实践
     */
    public void bestPracticeExample() {
        //  推荐：从 Environment 读取（实时获取最新值）
        boolean switchValue = environment.getProperty("business.switch", Boolean.class, false);
        int timeout = environment.getProperty("business.timeout", Integer.class, 1000);

        log.info("当前配置: switch={}, timeout={}", switchValue, timeout);

        // ❌ 不推荐：使用 @Value 注入的字段（不会自动更新）
        // boolean oldValue = this.businessSwitch; // 这个值可能是旧的
    }

    /**
     * 处理业务开关变更
     */
    private void handleBusinessSwitchChange(boolean newValue) {
        if (newValue) {
            log.info("业务功能已开启");
            // 开启业务逻辑
        } else {
            log.info("业务功能已关闭");
            // 关闭业务逻辑
        }
    }
}
