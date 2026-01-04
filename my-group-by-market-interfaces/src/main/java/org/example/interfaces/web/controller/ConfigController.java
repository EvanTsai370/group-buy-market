package org.example.interfaces.web.controller;

import org.example.application.service.ConfigManagementService;
import org.example.common.api.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 配置管理接口
 * 提供配置的增删改查功能
 */
@RestController
@RequestMapping("/api/v1/gbm/config")
public class ConfigController {

    private static final Logger log = LoggerFactory.getLogger(ConfigController.class);

    private final ConfigManagementService configManagementService;

    public ConfigController(ConfigManagementService configManagementService) {
        this.configManagementService = configManagementService;
    }

    /**
     * 更新配置
     *
     * 示例:
     * curl -X POST "http://localhost:8091/api/v1/gbm/config/update?key=order.timeout.seconds&value=300"
     * curl -X POST "http://localhost:8091/api/v1/gbm/config/update?key=activity.max.participants&value=100"
     */
    @PostMapping("/update")
    public Result<Boolean> updateConfig(@RequestParam String key,
                                         @RequestParam String value) {
        try {
            log.info("收到配置更新请求: key={}, value={}", key, value);
            configManagementService.updateConfig(key, value);
            return Result.success(true);
        } catch (IllegalArgumentException e) {
            log.warn("配置参数校验失败: {}", e.getMessage());
            return Result.failure("C001", e.getMessage());
        } catch (Exception e) {
            log.error("更新配置失败: key={}, value={}", key, value, e);
            return Result.failure("C999", "更新配置失败: " + e.getMessage());
        }
    }

    /**
     * 批量更新配置
     *
     * 示例:
     * curl -X POST "http://localhost:8091/api/v1/gbm/config/batch-update" \
     *   -H "Content-Type: application/json" \
     *   -d '{"order.timeout.seconds":"300","activity.max.participants":"100"}'
     */
    @PostMapping("/batch-update")
    public Result<Boolean> batchUpdate(@RequestBody Map<String, String> configs) {
        try {
            log.info("收到批量更新请求: {} 个配置项", configs.size());
            configManagementService.batchUpdateConfig(configs);
            return Result.success(true);
        } catch (IllegalArgumentException e) {
            log.warn("配置参数校验失败: {}", e.getMessage());
            return Result.failure("C001", e.getMessage());
        } catch (Exception e) {
            log.error("批量更新配置失败", e);
            return Result.failure("C999", "批量更新配置失败: " + e.getMessage());
        }
    }

    /**
     * 删除配置
     *
     * 示例:
     * curl -X DELETE "http://localhost:8091/api/v1/gbm/config/delete?key=test.config"
     */
    @DeleteMapping("/delete")
    public Result<Boolean> deleteConfig(@RequestParam String key) {
        try {
            log.info("收到删除配置请求: key={}", key);
            configManagementService.deleteConfig(key);
            return Result.success(true);
        } catch (Exception e) {
            log.error("删除配置失败: key={}", key, e);
            return Result.failure("C999", "删除配置失败: " + e.getMessage());
        }
    }

    /**
     * 获取配置值
     *
     * 示例:
     * curl "http://localhost:8091/api/v1/gbm/config/get?key=order.timeout.seconds"
     */
    @GetMapping("/get")
    public Result<String> getConfig(@RequestParam String key) {
        try {
            String value = configManagementService.getConfig(key, null);
            if (value == null) {
                return Result.failure("C002", "配置不存在: " + key);
            }
            return Result.success(value);
        } catch (Exception e) {
            log.error("获取配置失败: key={}", key, e);
            return Result.failure("C999", "获取配置失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有配置
     *
     * 示例:
     * curl "http://localhost:8091/api/v1/gbm/config/all"
     */
    @GetMapping("/all")
    public Result<Map<String, String>> getAllConfigs() {
        try {
            Map<String, String> configs = configManagementService.getAllConfigs();
            return Result.success(configs);
        } catch (Exception e) {
            log.error("获取所有配置失败", e);
            return Result.failure("C999", "获取所有配置失败: " + e.getMessage());
        }
    }

    /**
     * 获取常用配置列表（说明文档）
     *
     * 示例:
     * curl "http://localhost:8091/api/v1/gbm/config/common"
     */
    @GetMapping("/common")
    public Result<Map<String, String>> getCommonConfigs() {
        Map<String, String> commonConfigs = new HashMap<>();
        commonConfigs.put("order.timeout.seconds", "订单超时时间（秒），范围: 60-3600");
        commonConfigs.put("activity.max.participants", "活动最大参与人数，范围: 2-1000");
        commonConfigs.put("trial.degrade.switch", "拼团试算降级开关（true/false）");
        commonConfigs.put("cache.refresh.interval", "缓存刷新间隔（秒）");
        return Result.success(commonConfigs);
    }
}
