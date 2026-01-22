package org.example.interfaces.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.application.service.admin.AdminTagService;
import org.example.common.api.Result;
import org.example.common.model.PageResult;
import org.example.domain.model.tag.CrowdTag;
import org.example.interfaces.web.assembler.AdminTagAssembler;
import org.example.interfaces.web.dto.admin.CreateTagRequest;
import org.example.interfaces.web.dto.admin.TagResponse;
import org.example.interfaces.web.dto.admin.UpdateTagRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/admin/tags")
@RequiredArgsConstructor
@Tag(name = "人群标签管理", description = "人群标签管理接口")
@PreAuthorize("hasRole('ADMIN')")
public class AdminTagController {

    private final AdminTagService adminTagService;
    private final AdminTagAssembler adminTagAssembler;

    @GetMapping
    @Operation(summary = "标签列表", description = "分页查询标签列表")
    public Result<PageResult<TagResponse>> listTags(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        log.info("【AdminTag】查询标签列表, page: {}, size: {}, keyword: {}, status: {}", page, size, keyword, status);
        PageResult<CrowdTag> result = adminTagService.listTags(page, size, keyword, status);
        return Result.success(adminTagAssembler.toPageResponse(result));
    }

    @GetMapping("/{tagId}")
    @Operation(summary = "标签详情", description = "获取标签详情")
    public Result<TagResponse> getTag(@PathVariable String tagId) {
        log.info("【AdminTag】获取标签详情, tagId: {}", tagId);
        CrowdTag tag = adminTagService.getTag(tagId);
        return Result.success(adminTagAssembler.toResponse(tag));
    }

    @PostMapping
    @Operation(summary = "创建标签", description = "创建新标签")
    public Result<Void> createTag(@RequestBody @Validated CreateTagRequest request) {
        log.info("【AdminTag】创建标签, request: {}", request);
        adminTagService.createTag(request.getTagName(), request.getTagRule());
        return Result.success();
    }

    @PutMapping("/{tagId}")
    @Operation(summary = "更新标签", description = "更新标签信息")
    public Result<Void> updateTag(
            @PathVariable String tagId,
            @RequestBody UpdateTagRequest request) {
        log.info("【AdminTag】更新标签, tagId: {}, request: {}", tagId, request);
        adminTagService.updateTag(tagId, request.getTagName(), request.getTagRule());
        return Result.success();
    }

    @PostMapping("/{tagId}/calculate")
    @Operation(summary = "计算标签", description = "触发标签人群计算")
    public Result<Void> calculateTag(@PathVariable String tagId) {
        log.info("【AdminTag】触发标签计算, tagId: {}", tagId);
        adminTagService.calculateTag(tagId);
        return Result.success();
    }
}
