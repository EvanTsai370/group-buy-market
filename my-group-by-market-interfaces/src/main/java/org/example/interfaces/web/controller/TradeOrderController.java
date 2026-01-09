package org.example.interfaces.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.example.application.service.trade.TradeOrderService;
import org.example.application.service.trade.cmd.RefundCmd;
import org.example.common.ratelimit.RateLimit;
import org.example.domain.model.trade.valueobject.TradeStatus;
import org.example.interfaces.web.assembler.TradeOrderAssembler;
import org.example.interfaces.web.dto.LockOrderRequest;
import org.example.interfaces.web.dto.TradeOrderResponse;
import org.example.interfaces.web.request.RefundRequest;
import org.example.common.api.Result;
import org.example.common.exception.BizException;
import org.example.domain.model.payment.PaymentCallbackDTO;
import org.example.domain.model.payment.PaymentCallbackRecord;
import org.example.domain.model.payment.repository.PaymentCallbackRecordRepository;
import org.example.domain.model.trade.TradeOrder;
import org.example.domain.model.trade.repository.TradeOrderRepository;
import org.example.application.service.payment.PaymentSignatureValidator;
import org.example.domain.shared.IdGenerator;
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
    private final TradeOrderAssembler tradeOrderAssembler;
    private final PaymentSignatureValidator paymentSignatureValidator;
    private final PaymentCallbackRecordRepository paymentCallbackRecordRepository;
    private final TradeOrderRepository tradeOrderRepository;
    private final IdGenerator idGenerator;
    private final org.example.domain.service.refund.RefundTimeWindowValidator refundTimeWindowValidator;

    public TradeOrderController(TradeOrderService tradeOrderService,
            TradeOrderAssembler tradeOrderAssembler,
            PaymentSignatureValidator paymentSignatureValidator,
            PaymentCallbackRecordRepository paymentCallbackRecordRepository,
            TradeOrderRepository tradeOrderRepository,
            IdGenerator idGenerator,
            org.example.domain.service.refund.RefundTimeWindowValidator refundTimeWindowValidator) {
        this.tradeOrderService = tradeOrderService;
        this.tradeOrderAssembler = tradeOrderAssembler;
        this.paymentSignatureValidator = paymentSignatureValidator;
        this.paymentCallbackRecordRepository = paymentCallbackRecordRepository;
        this.tradeOrderRepository = tradeOrderRepository;
        this.idGenerator = idGenerator;
        this.refundTimeWindowValidator = refundTimeWindowValidator;
    }

    /**
     * 锁单接口
     *
     * @param request 锁单请求
     * @return 交易订单信息
     */
    @PostMapping("/lock")
    @Operation(summary = "锁单", description = "用户参与拼团，锁定交易订单")
    public Result<TradeOrderResponse> lockOrder(@RequestBody LockOrderRequest request) {
        log.info("【TradeOrderController】锁单请求, userId: {}, activityId: {}, orderId: {}",
                request.getUserId(), request.getActivityId(), request.getOrderId());

        // 1. 协议层 → 应用层转换
        var cmd = tradeOrderAssembler.toCommand(request);

        // 2. 调用应用服务
        var result = tradeOrderService.lockOrder(cmd);

        // 3. 应用层 → 协议层转换
        var response = tradeOrderAssembler.toResponse(result);

        return Result.success(response);
    }

    /**
     * 支付成功回调接口
     *
     * <p>
     * 安全措施：
     * <ol>
     * <li>IP白名单验证（拦截器层）</li>
     * <li>签名验证（HMAC-SHA256）</li>
     * <li>时间戳验证（防重放攻击）</li>
     * <li>幂等性检查（基于callbackId）</li>
     * <li>金额校验</li>
     * </ol>
     *
     * @param tradeOrderId 交易订单ID
     * @param sign         签名
     * @param timestamp    时间戳（毫秒）
     * @param callbackId   支付系统的唯一回调ID
     * @param callback     回调数据
     * @param request      HTTP请求（用于获取IP）
     * @return 处理结果
     */
    @PostMapping("/payment/success/{tradeOrderId}")
    @Operation(summary = "支付成功回调", description = "支付系统回调，通知支付成功")
    public Result<Void> handlePaymentSuccess(
            @PathVariable String tradeOrderId,
            @RequestParam String sign,
            @RequestParam Long timestamp,
            @RequestParam String callbackId,
            @RequestBody PaymentCallbackDTO callback,
            HttpServletRequest request) {

        log.info("【TradeOrderController】支付成功回调, tradeOrderId: {}, callbackId: {}, " +
                "amount: {}, payTime: {}, channel: {}, ip: {}",
                tradeOrderId, callbackId, callback.getAmount(),
                callback.getPayTime(), callback.getChannel(),
                request.getRemoteAddr());

        // 1. 签名验证
        if (!paymentSignatureValidator.validate(callback, sign, timestamp, callback.getChannel())) {
            log.error("【支付回调】签名验证失败, tradeOrderId: {}, callbackId: {}", tradeOrderId, callbackId);
            throw new BizException("签名验证失败");
        }

        // 2. 时间戳验证（防重放攻击，5分钟有效期）
        if (System.currentTimeMillis() - timestamp > 5 * 60 * 1000) {
            log.error("【支付回调】请求已过期, tradeOrderId: {}, timestamp: {}", tradeOrderId, timestamp);
            throw new BizException("请求已过期");
        }

        // 3. 幂等性检查
        if (paymentCallbackRecordRepository.existsByCallbackId(callbackId)) {
            log.warn("【支付回调】回调已处理, callbackId: {}", callbackId);
            return Result.success();
        }

        // 4. 金额校验
        TradeOrder tradeOrder = tradeOrderRepository.findByTradeOrderId(tradeOrderId)
                .orElseThrow(() -> new BizException("交易订单不存在"));

        if (callback.getAmount().compareTo(tradeOrder.getPayPrice()) != 0) {
            log.error("【支付回调】支付金额不匹配, tradeOrderId: {}, expected: {}, actual: {}",
                    tradeOrderId, tradeOrder.getPayPrice(), callback.getAmount());
            throw new BizException("支付金额不匹配");
        }

        // 5. 记录回调（幂等性保护）
        String recordId = "REC-" + idGenerator.nextId();
        PaymentCallbackRecord record = PaymentCallbackRecord.create(recordId, callbackId, callback);
        paymentCallbackRecordRepository.save(record);

        // 6. 处理支付成功
        tradeOrderService.handlePaymentSuccess(tradeOrderId);

        log.info("【TradeOrderController】支付成功处理完成, tradeOrderId: {}, callbackId: {}",
                tradeOrderId, callbackId);

        return Result.success();
    }

    /**
     * 退单接口
     *
     * <p>
     * 安全措施：
     * <ol>
     * <li>权限验证：验证当前用户是否有权退此订单</li>
     * <li>业务状态校验：验证订单状态是否支持退款</li>
     * <li>时间窗口校验：验证是否在退款时间窗口内</li>
     * <li>参数校验：验证交易订单ID格式</li>
     * <li>频率限制：每用户每分钟最多3次退款请求</li>
     * </ol>
     *
     * @param tradeOrderId  交易订单ID
     * @param currentUserId 当前用户ID（从认证上下文获取）
     * @param request       退款请求
     * @return 处理结果
     */
    @PostMapping("/refund/{tradeOrderId}")
    @Operation(summary = "退单", description = "用户申请退单")
    @RateLimit(key = "#currentUserId", message = "退款操作过于频繁，请稍后再试")
    public Result<Void> refundOrder(
            @PathVariable @Pattern(regexp = "^TRD\\d+$", message = "交易订单ID格式错误") String tradeOrderId,
            @RequestHeader(value = "X-User-Id", required = false) String currentUserId,
            @RequestBody @Valid RefundRequest request) {

        log.info("【TradeOrderController】退单请求, tradeOrderId: {}, userId: {}, reason: {}",
                tradeOrderId, currentUserId, request.getReason());

        // 1. 加载订单
        TradeOrder tradeOrder = tradeOrderRepository.findByTradeOrderId(tradeOrderId)
                .orElseThrow(() -> new BizException("交易订单不存在"));

        // 2. 权限校验
        if (currentUserId == null || currentUserId.isEmpty()) {
            log.warn("【权限校验】缺少用户身份信息, tradeOrderId: {}", tradeOrderId);
            throw new BizException("缺少用户身份信息");
        }
        if (!tradeOrder.getUserId().equals(currentUserId)) {
            log.warn("【权限校验】用户无权退单, currentUserId: {}, orderUserId: {}",
                    currentUserId, tradeOrder.getUserId());
            throw new BizException("无权操作此订单");
        }

        // 3. 业务状态校验
        TradeStatus status = tradeOrder.getStatus();
        if (status == TradeStatus.SETTLED) {
            throw new BizException("订单已结算，不支持退款");
        }
        if (status == TradeStatus.TIMEOUT) {
            throw new BizException("订单已超时，系统将自动处理");
        }
        if (status == TradeStatus.REFUND) {
            log.warn("订单已退款, tradeOrderId: {}", tradeOrderId);
            return Result.success(); // 幂等性
        }
        if (!status.canRefund()) {
            throw new BizException("当前订单状态不支持退款");
        }

        // 4. 时间窗口校验
        refundTimeWindowValidator.validate(tradeOrder);

        // 5. 执行退款
        tradeOrderService.refundTradeOrder(tradeOrderId, request.getReason());

        log.info("【TradeOrderController】退单成功, tradeOrderId: {}", tradeOrderId);

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
    public Result<TradeOrderResponse> queryTradeOrder(@PathVariable String tradeOrderId) {
        log.info("【TradeOrderController】查询交易订单, tradeOrderId: {}", tradeOrderId);

        // 1. 调用应用服务
        var result = tradeOrderService.queryTradeOrder(tradeOrderId);

        // 2. 应用层 → 协议层转换
        var response = tradeOrderAssembler.toResponse(result);

        return Result.success(response);
    }

    /**
     * 退款接口
     *
     * @param request     退款请求
     * @param httpRequest HTTP请求
     * @return 操作结果
     */
    @PostMapping("/refund")
    @Operation(summary = "退款", description = "对交易订单进行退款操作")
    public Result<Void> refund(@Valid @RequestBody RefundRequest request,
            HttpServletRequest httpRequest) {
        log.info("【TradeOrderController】退款请求, tradeOrderId: {}, reason: {}",
                request.getTradeOrderId(), request.getReason());

        // 1. 协议层 → 用例层转换
        RefundCmd cmd = RefundCmd.builder()
                .tradeOrderId(request.getTradeOrderId())
                .reason(request.getReason())
                .operator("SYSTEM") // TODO: 从认证上下文获取当前用户
                .clientIp(httpRequest.getRemoteAddr())
                .build();

        // 2. 调用应用服务
        tradeOrderService.refund(cmd);

        return Result.success();
    }
}
