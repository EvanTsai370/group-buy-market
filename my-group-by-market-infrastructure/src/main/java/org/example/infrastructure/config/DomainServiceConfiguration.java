package org.example.infrastructure.config;

import org.example.domain.model.activity.repository.ActivityRepository;
import org.example.domain.model.notification.repository.NotificationTaskRepository;
import org.example.domain.model.order.repository.OrderRepository;
import org.example.domain.model.tag.repository.CrowdTagRepository;
import org.example.domain.model.trade.repository.TradeOrderRepository;
import org.example.domain.service.lock.IDistributedLockService;
import org.example.domain.service.LockOrderService;
import org.example.domain.service.RefundService;
import org.example.domain.service.SettlementService;
import org.example.domain.service.discount.*;
import org.example.domain.service.refund.*;
import org.example.domain.shared.IdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

/**
 * 领域服务配置类
 *
 * <p>
 * 职责：
 * <ul>
 * <li>将Domain层的领域服务注册为Spring Bean</li>
 * <li>保持Domain层的框架无关性（不在Domain层使用Spring注解）</li>
 * <li>遵循DDD架构原则：Domain层纯粹，Infrastructure层提供技术支持</li>
 * </ul>
 *
 * <p>
 * 架构说明：
 * <ul>
 * <li>Domain层：只包含业务逻辑，不依赖任何框架</li>
 * <li>Infrastructure层：通过Configuration类将Domain对象注册为Bean</li>
 * <li>Application层：通过依赖注入使用领域服务</li>
 * </ul>
 *
 * @author 开发团队
 * @since 2026-01-04
 */
@Configuration
public class DomainServiceConfiguration {

    /**
     * 锁单领域服务
     *
     * <p>
     * 协调Order聚合和TradeOrder聚合的锁单操作
     */
    @Bean
    public LockOrderService lockOrderService(
            OrderRepository orderRepository,
            TradeOrderRepository tradeOrderRepository) {
        return new LockOrderService(orderRepository, tradeOrderRepository);
    }

    /**
     * 结算领域服务
     *
     * <p>
     * 处理支付成功后的结算逻辑，协调Order和TradeOrder的状态变更，并生成通知任务
     */
    @Bean
    public SettlementService settlementService(
            OrderRepository orderRepository,
            TradeOrderRepository tradeOrderRepository,
            NotificationTaskRepository notificationTaskRepository,
            IdGenerator idGenerator) {
        return new SettlementService(orderRepository, tradeOrderRepository,
                notificationTaskRepository, idGenerator);
    }

    /**
     * 退单领域服务
     *
     * <p>
     * 处理订单失败/超时后的退款逻辑，确保锁单量正确释放
     * <p>
     * 使用策略模式处理不同场景的退单逻辑
     * <p>
     * 降级策略：退款失败时发送到MQ异步重试
     */
    @Bean
    public RefundService refundService(
            TradeOrderRepository tradeOrderRepository,
            RefundStrategyFactory refundStrategyFactory,
            TeamRefundStrategy teamRefundStrategy,
            IDistributedLockService lockService,
            IRefundFallbackService fallbackService) {

      return new RefundService(
                tradeOrderRepository, refundStrategyFactory, teamRefundStrategy, lockService, fallbackService);
    }

    // ==================== 退单策略配置 ====================

    /**
     * 未支付退单策略
     *
     * <p>
     * 处理未支付订单的退单逻辑，释放锁定的拼团名额并恢复Redis库存
     */
    @Bean
    public UnpaidRefundStrategy unpaidRefundStrategy(
            OrderRepository orderRepository,
            TradeOrderRepository tradeOrderRepository,
            ActivityRepository activityRepository,
            IDistributedLockService lockService) {
        return new UnpaidRefundStrategy(orderRepository, tradeOrderRepository, activityRepository, lockService);
    }

    /**
     * 已支付退单策略
     *
     * <p>
     * 处理已支付订单的退单逻辑，调用支付网关退款并恢复Redis库存
     */
    @Bean
    public PaidRefundStrategy paidRefundStrategy(
            OrderRepository orderRepository,
            TradeOrderRepository tradeOrderRepository,
            ActivityRepository activityRepository,
            IDistributedLockService lockService) {
        return new PaidRefundStrategy(orderRepository, tradeOrderRepository, activityRepository, lockService);
    }

    /**
     * 拼团退单策略
     *
     * <p>
     * 处理拼团失败场景的批量退单逻辑
     */
    @Bean
    public TeamRefundStrategy teamRefundStrategy(
            OrderRepository orderRepository,
            TradeOrderRepository tradeOrderRepository,
            UnpaidRefundStrategy unpaidRefundStrategy,
            PaidRefundStrategy paidRefundStrategy) {
        return new TeamRefundStrategy(
                orderRepository,
                tradeOrderRepository,
                unpaidRefundStrategy,
                paidRefundStrategy);
    }

    /**
     * 退单策略工厂
     *
     * <p>
     * 根据交易订单状态选择合适的退单策略
     */
    @Bean
    public RefundStrategyFactory refundStrategyFactory(
            UnpaidRefundStrategy unpaidRefundStrategy,
            PaidRefundStrategy paidRefundStrategy) {
        return new RefundStrategyFactory(
                List.of(unpaidRefundStrategy, paidRefundStrategy));
    }

    /**
     * 退款时间窗口验证器
     *
     * <p>
     * 验证不同状态订单的退款时间规则
     */
    @Bean
    public org.example.domain.service.refund.RefundTimeWindowValidator refundTimeWindowValidator(
            OrderRepository orderRepository) {
        return new org.example.domain.service.refund.RefundTimeWindowValidator(orderRepository);
    }

    // ==================== 折扣计算器配置 ====================

    /**
     * 直接减免折扣计算器
     *
     * <p>
     * 原价基础上直接减去固定金额
     */
    @Bean
    public DirectDiscountCalculator directDiscountCalculator(
            CrowdTagRepository crowdTagRepository) {
        return new DirectDiscountCalculator(crowdTagRepository);
    }

    /**
     * 百分比折扣计算器
     *
     * <p>
     * 原价基础上按百分比计算折扣（例如9折）
     */
    @Bean
    public PercentageDiscountCalculator percentageDiscountCalculator(
            CrowdTagRepository crowdTagRepository) {
        return new PercentageDiscountCalculator(crowdTagRepository);
    }

    /**
     * 固定价格折扣计算器
     *
     * <p>
     * 不管原价多少，统一按固定价格售卖
     */
    @Bean
    public FixedPriceDiscountCalculator fixedPriceDiscountCalculator(
            CrowdTagRepository crowdTagRepository) {
        return new FixedPriceDiscountCalculator(crowdTagRepository);
    }

    /**
     * 满减折扣计算器
     *
     * <p>
     * 满足条件后减免固定金额（例如满100减20）
     */
    @Bean
    public FullReductionDiscountCalculator fullReductionDiscountCalculator(
            CrowdTagRepository crowdTagRepository) {
        return new FullReductionDiscountCalculator(crowdTagRepository);
    }

    /**
     * 折扣计算器映射表
     *
     * <p>
     * 根据营销计划类型(marketPlan)动态选择具体的折扣计算器实现
     * <p>
     * 支持的营销计划类型:
     * <ul>
     * <li>ZJ - 直减折扣</li>
     * <li>ZK - 百分比折扣</li>
     * <li>N - N元购(固定价格)</li>
     * <li>MJ - 满减折扣</li>
     * </ul>
     */
    @Bean
    public Map<String, DiscountCalculator> discountCalculatorMap(
            DirectDiscountCalculator directDiscountCalculator,
            PercentageDiscountCalculator percentageDiscountCalculator,
            FixedPriceDiscountCalculator fixedPriceDiscountCalculator,
            FullReductionDiscountCalculator fullReductionDiscountCalculator) {
        return Map.of(
                "ZJ", directDiscountCalculator,
                "ZK", percentageDiscountCalculator,
                "N", fixedPriceDiscountCalculator,
                "MJ", fullReductionDiscountCalculator);
    }
}
