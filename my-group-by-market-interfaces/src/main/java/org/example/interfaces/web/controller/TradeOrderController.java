package org.example.interfaces.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.example.application.service.trade.TradeOrderService;
import org.example.application.service.trade.cmd.LockOrderCmd;
import org.example.application.service.trade.vo.TradeOrderVO;
import org.example.common.api.Result;
import org.springframework.web.bind.annotation.*;

/**
 * 交易订单控制器
 *
 * @author 开发团队
 * @since 2026-01-04
 */
@Slf4j
@RestController
@RequestMapping("/api/trade")
@Tag(name = "交易订单管理", description = "交易订单相关接口")
public class TradeOrderController {

    private final TradeOrderService tradeOrderService;

    public TradeOrderController(TradeOrderService tradeOrderService) {
        this.tradeOrderService = tradeOrderService;
    }

    /**
     * 锁单接口
     *
     * @param cmd 锁单命令
     * @return 交易订单信息
     */
    @PostMapping("/lock")
    @Operation(summary = "锁单", description = "用户参与拼团，锁定交易订单")
    public Result<TradeOrderVO> lockOrder(@RequestBody LockOrderCmd cmd) {
        log.info("【TradeOrderController】锁单请求, userId: {}, activityId: {}, orderId: {}",
                cmd.getUserId(), cmd.getActivityId(), cmd.getOrderId());

        // 调用应用服务
        TradeOrderVO vo = tradeOrderService.lockOrder(cmd);

        return Result.success(vo);
    }

    /**
     * 支付成功回调接口
     *
     * @param tradeOrderId 交易订单ID
     * @return 处理结果
     */
    @PostMapping("/payment/success/{tradeOrderId}")
    @Operation(summary = "支付成功回调", description = "支付系统回调，通知支付成功")
    public Result<Void> handlePaymentSuccess(@PathVariable String tradeOrderId) {
        log.info("【TradeOrderController】支付成功回调, tradeOrderId: {}", tradeOrderId);

        tradeOrderService.handlePaymentSuccess(tradeOrderId);

        return Result.success();
    }

    /**
     * 退单接口
     *
     * @param tradeOrderId 交易订单ID
     * @return 处理结果
     */
    @PostMapping("/refund/{tradeOrderId}")
    @Operation(summary = "退单", description = "用户申请退单或系统自动退单")
    public Result<Void> refundOrder(@PathVariable String tradeOrderId) {
        log.info("【TradeOrderController】退单请求, tradeOrderId: {}", tradeOrderId);

        tradeOrderService.refundTradeOrder(tradeOrderId);

        return Result.success();
    }

    /**
     * 查询交易订单接口
     *
     * @param tradeOrderId 交易订单ID
     * @return 交易订单信息
     */
    @GetMapping("/{tradeOrderId}")
    @Operation(summary = "查询交易订单", description = "根据交易订单ID查询订单详情")
    public Result<TradeOrderVO> queryTradeOrder(@PathVariable String tradeOrderId) {
        log.info("【TradeOrderController】查询交易订单, tradeOrderId: {}", tradeOrderId);

        TradeOrderVO vo = tradeOrderService.queryTradeOrder(tradeOrderId);

        return Result.success(vo);
    }
}
