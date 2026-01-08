package org.example.application.service.trade;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.example.application.assembler.TradeOrderAssembler;
import org.example.application.service.trade.cmd.LockOrderCmd;
import org.example.application.service.trade.vo.TradeOrderVO;
import org.example.common.exception.BizException;
import org.example.common.pattern.chain.model2.ChainExecutor;
import org.example.common.util.LogDesensitizer;
import org.example.domain.model.account.Account;
import org.example.domain.model.account.repository.AccountRepository;
import org.example.domain.model.activity.Activity;
import org.example.domain.model.activity.Discount;
import org.example.domain.model.activity.repository.ActivityRepository;
import org.example.domain.model.goods.Sku;
import org.example.domain.model.goods.repository.SkuRepository;
import org.example.domain.model.order.Order;
import org.example.domain.model.order.repository.OrderRepository;
import org.example.domain.model.order.valueobject.Money;
import org.example.domain.model.trade.TradeOrder;
import org.example.domain.model.trade.event.TradeOrderTimeoutMessage;
import org.example.domain.model.trade.filter.*;
import org.example.domain.model.trade.repository.TradeOrderRepository;
import org.example.domain.model.trade.valueobject.NotifyConfig;
import org.example.domain.model.trade.valueobject.NotifyType;
import org.example.domain.service.LockOrderService;
import org.example.domain.service.RefundService;
import org.example.domain.service.SettlementService;
import org.example.domain.service.discount.DiscountCalculator;
import org.example.domain.service.timeout.ITimeoutMessageProducer;
import org.example.domain.shared.IdGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * 交易订单应用服务
 *
 * <p>
 * 职责：
 * <ul>
 * <li>编排交易业务流程（锁单、结算、退单）</li>
 * <li>协调多个聚合和领域服务</li>
 * <li>处理事务边界</li>
 * </ul>
 *
 * @author 开发团队
 * @since 2026-01-04
 */
@Slf4j
@Service
public class TradeOrderService {

    private final ActivityRepository activityRepository;
    private final SkuRepository skuRepository;
    private final OrderRepository orderRepository;
    private final TradeOrderRepository tradeOrderRepository;
    private final AccountRepository accountRepository;
    private final IdGenerator idGenerator;
    private final Map<String, DiscountCalculator> discountCalculatorMap;

    // 领域服务
    private final LockOrderService lockOrderService;
    private final SettlementService settlementService;
    private final RefundService refundService;

    // 责任链工厂
    private final TradeFilterFactory tradeFilterFactory;

    // Assembler
    private final TradeOrderAssembler tradeOrderAssembler;

    // 超时消息生产者
    private final ITimeoutMessageProducer timeoutProducer;

    public TradeOrderService(ActivityRepository activityRepository,
            SkuRepository skuRepository,
            OrderRepository orderRepository,
            TradeOrderRepository tradeOrderRepository,
            AccountRepository accountRepository,
            IdGenerator idGenerator,
            Map<String, DiscountCalculator> discountCalculatorMap,
            LockOrderService lockOrderService,
            SettlementService settlementService,
            RefundService refundService,
            TradeOrderAssembler tradeOrderAssembler,
            ITimeoutMessageProducer timeoutProducer) {
        this.activityRepository = activityRepository;
        this.skuRepository = skuRepository;
        this.orderRepository = orderRepository;
        this.tradeOrderRepository = tradeOrderRepository;
        this.accountRepository = accountRepository;
        this.idGenerator = idGenerator;
        this.discountCalculatorMap = discountCalculatorMap;
        this.lockOrderService = lockOrderService;
        this.settlementService = settlementService;
        this.refundService = refundService;
        this.tradeFilterFactory = new TradeFilterFactory(activityRepository, accountRepository, tradeOrderRepository);
        this.tradeOrderAssembler = tradeOrderAssembler;
        this.timeoutProducer = timeoutProducer;
    }

    /**
     * 锁单
     *
     * <p>
     * 业务流程：
     * <p>
     * 业务流程：
     * <ol>
     * <li>幂等性检查</li>
     * <li>执行交易规则过滤链</li>
     * <li>计算并校验价格</li>
     * <li>创建或加载Order</li>
     * <li>扣减用户参团次数</li>
     * <li>调用锁单领域服务</li>
     * </ol>
     *
     * <p>
     * 事务说明：
     * <ul>
     * <li>整个方法在一个事务中执行</li>
     * <li>如果任何步骤失败,所有数据库操作会自动回滚</li>
     * <li>Account 参团次数扣减会回滚</li>
     * <li>Order 创建会回滚</li>
     * <li>TradeOrder 不会被创建</li>
     * </ul>
     *
     * @param cmd 锁单命令
     * @return 交易订单VO
     * @throws BizException 业务异常(如参团次数已达上限、拼团已满等)
     */
    @Transactional(rollbackFor = Exception.class)
    public TradeOrderVO lockOrder(LockOrderCmd cmd) {
        log.info("【TradeOrderService】开始锁单, userId: {}, activityId: {}, orderId: {}, outTradeNo: {}",
                cmd.getUserId(), cmd.getActivityId(), cmd.getOrderId(), cmd.getOutTradeNo());

        // 用于回滚的上下文
        TradeFilterContext filterContext = null;

        try {
            // 0. 幂等性检查：检查外部交易单号是否已存在
            Optional<TradeOrder> existingTradeOrder = tradeOrderRepository.findByOutTradeNo(cmd.getOutTradeNo());
            if (existingTradeOrder.isPresent()) {
                log.warn("【TradeOrderService】交易单号已存在，返回已有订单, outTradeNo: {}, tradeOrderId: {}",
                        cmd.getOutTradeNo(), existingTradeOrder.get().getTradeOrderId());
                return tradeOrderAssembler.toVO(existingTradeOrder.get());
            }

            // 1. 执行交易规则过滤链（保存context用于回滚）
            filterContext = executeTradeFilter(cmd);
            Activity activity = filterContext.getActivity();

            // 2. 加载Sku
            Sku sku = loadSku(cmd.getGoodsId());

            // 3. 计算并校验价格
            PriceValidationResult priceResult = calculateAndValidatePrice(cmd, activity, sku);

            // 4. 创建或加载Order
            String orderId = createOrderIfNeeded(cmd, activity, priceResult.originalPrice, priceResult.deductionPrice);

            // 5. 构建通知配置
            NotifyConfig notifyConfig = buildNotifyConfig(cmd);

            // 6. 加载或创建Account,并扣减参团次数
            Account account = loadOrCreateAccount(cmd.getUserId(), cmd.getActivityId());
            account.deductCount(activity);
            accountRepository.save(account);

            log.info("【TradeOrderService】参团次数扣减成功, userId: {}, activityId: {}, remainingCount: {}",
                    cmd.getUserId(), cmd.getActivityId(), account.getRemainingCount(activity));

            // 7. 调用锁单领域服务
            String tradeOrderId = "TRD" + idGenerator.nextId();
            TradeOrder tradeOrder = lockOrderService.lockOrder(
                    tradeOrderId,
                    orderId,
                    cmd.getActivityId(),
                    cmd.getUserId(),
                    cmd.getGoodsId(),
                    sku.getGoodsName(),
                    priceResult.originalPrice,
                    priceResult.deductionPrice,
                    priceResult.payPrice,
                    cmd.getOutTradeNo(),
                    cmd.getSource(),
                    cmd.getChannel(),
                    notifyConfig);

            log.info("【TradeOrderService】锁单成功, tradeOrderId: {}, orderId: {}, userId: {}, payPrice: {}",
                    tradeOrderId, orderId, cmd.getUserId(), LogDesensitizer.maskPrice(priceResult.payPrice, log));

            // 8. 发送超时消息（30分钟后自动退单）
            sendTimeoutMessage(tradeOrder);

            return tradeOrderAssembler.toVO(tradeOrder);

        } catch (BizException e) {
            // 业务异常：回滚Redis库存
            rollbackTeamStock(filterContext, cmd);

            log.warn("【TradeOrderService】锁单失败(业务异常), userId: {}, activityId: {}, outTradeNo: {}, reason: {}",
                    cmd.getUserId(), cmd.getActivityId(), cmd.getOutTradeNo(), e.getMessage());
            throw e;

        } catch (Exception e) {
            // 系统异常：回滚Redis库存
            rollbackTeamStock(filterContext, cmd);

            log.error("【TradeOrderService】锁单失败(系统异常), userId: {}, activityId: {}, outTradeNo: {}",
                    cmd.getUserId(), cmd.getActivityId(), cmd.getOutTradeNo(), e);
            throw new BizException("锁单失败: " + e.getMessage());
        }
    }

    /**
     * 支付成功回调
     *
     * <p>
     * 业务流程：
     * <ol>
     * <li>调用结算领域服务完成结算</li>
     * </ol>
     *
     * <p>
     * 设计说明：
     * <ul>
     * <li>移除了贫血的结算过滤链，业务逻辑现在封装在聚合内部</li>
     * <li>SettlementService 会加载 TradeOrder 并调用其 validatePayment() 方法</li>
     * <li>保持应用服务的简洁性，只做编排不做业务逻辑</li>
     * </ul>
     *
     * @param tradeOrderId 交易订单ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void handlePaymentSuccess(String tradeOrderId) {
        log.info("【TradeOrderService】处理支付成功, tradeOrderId: {}", tradeOrderId);

        // 直接调用结算领域服务
        settlementService.handlePaymentSuccess(tradeOrderId);

        log.info("【TradeOrderService】支付成功处理完成, tradeOrderId: {}", tradeOrderId);
    }

    /**
     * 退单
     *
     * @param tradeOrderId 交易订单ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void refundTradeOrder(String tradeOrderId) {
        log.info("【TradeOrderService】处理退单, tradeOrderId: {}", tradeOrderId);
        refundService.refundTradeOrder(tradeOrderId);
    }

    /**
     * 查询交易订单
     *
     * @param tradeOrderId 交易订单ID
     * @return 交易订单VO
     */
    public TradeOrderVO queryTradeOrder(String tradeOrderId) {
        Optional<TradeOrder> tradeOrderOpt = tradeOrderRepository.findByTradeOrderId(tradeOrderId);
        if (tradeOrderOpt.isEmpty()) {
            throw new BizException("交易订单不存在");
        }
        return tradeOrderAssembler.toVO(tradeOrderOpt.get());
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 执行交易规则过滤链
     *
     * <p>
     * 设计说明：
     * <ul>
     * <li>返回整个context而不是只返回Activity，用于后续回滚</li>
     * <li>context中包含recoveryTeamStockKey，用于失败时恢复Redis库存</li>
     * </ul>
     *
     * @param cmd 锁单命令
     * @return 过滤链上下文（包含Activity和回滚信息）
     */
    private TradeFilterContext executeTradeFilter(LockOrderCmd cmd) {
        ChainExecutor<TradeFilterRequest, TradeFilterContext, TradeFilterResponse> filterChain = tradeFilterFactory
                .createFilterChain();

        TradeFilterRequest request = TradeFilterRequest.builder()
                .userId(cmd.getUserId())
                .activityId(cmd.getActivityId())
                .goodsId(cmd.getGoodsId())
                .orderId(cmd.getOrderId())
                .build();

        TradeFilterContext context = new TradeFilterContext();

        try {
            TradeFilterResponse response = filterChain.execute(request, context);
            if (response != null && !response.isAllowed()) {
                throw new BizException(response.getReason());
            }
        } catch (Exception e) {
            if (e instanceof BizException) {
                throw (BizException) e;
            }
            throw new BizException("交易规则校验失败: " + e.getMessage());
        }

        Activity activity = context.getActivity();
        if (activity == null) {
            throw new BizException("活动信息加载失败");
        }

        return context;
    }

    /**
     * 发送超时消息
     *
     * <p>
     * 用途：在用户锁单后发送延迟消息，30分钟后自动检查并退单
     *
     * @param tradeOrder 交易订单
     */
    private void sendTimeoutMessage(TradeOrder tradeOrder) {
        try {
            // 构建超时消息
            TradeOrderTimeoutMessage message = new TradeOrderTimeoutMessage(
                    tradeOrder.getTradeOrderId(),
                    tradeOrder.getOrderId(),
                    tradeOrder.getUserId(),
                    tradeOrder.getActivityId(),
                    System.currentTimeMillis());

            // 发送延迟消息（默认30分钟）
            timeoutProducer.sendDelayMessage(message);

        } catch (Exception e) {
            // 发送失败只记录日志，不影响锁单主流程
            log.error("【TradeOrderService】发送超时消息失败, tradeOrderId={}",
                    tradeOrder.getTradeOrderId(), e);
        }
    }

    /**
     * 回滚Redis库存
     *
     * <p>
     * 业务场景：
     * <ul>
     * <li>TeamStockOccupyHandler已经扣减了Redis库存</li>
     * <li>后续步骤（如Account扣减、TradeOrder创建）失败</li>
     * <li>需要恢复Redis库存，防止"幽灵库存"问题</li>
     * </ul>
     *
     * <p>
     * 设计说明：
     * <ul>
     * <li>只在有recoveryKey时才执行回滚</li>
     * <li>回滚失败只记录日志，不影响主流程异常抛出</li>
     * <li>使用INCR原子操作，多次回滚不会有副作用</li>
     * </ul>
     *
     * @param filterContext 过滤链上下文
     * @param cmd           锁单命令
     */
    private void rollbackTeamStock(TradeFilterContext filterContext, LockOrderCmd cmd) {
        if (filterContext == null) {
            return;
        }

        String recoveryKey = filterContext.getRecoveryTeamStockKey();
        if (recoveryKey == null || recoveryKey.isEmpty()) {
            // 没有占用库存（首次开团），无需回滚
            return;
        }

        try {
            // 从Activity获取validTime
            Activity activity = filterContext.getActivity();
            Integer validTime = activity != null ? activity.getValidTime() : 1200; // 默认20分钟

            tradeOrderRepository.recoveryTeamStock(recoveryKey, validTime);
            log.info("【TradeOrderService】回滚Redis库存成功, teamStockKey: {}, userId: {}, orderId: {}",
                    recoveryKey, cmd.getUserId(), cmd.getOrderId());
        } catch (Exception ex) {
            // 回滚失败只记录日志，不影响主流程异常抛出
            // 运维可以通过日志发现问题，手动修复Redis数据
            log.error("【TradeOrderService】回滚Redis库存失败, teamStockKey: {}, userId: {}, orderId: {}",
                    recoveryKey, cmd.getUserId(), cmd.getOrderId(), ex);
        }
    }

    /**
     * 加载商品信息
     *
     * @param goodsId 商品ID
     * @return Sku商品
     */
    private Sku loadSku(String goodsId) {
        return skuRepository.findByGoodsId(goodsId)
                .orElseThrow(() -> new BizException("商品不存在"));
    }

    /**
     * 计算并校验价格
     *
     * @param cmd      锁单命令
     * @param activity 活动信息
     * @param sku      商品信息
     * @return 价格校验结果
     */
    private PriceValidationResult calculateAndValidatePrice(LockOrderCmd cmd, Activity activity, Sku sku) {
        // 1. 加载折扣配置
        Discount discount = activityRepository.queryDiscountById(activity.getDiscountId());
        if (discount == null) {
            throw new BizException("折扣配置不存在");
        }

        // 2. 选择折扣计算器
        String marketPlan = discount.getMarketPlan();
        DiscountCalculator calculator = discountCalculatorMap.get(marketPlan);
        if (calculator == null) {
            throw new BizException("不支持的营销计划类型: " + marketPlan);
        }

        // 3. 后端计算价格
        BigDecimal skuOriginalPrice = sku.getOriginalPrice();
        BigDecimal backendPayPrice = calculator.calculate(cmd.getUserId(), skuOriginalPrice, discount);
        BigDecimal backendDeductionPrice = skuOriginalPrice.subtract(backendPayPrice);

        log.info("【TradeOrderService】后端价格计算完成, 原价: {}, 优惠: {}, 实付: {}",
                skuOriginalPrice, backendDeductionPrice, backendPayPrice);

        // 4. 校验前端价格
        validatePriceParameters(cmd, skuOriginalPrice, backendPayPrice, backendDeductionPrice);

        log.info("【TradeOrderService】价格校验通过, userId: {}, payPrice: {}",
                cmd.getUserId(), LogDesensitizer.maskPrice(backendPayPrice, log));

        return new PriceValidationResult(skuOriginalPrice, backendDeductionPrice, backendPayPrice);
    }

    /**
     * 校验前端传入的价格参数
     */
    private void validatePriceParameters(LockOrderCmd cmd, BigDecimal skuOriginalPrice,
            BigDecimal backendPayPrice, BigDecimal backendDeductionPrice) {
        BigDecimal originalPrice = cmd.getOriginalPrice();
        BigDecimal deductionPrice = cmd.getDeductionPrice();
        BigDecimal payPrice = cmd.getPayPrice();

        if (originalPrice == null || deductionPrice == null || payPrice == null) {
            throw new BizException("价格参数不能为空，请先调用试算接口获取价格");
        }

        if (originalPrice.compareTo(skuOriginalPrice) != 0) {
            throw new BizException(String.format("原价校验失败，前端: %s, 后端: %s",
                    originalPrice, skuOriginalPrice));
        }
        if (payPrice.compareTo(backendPayPrice) != 0) {
            throw new BizException(String.format("实付金额校验失败，前端: %s, 后端: %s",
                    payPrice, backendPayPrice));
        }
        if (deductionPrice.compareTo(backendDeductionPrice) != 0) {
            throw new BizException(String.format("优惠金额校验失败，前端: %s, 后端: %s",
                    deductionPrice, backendDeductionPrice));
        }
    }

    /**
     * 创建或加载Order
     *
     * @param cmd            锁单命令
     * @param activity       活动信息
     * @param originalPrice  原价
     * @param deductionPrice 优惠金额
     * @return orderId
     */
    private String createOrderIfNeeded(LockOrderCmd cmd, Activity activity,
            BigDecimal originalPrice, BigDecimal deductionPrice) {
        String orderId = cmd.getOrderId();

        // 如果已有orderId,直接返回
        if (orderId != null && !orderId.isEmpty()) {
            Order existingOrder = orderRepository.findById(orderId)
                    .orElseThrow(() -> new BizException("拼团订单不存在"));
            existingOrder.validateLock();
            return orderId;
        }

        // 检查 outTradeNo 是否已存在（防止并发创建重复Order）
        Optional<TradeOrder> existingTradeOrder = tradeOrderRepository.findByOutTradeNo(cmd.getOutTradeNo());
        if (existingTradeOrder.isPresent()) {
            log.warn("【TradeOrderService】outTradeNo已存在，使用已有Order, outTradeNo: {}, orderId: {}",
                    cmd.getOutTradeNo(), existingTradeOrder.get().getOrderId());
            return existingTradeOrder.get().getOrderId();
        }

        // 创建新的拼团订单（用户是团长）
        orderId = orderRepository.nextId();
        String teamId = String.valueOf(idGenerator.nextId());

        Money price = Money.of(originalPrice, deductionPrice);
        LocalDateTime deadlineTime = LocalDateTime.now().plusSeconds(activity.getValidTime());

        Order newOrder = Order.create(
                orderId,
                teamId,
                cmd.getActivityId(),
                cmd.getGoodsId(),
                cmd.getUserId(),
                activity.getTarget(),
                price,
                deadlineTime,
                cmd.getSource(),
                cmd.getChannel(),
                idGenerator);

        orderRepository.save(newOrder);
        log.info("【TradeOrderService】创建新拼团, orderId: {}, teamId: {}", orderId, teamId);

        return orderId;
    }

    /**
     * 构建通知配置
     *
     * @param cmd 锁单命令
     * @return 通知配置
     */
    private NotifyConfig buildNotifyConfig(LockOrderCmd cmd) {
        if (cmd.getNotifyType() == null) {
            return null;
        }

        return NotifyConfig.builder()
                .notifyType(NotifyType.fromCode(cmd.getNotifyType()))
                .notifyUrl(cmd.getNotifyUrl())
                .notifyMq(cmd.getNotifyMq())
                .build();
    }

    /**
     * 加载或创建用户账户
     *
     * @param userId     用户ID
     * @param activityId 活动ID
     * @return Account聚合
     */
    private Account loadOrCreateAccount(String userId, String activityId) {
        return accountRepository.findByUserAndActivity(userId, activityId)
                .orElseGet(() -> {
                    String accountId = accountRepository.nextId();
                    Account newAccount = Account.create(accountId, userId, activityId, null);
                    accountRepository.save(newAccount);
                    log.info("【TradeOrderService】创建新账户, accountId: {}, userId: {}, activityId: {}",
                            accountId, userId, activityId);
                    return newAccount;
                });
    }

    /**
     * 价格校验结果内部类
     */
    private static class PriceValidationResult {
        final BigDecimal originalPrice;
        final BigDecimal deductionPrice;
        final BigDecimal payPrice;

        PriceValidationResult(BigDecimal originalPrice, BigDecimal deductionPrice, BigDecimal payPrice) {
            this.originalPrice = originalPrice;
            this.deductionPrice = deductionPrice;
            this.payPrice = payPrice;
        }
    }
}
