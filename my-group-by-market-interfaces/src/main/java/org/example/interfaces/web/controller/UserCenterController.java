package org.example.interfaces.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.application.service.customer.UserCenterService;
import org.example.application.service.customer.result.UserOrderResult;
import org.example.application.service.customer.result.UserProfileResult;
import org.example.common.api.Result;
import org.example.interfaces.web.assembler.UserCenterAssembler;
import org.example.interfaces.web.dto.customer.UserOrderResponse;
import org.example.interfaces.web.dto.customer.UserProfileResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户中心控制器
 * 
 * 提供给前端用户的个人中心接口
 * 
 * @author 开发团队
 * @since 2026-01-11
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "用户中心", description = "面向用户的个人中心接口")
public class UserCenterController {

    private final UserCenterService userCenterService;
    private final UserCenterAssembler userCenterAssembler;

    /**
     * 获取用户资料
     */
    @GetMapping("/profile")
    @Operation(summary = "用户资料", description = "获取当前用户的资料信息")
    public Result<UserProfileResponse> getProfile(@RequestHeader("X-User-Id") String userId) {
        log.info("【UserCenterController】获取用户资料, userId: {}", userId);

        UserProfileResult result = userCenterService.getUserProfile(userId);
        UserProfileResponse response = userCenterAssembler.toResponse(result);

        return Result.success(response);
    }

    /**
     * 获取用户订单列表
     */
    @GetMapping("/orders")
    @Operation(summary = "订单列表", description = "获取当前用户的订单列表（分页）")
    public Result<List<UserOrderResponse>> getOrders(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("【UserCenterController】获取用户订单, userId: {}, page: {}, size: {}", userId, page, size);

        List<UserOrderResult> results = userCenterService.getUserOrders(userId, page, size);
        List<UserOrderResponse> responses = userCenterAssembler.toOrderListResponse(results);

        return Result.success(responses);
    }
}
