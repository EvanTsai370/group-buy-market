package org.example.interfaces.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.application.service.admin.AdminStatisticsService;
import org.example.application.service.admin.AdminStatisticsService.*;
import org.example.application.service.admin.AdminUserService;
import org.example.common.api.Result;
import org.example.domain.model.user.User;
import org.example.domain.model.user.valueobject.UserRole;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理后台控制器
 * 
 * @author 开发团队
 * @since 2026-01-10
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "管理后台", description = "管理后台接口")
public class AdminDashboardController {

    private final AdminStatisticsService adminStatisticsService;
    private final AdminUserService adminUserService;

    // ============== 仪表盘 ==============

    @GetMapping("/dashboard")
    @Operation(summary = "仪表盘概览", description = "获取系统概览数据")
    public Result<DashboardOverview> getDashboard() {
        log.info("【AdminDashboard】获取仪表盘概览");
        DashboardOverview overview = adminStatisticsService.getDashboardOverview();
        return Result.success(overview);
    }

    @GetMapping("/statistics/users")
    @Operation(summary = "用户统计", description = "获取用户统计数据")
    public Result<UserStatistics> getUserStatistics() {
        log.info("【AdminDashboard】获取用户统计");
        UserStatistics statistics = adminStatisticsService.getUserStatistics();
        return Result.success(statistics);
    }

    @GetMapping("/statistics/goods")
    @Operation(summary = "商品统计", description = "获取商品统计数据")
    public Result<GoodsStatistics> getGoodsStatistics() {
        log.info("【AdminDashboard】获取商品统计");
        GoodsStatistics statistics = adminStatisticsService.getGoodsStatistics();
        return Result.success(statistics);
    }

    // ============== 用户管理 ==============

    @GetMapping("/users")
    @Operation(summary = "用户列表", description = "分页查询用户列表")
    public Result<List<User>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("【AdminDashboard】查询用户列表, page: {}, size: {}", page, size);
        List<User> users = adminUserService.listUsers(page, size);
        return Result.success(users);
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "用户详情", description = "获取用户详情")
    public Result<User> getUserDetail(@PathVariable String userId) {
        log.info("【AdminDashboard】查询用户详情, userId: {}", userId);
        User user = adminUserService.getUserDetail(userId);
        return Result.success(user);
    }

    @PostMapping("/users/{userId}/disable")
    @Operation(summary = "禁用用户", description = "禁用指定用户")
    public Result<Void> disableUser(@PathVariable String userId) {
        log.info("【AdminDashboard】禁用用户, userId: {}", userId);
        adminUserService.disableUser(userId);
        return Result.success();
    }

    @PostMapping("/users/{userId}/enable")
    @Operation(summary = "启用用户", description = "启用指定用户")
    public Result<Void> enableUser(@PathVariable String userId) {
        log.info("【AdminDashboard】启用用户, userId: {}", userId);
        adminUserService.enableUser(userId);
        return Result.success();
    }

    @PutMapping("/users/{userId}/role")
    @Operation(summary = "修改角色", description = "修改用户角色")
    public Result<Void> setUserRole(
            @PathVariable String userId,
            @RequestParam UserRole role) {
        log.info("【AdminDashboard】修改用户角色, userId: {}, role: {}", userId, role);
        adminUserService.setUserRole(userId, role);
        return Result.success();
    }

    @PostMapping("/users/{userId}/reset-password")
    @Operation(summary = "重置密码", description = "重置用户密码")
    public Result<String> resetPassword(
            @PathVariable String userId,
            @RequestParam String newPassword) {
        log.info("【AdminDashboard】重置用户密码, userId: {}", userId);
        String result = adminUserService.resetPassword(userId, newPassword);
        return Result.success(result);
    }
}
