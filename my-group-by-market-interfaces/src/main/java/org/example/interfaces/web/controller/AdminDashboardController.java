package org.example.interfaces.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.application.service.admin.AdminStatisticsService;
import org.example.application.service.admin.AdminUserService;
import org.example.application.service.admin.result.*;
import org.example.common.api.Result;
import org.example.domain.model.user.valueobject.UserRole;
import org.example.interfaces.web.assembler.AdminDashboardAssembler;
import org.example.interfaces.web.dto.admin.*;
import org.springframework.security.access.prepost.PreAuthorize;
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
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final AdminStatisticsService adminStatisticsService;
    private final AdminUserService adminUserService;
    private final AdminDashboardAssembler adminDashboardAssembler;

    // ============== 仪表盘 ==============

    @GetMapping("/dashboard")
    @Operation(summary = "仪表盘概览", description = "获取系统概览数据")
    public Result<DashboardOverviewResponse> getDashboard() {
        log.info("【AdminDashboard】获取仪表盘概览");
        DashboardOverviewResult result = adminStatisticsService.getDashboardOverview();
        return Result.success(adminDashboardAssembler.toDashboardOverviewResponse(result));
    }

    @GetMapping("/dashboard/stats")
    @Operation(summary = "仪表盘统计", description = "获取系统详细统计数据（今日数据、最近订单等）")
    public Result<DashboardOverviewResponse> getDashboardStats() {
        log.info("【AdminDashboard】获取仪表盘统计数据");
        DashboardOverviewResult result = adminStatisticsService.getDashboardOverview();
        return Result.success(adminDashboardAssembler.toDashboardOverviewResponse(result));
    }

    @GetMapping("/statistics/users")
    @Operation(summary = "用户统计", description = "获取用户统计数据")
    public Result<UserStatisticsResponse> getUserStatistics() {
        log.info("【AdminDashboard】获取用户统计");
        UserStatisticsResult result = adminStatisticsService.getUserStatistics();
        return Result.success(adminDashboardAssembler.toUserStatisticsResponse(result));
    }

    @GetMapping("/statistics/goods")
    @Operation(summary = "商品统计", description = "获取商品统计数据")
    public Result<GoodsStatisticsResponse> getGoodsStatistics() {
        log.info("【AdminDashboard】获取商品统计");
        GoodsStatisticsResult result = adminStatisticsService.getGoodsStatistics();
        return Result.success(adminDashboardAssembler.toGoodsStatisticsResponse(result));
    }

    // ============== 用户管理 ==============

    @GetMapping("/users/{userId}")
    @Operation(summary = "用户详情", description = "获取用户详情")
    public Result<UserDetailResponse> getUserDetail(@PathVariable String userId) {
        log.info("【AdminDashboard】查询用户详情, userId: {}", userId);
        UserDetailResult result = adminUserService.getUserDetail(userId);
        return Result.success(adminDashboardAssembler.toUserDetailResponse(result));
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
