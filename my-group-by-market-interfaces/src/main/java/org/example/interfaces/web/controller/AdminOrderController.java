package org.example.interfaces.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.application.service.admin.AdminOrderService;
import org.example.common.api.Result;
import org.example.domain.model.order.Order;
import org.example.domain.model.trade.TradeOrder;
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
@RequestMapping("/api/admin/order")
@RequiredArgsConstructor
@Tag(name = "订单管理", description = "管理后台订单管理接口")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    // ============== 拼团订单 ==============

    @GetMapping("/{orderId}")
    @Operation(summary = "拼团订单详情", description = "查询拼团订单详情")
    public Result<Order> getOrderDetail(@PathVariable String orderId) {
        log.info("【AdminOrder】查询拼团订单详情, orderId: {}", orderId);
        Order order = adminOrderService.getOrderDetail(orderId);
        return Result.success(order);
    }

    @GetMapping("/{orderId}/trades")
    @Operation(summary = "拼团交易明细", description = "查询拼团订单下的交易订单列表")
    public Result<List<TradeOrder>> listTradesByOrderId(@PathVariable String orderId) {
        log.info("【AdminOrder】查询拼团订单的交易明细, orderId: {}", orderId);
        List<TradeOrder> trades = adminOrderService.listTradeOrdersByOrderId(orderId);
        return Result.success(trades);
    }

    @GetMapping("/activity/{activityId}")
    @Operation(summary = "活动拼团列表", description = "查询活动下的拼团订单列表")
    public Result<List<Order>> listOrdersByActivity(@PathVariable String activityId) {
        log.info("【AdminOrder】查询活动的拼团订单, activityId: {}", activityId);
        List<Order> orders = adminOrderService.listOrdersByActivityId(activityId);
        return Result.success(orders);
    }

    // ============== 交易订单 ==============

    @GetMapping("/trade/{tradeOrderId}")
    @Operation(summary = "交易订单详情", description = "查询交易订单详情")
    public Result<TradeOrder> getTradeOrderDetail(@PathVariable String tradeOrderId) {
        log.info("【AdminOrder】查询交易订单详情, tradeOrderId: {}", tradeOrderId);
        TradeOrder trade = adminOrderService.getTradeOrderDetail(tradeOrderId);
        return Result.success(trade);
    }
}
