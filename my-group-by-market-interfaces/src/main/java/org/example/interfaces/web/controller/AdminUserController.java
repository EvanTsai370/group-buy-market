package org.example.interfaces.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.application.service.admin.AdminUserService;
import org.example.application.service.admin.result.UserDetailResult;
import org.example.common.api.Result;
import org.example.interfaces.web.assembler.AdminUserAssembler;
import org.example.interfaces.web.dto.admin.CreateAdminRequest;
import org.example.interfaces.web.dto.admin.UserDetailResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员用户控制器
 * 
 * @author 开发团队
 * @since 2026-01-12
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "管理后台用户管理接口")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final AdminUserAssembler adminUserAssembler;

    @GetMapping
    @Operation(summary = "用户列表", description = "分页查询用户列表")
    public Result<org.example.common.model.PageResult<UserDetailResponse>> listUsers(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        log.info("【AdminUser】分页查询用户列表, page: {}, size: {}", page, size);
        org.example.common.model.PageResult<UserDetailResult> result = adminUserService.listUsers(page, size);

        return Result.success(new org.example.common.model.PageResult<>(
                adminUserAssembler.toResponseList(result.getList()),
                result.getTotal(),
                page,
                size));
    }

    @PostMapping("/admin")
    @Operation(summary = "创建管理员", description = "创建新的管理员用户（需要管理员权限）")
    public Result<UserDetailResponse> createAdmin(@RequestBody CreateAdminRequest request) {
        log.info("【AdminUser】创建管理员请求, username: {}", request.getUsername());

        UserDetailResult result = adminUserService.createAdmin(
                adminUserAssembler.toCommand(request));

        return Result.success(adminUserAssembler.toResponse(result));
    }
}
