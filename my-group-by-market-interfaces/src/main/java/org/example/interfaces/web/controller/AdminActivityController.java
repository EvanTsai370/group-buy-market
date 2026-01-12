package org.example.interfaces.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.application.service.admin.AdminActivityService;
import org.example.common.api.Result;
import org.example.domain.model.activity.Activity;
import org.example.domain.model.activity.ActivityGoods;
import org.example.domain.model.activity.Discount;
import org.example.domain.model.activity.valueobject.DiscountType;
import org.example.domain.model.activity.valueobject.GroupType;
import org.example.domain.model.activity.valueobject.TagScope;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 活动管理控制器
 * 
 * @author 开发团队
 * @since 2026-01-10
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/activity")
@RequiredArgsConstructor
@Tag(name = "活动管理", description = "管理后台活动管理接口")
@PreAuthorize("hasRole('ADMIN')")
public class AdminActivityController {

    private final AdminActivityService adminActivityService;

    // ==================== 活动管理 ====================

    @GetMapping
    @Operation(summary = "活动列表", description = "分页查询活动列表")
    public Result<List<Activity>> listActivities(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("【AdminActivity】查询活动列表, page: {}, size: {}", page, size);
        List<Activity> activities = adminActivityService.listActivities(page, size);
        return Result.success(activities);
    }

    @GetMapping("/{activityId}")
    @Operation(summary = "活动详情", description = "查询活动详情")
    public Result<Activity> getActivityDetail(@PathVariable String activityId) {
        log.info("【AdminActivity】查询活动详情, activityId: {}", activityId);
        Activity activity = adminActivityService.getActivityDetail(activityId);
        return Result.success(activity);
    }

    @PostMapping
    @Operation(summary = "创建活动", description = "创建拼团活动")
    public Result<Activity> createActivity(@RequestBody CreateActivityRequest request) {
        log.info("【AdminActivity】创建活动, name: {}", request.getActivityName());

        AdminActivityService.CreateActivityCmd cmd = new AdminActivityService.CreateActivityCmd();
        cmd.setActivityName(request.getActivityName());
        cmd.setActivityDesc(request.getActivityDesc());
        cmd.setDiscountId(request.getDiscountId());
        cmd.setTagId(request.getTagId());
        cmd.setTagScope(request.getTagScope());
        cmd.setGroupType(request.getGroupType());
        cmd.setTarget(request.getTarget());
        cmd.setValidTime(request.getValidTime());
        cmd.setParticipationLimit(request.getParticipationLimit());
        cmd.setStartTime(request.getStartTime());
        cmd.setEndTime(request.getEndTime());

        Activity activity = adminActivityService.createActivity(cmd);
        return Result.success(activity);
    }

    @PostMapping("/{activityId}/activate")
    @Operation(summary = "上架活动", description = "上架拼团活动")
    public Result<String> activateActivity(@PathVariable String activityId) {
        log.info("【AdminActivity】上架活动, activityId: {}", activityId);
        adminActivityService.activateActivity(activityId);
        return Result.success("活动已上架");
    }

    @PostMapping("/{activityId}/close")
    @Operation(summary = "下架活动", description = "下架拼团活动")
    public Result<String> closeActivity(@PathVariable String activityId) {
        log.info("【AdminActivity】下架活动, activityId: {}", activityId);
        adminActivityService.closeActivity(activityId);
        return Result.success("活动已下架");
    }

    // ==================== 折扣管理 ====================

    @GetMapping("/discount/{discountId}")
    @Operation(summary = "折扣详情", description = "查询折扣配置详情")
    public Result<Discount> getDiscount(@PathVariable String discountId) {
        log.info("【AdminActivity】查询折扣详情, discountId: {}", discountId);
        Discount discount = adminActivityService.getDiscount(discountId);
        return Result.success(discount);
    }

    @PostMapping("/discount")
    @Operation(summary = "创建折扣", description = "创建折扣配置")
    public Result<Discount> createDiscount(@RequestBody CreateDiscountRequest request) {
        log.info("【AdminActivity】创建折扣, name: {}", request.getDiscountName());

        AdminActivityService.CreateDiscountCmd cmd = new AdminActivityService.CreateDiscountCmd();
        cmd.setDiscountName(request.getDiscountName());
        cmd.setDiscountDesc(request.getDiscountDesc());
        cmd.setDiscountAmount(request.getDiscountAmount());
        cmd.setDiscountType(request.getDiscountType());
        cmd.setMarketPlan(request.getMarketPlan());
        cmd.setMarketExpr(request.getMarketExpr());
        cmd.setTagId(request.getTagId());

        Discount discount = adminActivityService.createDiscount(cmd);
        return Result.success(discount);
    }

    // ==================== 活动商品关联 ====================

    @PostMapping("/{activityId}/goods")
    @Operation(summary = "添加活动商品", description = "添加活动商品关联")
    public Result<String> addActivityGoods(
            @PathVariable String activityId,
            @RequestBody AddActivityGoodsRequest request) {
        log.info("【AdminActivity】添加活动商品关联, activityId: {}, skuId: {}",
                activityId, request.getSkuId());

        adminActivityService.addActivityGoods(
                activityId,
                request.getSkuId(),
                request.getSource(),
                request.getChannel(),
                request.getDiscountId());

        return Result.success("活动商品关联添加成功");
    }

    @GetMapping("/{activityId}/goods")
    @Operation(summary = "查询活动商品", description = "查询活动商品关联")
    public Result<ActivityGoods> getActivityGoods(
            @PathVariable String activityId,
            @RequestParam String skuId,
            @RequestParam String source,
            @RequestParam String channel) {
        log.info("【AdminActivity】查询活动商品关联, activityId: {}, skuId: {}",
                activityId, skuId);

        ActivityGoods activityGoods = adminActivityService.getActivityGoods(
                activityId, skuId, source, channel);
        return Result.success(activityGoods);
    }

    // ==================== 请求对象 ====================

    @Data
    public static class CreateActivityRequest {
        private String activityName;
        private String activityDesc;
        private String discountId;
        private String tagId;
        private TagScope tagScope;
        private GroupType groupType;
        private Integer target; // 成团目标人数
        private Integer validTime; // 拼单有效时长（秒）
        private Integer participationLimit; // 用户参团次数限制
        private LocalDateTime startTime;
        private LocalDateTime endTime;
    }

    @Data
    public static class CreateDiscountRequest {
        private String discountName;
        private String discountDesc;
        private BigDecimal discountAmount;
        private DiscountType discountType;
        private String marketPlan; // ZJ=直减, ZK=折扣, N=N元购, MJ=满减
        private String marketExpr; // 例如 "0.9" 表示9折, "10" 表示减10元
        private String tagId;
    }

    @Data
    public static class AddActivityGoodsRequest {
        private String skuId;
        private String source; // 来源（如：s01-小程序）
        private String channel; // 渠道（如：c01-首页）
        private String discountId; // 可选，为空则使用活动默认折扣
    }
}
