package org.example.interfaces.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.application.service.payment.AlipayPaymentService;
import org.example.application.service.payment.PaymentCallbackApplicationService;
import org.example.application.service.payment.result.PaymentQueryResultObj;
import org.example.common.api.Result;
import org.example.common.exception.BizException;
import org.example.domain.model.trade.TradeOrder;
import org.example.domain.model.trade.repository.TradeOrderRepository;
import org.example.domain.shared.AuthContextService;
import org.example.interfaces.web.assembler.PaymentAssembler;
import org.example.interfaces.web.dto.payment.PaymentQueryResponse;
import org.example.interfaces.web.request.CreatePaymentRequest;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Tag(name = "支付", description = "支付相关接口")
public class PaymentController {

    private final AlipayPaymentService alipayPaymentService;
    private final PaymentAssembler paymentAssembler;
    private final PaymentCallbackApplicationService paymentCallbackApplicationService;
    private final TradeOrderRepository tradeOrderRepository;
    private final AuthContextService authContextService;

    /**
     * 创建支付页面
     */
    @PostMapping("/create")
    @Operation(summary = "创建支付", description = "创建支付宝支付页面")
    public Result<String> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        // 1. 获取当前用户
        String currentUserId = authContextService.getCurrentUserId();
        log.info("【PaymentController】创建支付, userId: {}, outTradeNo: {}", currentUserId, request.getOutTradeNo());

        // 2. 查询交易订单
        TradeOrder tradeOrder = tradeOrderRepository
                .findByOutTradeNo(request.getOutTradeNo())
                .orElseThrow(() -> new BizException("交易订单不存在"));

        // 3. 安全校验：确认订单属于当前用户
        if (!tradeOrder.getUserId().equals(currentUserId)) {
            log.warn("【PaymentController】非本人操作支付, userId: {}, orderUserId: {}",
                    currentUserId, tradeOrder.getUserId());
            throw new BizException("无权操作此订单");
        }

        // 4. 状态校验
        if (tradeOrder.isPaid() || tradeOrder.isSettled()) {
            throw new BizException("订单已支付，请勿重复支付");
        }
        if (tradeOrder.isTimeout() || tradeOrder.isRefunded()) {
            throw new BizException("订单已失效，无法支付");
        }
        if (!tradeOrder.isCreated()) {
            throw new BizException("当前订单状态不支持支付");
        }

        // 5. 发起支付（使用订单中的真实金额和标题）
        String paymentForm = alipayPaymentService.createPaymentPage(
                tradeOrder.getOutTradeNo(),
                tradeOrder.getPayPrice(),
                tradeOrder.getGoodsName()); // 使用商品名称作为订单标题

        return Result.success(paymentForm);
    }

    /**
     * 查询支付状态
     *
     * <p>
     * 使用场景：
     * <ul>
     * <li>前端支付完成后，从支付宝跳转回商户页面，需要立即查询当前订单状态</li>
     * <li>前端页面轮询，确保支付状态同步</li>
     * </ul>
     */
    @GetMapping("/query")
    @Operation(summary = "查询支付", description = "查询支付状态")
    public Result<PaymentQueryResponse> queryPayment(
            @Parameter(description = "商户订单号", required = true) @RequestParam String outTradeNo) {
        log.info("【PaymentController】查询支付, outTradeNo: {}", outTradeNo);

        PaymentQueryResultObj result = alipayPaymentService.queryPayment(outTradeNo);
        return Result.success(paymentAssembler.toResponse(result));
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
            log.info("【PaymentController】参数详情 - sign参数: {}, app_id: {}, trade_status: {}, out_trade_no: {}",
                    params.get("sign"), params.get("app_id"), params.get("trade_status"), params.get("out_trade_no"));
            log.info("【PaymentController】参数总数: {}, 参数名列表: {}", params.size(), params.keySet());

            // 2. 验证签名
            if (!alipayPaymentService.verifyCallback(params)) {
                log.error("【PaymentController】验签失败 - 完整参数: {}", params);
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
                // 支付成功：调用应用服务（内置事务管理、幂等性检查和金额校验）
                BigDecimal callbackAmount = totalAmount != null ? new BigDecimal(totalAmount) : null;
                paymentCallbackApplicationService.handlePaymentSuccess(outTradeNo, callbackAmount);

            } else if ("TRADE_CLOSED".equals(tradeStatus)) {
                // 交易关闭：释放锁定的名额和库存
                log.info("【PaymentController】交易关闭，触发退单流程, outTradeNo={}", outTradeNo);
                try {
                    paymentCallbackApplicationService.handlePaymentFailure(outTradeNo);
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
     *
     * <p>
     * 使用场景：
     * <ul>
     * <li>支付宝支付成功后，浏览器重定向回商户页面的 Landing Page。</li>
     * <li>作为“支付成功”的展示页面路由，展示订单号并引导用户查看订单详情。</li>
     * </ul>
     */
    @GetMapping("/return")
    @Operation(summary = "支付跳转", description = "支付完成同步跳转")
    public void paymentReturn(HttpServletRequest request, HttpServletResponse response) throws java.io.IOException {
        log.info("【PaymentController】支付同步跳转");

        String outTradeNo = request.getParameter("out_trade_no");
        String tradeNo = request.getParameter("trade_no");

        log.info("【PaymentController】支付跳转, outTradeNo: {}, tradeNo: {}", outTradeNo, tradeNo);

        try {
            // 查询交易订单获取 orderId
            TradeOrder tradeOrder = tradeOrderRepository.findByOutTradeNo(outTradeNo)
                    .orElseThrow(() -> new BizException("交易订单不存在"));

            // 重定向到前端拼团进度页 (Vue Router history 模式)
            String redirectUrl = "http://localhost:8888/customer/progress/" + tradeOrder.getOrderId();
            log.info("【PaymentController】重定向到前端: {}", redirectUrl);
            response.sendRedirect(redirectUrl);

        } catch (Exception e) {
            log.error("【PaymentController】支付跳转失败", e);
            // 失败时重定向回订单列表
            response.sendRedirect("http://localhost:8888/customer/orders");
        }
    }
}
