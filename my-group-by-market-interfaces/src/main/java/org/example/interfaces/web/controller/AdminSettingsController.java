package org.example.interfaces.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.application.service.admin.AdminSettingsService;
import org.example.application.service.admin.AdminSettingsService.SystemInfo;
import org.example.common.api.Result;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 系统设置控制器
 * 
 * @author 开发团队
 * @since 2026-01-10
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
@Tag(name = "系统设置", description = "管理后台系统设置接口")
public class AdminSettingsController {

    private final AdminSettingsService adminSettingsService;

    @GetMapping("/system")
    @Operation(summary = "系统信息", description = "获取系统运行信息")
    public Result<SystemInfo> getSystemInfo() {
        log.info("【AdminSettings】获取系统信息");
        SystemInfo info = adminSettingsService.getSystemInfo();
        return Result.success(info);
    }

    @GetMapping("/config")
    @Operation(summary = "配置列表", description = "获取所有配置项")
    public Result<Map<String, String>> getAllConfigs() {
        log.info("【AdminSettings】获取配置列表");
        Map<String, String> configs = adminSettingsService.getAllConfigs();
        return Result.success(configs);
    }

    @GetMapping("/config/{key}")
    @Operation(summary = "获取配置", description = "获取单个配置项")
    public Result<String> getConfig(@PathVariable String key) {
        log.info("【AdminSettings】获取配置, key: {}", key);
        String value = adminSettingsService.getConfig(key);
        return Result.success(value);
    }

    @PutMapping("/config/{key}")
    @Operation(summary = "更新配置", description = "更新配置项")
    public Result<String> updateConfig(
            @PathVariable String key,
            @RequestBody String value) {
        log.info("【AdminSettings】更新配置, key: {}", key);
        adminSettingsService.updateConfig(key, value);
        return Result.success("配置已更新");
    }

    @DeleteMapping("/config/{key}")
    @Operation(summary = "删除配置", description = "删除配置项")
    public Result<String> deleteConfig(@PathVariable String key) {
        log.info("【AdminSettings】删除配置, key: {}", key);
        adminSettingsService.deleteConfig(key);
        return Result.success("配置已删除");
    }
}
