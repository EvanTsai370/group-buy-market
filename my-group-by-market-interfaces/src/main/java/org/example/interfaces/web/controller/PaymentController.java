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
import org.example.common.exception.BizException;
import org.example.domain.service.SettlementService;
import org.example.interfaces.web.assembler.PaymentAssembler;
import org.example.interfaces.web.dto.payment.PaymentQueryResponse;
import org.example.interfaces.web.dto.payment.RefundQueryResponse;
import org.example.interfaces.web.dto.payment.RefundResponse;
import org.example.interfaces.web.request.CreatePaymentRequest;
import org.example.interfaces.web.request.PaymentRefundRequest;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
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
    private final SettlementService settlementService;

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
     * 
     * <p>
     * 核心流程：
     * <ol>
     * <li>解析支付宝回调参数</li>
     * <li>验证签名（SDK验签）</li>
     * <li>调用结算服务处理支付成功（内置幂等性和金额校验）</li>
     * </ol>
     */
    @PostMapping("/callback/alipay")
    @Operation(summary = "支付回调", description = "支付宝异步通知接口")
    public String alipayCallback(HttpServletRequest request) {
        log.info("【PaymentController】收到支付宝回调");

        try {
            // 1. 获取所有参数
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

            // 2. 验证签名
            if (!alipayPaymentService.verifyCallback(params)) {
                log.error("【PaymentController】验签失败");
                return "fail";
            }

            // 3. 解析交易状态
            String outTradeNo = params.get("out_trade_no");
            String tradeStatus = params.get("trade_status");
            String totalAmount = params.get("total_amount");

            log.info("【PaymentController】回调详情: outTradeNo={}, tradeStatus={}, totalAmount={}",
                    outTradeNo, tradeStatus, totalAmount);

            // 4. 只处理成功状态
            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                BigDecimal callbackAmount = totalAmount != null ? new BigDecimal(totalAmount) : null;
                // 调用结算服务（内置幂等性检查和金额校验）
                settlementService.handlePaymentSuccessByOutTradeNo(outTradeNo, callbackAmount);
            }
            // TODO：支付失败释放锁定的名额以及库存

            return "success";

        } catch (BizException e) {
            // 业务异常（如订单不存在）→ 返回 fail，支付宝会重试
            log.error("【PaymentController】支付回调业务异常: {}", e.getMessage());
            return "fail";
        } catch (Exception e) {
            // 系统异常 → 返回 fail，支付宝会重试
            log.error("【PaymentController】支付回调系统异常", e);
            return "fail";
        }
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
