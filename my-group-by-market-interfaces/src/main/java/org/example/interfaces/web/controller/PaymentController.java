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
     * 核心流程（按支付宝文档要求）：
     * <ol>
     * <li>解析支付宝回调参数</li>
     * <li>验证签名（SDK验签）</li>
     * <li>校验 app_id 是否为本商户</li>
     * <li>校验 seller_id 是否匹配</li>
     * <li>根据 trade_status 处理业务逻辑</li>
     * </ol>
     * 
     * <p>
     * 支付宝文档要求：
     * <ul>
     * <li>验证 app_id 是否为该商家本身</li>
     * <li>校验 seller_id 是否为对应操作方</li>
     * <li>校验 out_trade_no 和 total_amount 是否正确</li>
     * <li>只有 TRADE_SUCCESS 或 TRADE_FINISHED 才认定为付款成功</li>
     * </ul>
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

            // 3. 校验 app_id（支付宝文档明确要求）
            String callbackAppId = params.get("app_id");
            String configAppId = alipayPaymentService.getAppId();
            if (configAppId != null && !configAppId.isEmpty() && !configAppId.equals(callbackAppId)) {
                log.error("【PaymentController】app_id 校验失败, 回调: {}, 配置: {}", callbackAppId, configAppId);
                return "fail";
            }

            // 4. 校验 seller_id（支付宝文档明确要求）
            String callbackSellerId = params.get("seller_id");
            String configSellerId = alipayPaymentService.getSellerId();
            if (configSellerId != null && !configSellerId.isEmpty() && !configSellerId.equals(callbackSellerId)) {
                log.error("【PaymentController】seller_id 校验失败, 回调: {}, 配置: {}", callbackSellerId, configSellerId);
                return "fail";
            }

            // 5. 解析交易状态
            String outTradeNo = params.get("out_trade_no");
            String tradeStatus = params.get("trade_status");
            String totalAmount = params.get("total_amount");

            log.info("【PaymentController】回调详情: outTradeNo={}, tradeStatus={}, totalAmount={}",
                    outTradeNo, tradeStatus, totalAmount);

            // 6. 根据交易状态处理业务
            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                // 支付成功：调用结算服务（内置幂等性检查和金额校验）
                BigDecimal callbackAmount = totalAmount != null ? new BigDecimal(totalAmount) : null;
                settlementService.handlePaymentSuccessByOutTradeNo(outTradeNo, callbackAmount);

            } else if ("TRADE_CLOSED".equals(tradeStatus)) {
                // 交易关闭：释放锁定的名额和库存
                log.info("【PaymentController】交易关闭，触发退单流程, outTradeNo={}", outTradeNo);
                try {
                    settlementService.handlePaymentFailedByOutTradeNo(outTradeNo);
                } catch (Exception ex) {
                    // 退单失败只记录日志，不影响返回 success（避免重复通知）
                    log.error("【PaymentController】交易关闭处理失败, outTradeNo={}", outTradeNo, ex);
                }
            }
            // WAIT_BUYER_PAY 状态无需处理

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
