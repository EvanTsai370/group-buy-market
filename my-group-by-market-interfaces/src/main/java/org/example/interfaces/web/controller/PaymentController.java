package org.example.interfaces.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.application.service.payment.AlipayPaymentService;
import org.example.application.service.payment.result.PaymentQueryResultObj;
import org.example.application.service.payment.result.RefundQueryResultObj;
import org.example.application.service.payment.result.RefundResultObj;
import org.example.common.api.Result;
import org.example.interfaces.web.assembler.PaymentAssembler;
import org.example.interfaces.web.dto.payment.PaymentQueryResponse;
import org.example.interfaces.web.dto.payment.RefundQueryResponse;
import org.example.interfaces.web.dto.payment.RefundResponse;
import org.example.interfaces.web.request.CreatePaymentRequest;
import org.example.interfaces.web.request.PaymentRefundRequest;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 支付控制器
 * 
 * @author 开发团队
 * @since 2026-01-10
 */
@Slf4j
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Tag(name = "支付", description = "支付相关接口")
public class PaymentController {

    private final AlipayPaymentService alipayPaymentService;
    private final PaymentAssembler paymentAssembler;

    /**
     * 创建支付页面
     */
    @PostMapping("/create")
    @Operation(summary = "创建支付", description = "创建支付宝支付页面")
    public Result<String> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        log.info("【PaymentController】创建支付, outTradeNo: {}, amount: {}",
                request.getOutTradeNo(), request.getAmount());

        String paymentForm = alipayPaymentService.createPaymentPage(
                request.getOutTradeNo(), request.getAmount(), request.getSubject());
        return Result.success(paymentForm);
    }

    /**
     * 查询支付状态
     */
    @GetMapping("/query")
    @Operation(summary = "查询支付", description = "查询支付状态")
    public Result<PaymentQueryResponse> queryPayment(@RequestParam String outTradeNo) {
        log.info("【PaymentController】查询支付, outTradeNo: {}", outTradeNo);

        PaymentQueryResultObj result = alipayPaymentService.queryPayment(outTradeNo);
        return Result.success(paymentAssembler.toResponse(result));
    }

    /**
     * 退款
     */
    @PostMapping("/refund")
    @Operation(summary = "退款", description = "发起退款")
    public Result<RefundResponse> refund(@Valid @RequestBody PaymentRefundRequest request) {
        log.info("【PaymentController】退款, outTradeNo: {}, refundAmount: {}",
                request.getOutTradeNo(), request.getRefundAmount());

        RefundResultObj result = alipayPaymentService.refund(
                request.getOutTradeNo(), request.getRefundAmount(),
                request.getRefundReason(), request.getOutRequestNo());
        return Result.success(paymentAssembler.toResponse(result));
    }

    /**
     * 查询退款
     */
    @GetMapping("/refund/query")
    @Operation(summary = "查询退款", description = "查询退款状态")
    public Result<RefundQueryResponse> queryRefund(
            @RequestParam String outTradeNo,
            @RequestParam String outRequestNo) {
        log.info("【PaymentController】查询退款, outTradeNo: {}, outRequestNo: {}", outTradeNo, outRequestNo);

        RefundQueryResultObj result = alipayPaymentService.queryRefund(outTradeNo, outRequestNo);
        return Result.success(paymentAssembler.toResponse(result));
    }

    /**
     * 关闭订单
     */
    @PostMapping("/close")
    @Operation(summary = "关闭订单", description = "关闭支付订单")
    public Result<Boolean> closeOrder(@RequestParam String outTradeNo) {
        log.info("【PaymentController】关闭订单, outTradeNo: {}", outTradeNo);

        boolean result = alipayPaymentService.closeOrder(outTradeNo);
        return Result.success(result);
    }

    /**
     * 支付宝异步回调
     */
    @PostMapping("/callback/alipay")
    @Operation(summary = "支付回调", description = "支付宝异步通知接口")
    public String alipayCallback(HttpServletRequest request) {
        log.info("【PaymentController】收到支付宝回调");

        // 获取所有参数
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();
        for (String name : requestParams.keySet()) {
            String[] values = requestParams.get(name);
            StringBuilder valueStr = new StringBuilder();
            for (int i = 0; i < values.length; i++) {
                valueStr.append((i == values.length - 1) ? values[i] : values[i] + ",");
            }
            params.put(name, valueStr.toString());
        }

        log.info("【PaymentController】回调参数: {}", params);

        // 处理回调
        boolean success = alipayPaymentService.handlePaymentCallback(params);

        // 返回处理结果
        return success ? "success" : "fail";
    }

    /**
     * 支付同步跳转
     */
    @GetMapping("/return")
    @Operation(summary = "支付跳转", description = "支付完成同步跳转")
    public Result<String> paymentReturn(HttpServletRequest request) {
        log.info("【PaymentController】支付同步跳转");

        String outTradeNo = request.getParameter("out_trade_no");
        String tradeNo = request.getParameter("trade_no");

        log.info("【PaymentController】支付跳转, outTradeNo: {}, tradeNo: {}", outTradeNo, tradeNo);

        return Result.success("支付完成，订单号: " + outTradeNo);
    }
}
