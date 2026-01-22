package org.example.interfaces.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.application.service.admin.AdminOrderService;
import org.example.application.service.admin.result.OrderResult;
import org.example.application.service.admin.result.TradeOrderResult;
import org.example.common.api.Result;
import org.example.interfaces.web.assembler.AdminOrderAssembler;
import org.example.interfaces.web.dto.admin.AdminTradeOrderResponse;
import org.example.interfaces.web.dto.admin.OrderResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单管理控制器
 * 
 * @author 开发团队
 * @since 2026-01-10
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@Tag(name = "订单管理", description = "管理后台订单管理接口")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;
    private final AdminOrderAssembler adminOrderAssembler;

    // ============== 交易订单 ==============

    @GetMapping
    @Operation(summary = "交易订单列表", description = "分页查询交易订单")
    public Result<org.example.common.model.PageResult<AdminTradeOrderResponse>> listOrders(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "startDate", required = false) @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd") java.time.LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd") java.time.LocalDate endDate) {
        log.info("【AdminOrder】分页查询交易订单, page: {}, size: {}, keyword: {}, status: {}", page, size, keyword, status);
        java.time.LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        java.time.LocalDateTime end = endDate != null ? endDate.atTime(java.time.LocalTime.MAX) : null;

        org.example.common.model.PageResult<TradeOrderResult> result = adminOrderService.listTradeOrders(page, size,
                keyword, status, start, end);

        List<AdminTradeOrderResponse> responseList = adminOrderAssembler.toTradeOrderResponseList(result.getList());
        org.example.common.model.PageResult<AdminTradeOrderResponse> pageResponse = new org.example.common.model.PageResult<>(
                responseList, result.getTotal(), page, size);

        return Result.success(pageResponse);
    }

    // ============== 拼团订单 ==============

    @GetMapping("/{orderId}")
    @Operation(summary = "拼团订单详情", description = "查询拼团订单详情")
    public Result<OrderResponse> getOrderDetail(@PathVariable String orderId) {
        log.info("【AdminOrder】查询拼团订单详情, orderId: {}", orderId);
        OrderResult result = adminOrderService.getOrderDetail(orderId);
        return Result.success(adminOrderAssembler.toOrderResponse(result));
    }

    @GetMapping("/{orderId}/trades")
    @Operation(summary = "拼团交易明细", description = "查询拼团订单下的交易订单列表")
    public Result<List<AdminTradeOrderResponse>> listTradesByOrderId(@PathVariable String orderId) {
        log.info("【AdminOrder】查询拼团订单的交易明细, orderId: {}", orderId);
        List<TradeOrderResult> results = adminOrderService.listTradeOrdersByOrderId(orderId);
        return Result.success(adminOrderAssembler.toTradeOrderResponseList(results));
    }

    @GetMapping("/activity/{activityId}")
    @Operation(summary = "活动拼团列表", description = "查询活动下的拼团订单列表")
    public Result<List<OrderResponse>> listOrdersByActivity(@PathVariable String activityId) {
        log.info("【AdminOrder】查询活动的拼团订单, activityId: {}", activityId);
        List<OrderResult> results = adminOrderService.listOrdersByActivityId(activityId);
        return Result.success(adminOrderAssembler.toOrderResponseList(results));
    }

    // ============== 交易订单 ==============

    @GetMapping("/trade/{tradeOrderId}")
    @Operation(summary = "交易订单详情", description = "查询交易订单详情")
    public Result<AdminTradeOrderResponse> getTradeOrderDetail(@PathVariable String tradeOrderId) {
        log.info("【AdminOrder】查询交易订单详情, tradeOrderId: {}", tradeOrderId);
        TradeOrderResult result = adminOrderService.getTradeOrderDetail(tradeOrderId);
        return Result.success(adminOrderAssembler.toTradeOrderResponse(result));
    }
}
