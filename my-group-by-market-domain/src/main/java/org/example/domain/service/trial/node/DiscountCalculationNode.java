package org.example.domain.service.trial.node;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.example.common.exception.BizException;
import org.example.common.pattern.flow.AbstractAsyncDataFlowNode;
import org.example.common.pattern.flow.DataLoader;
import org.example.common.pattern.flow.FlowNode;
import org.example.domain.model.activity.Activity;
import org.example.domain.model.activity.Discount;
import org.example.domain.model.activity.TrialBalanceRequest;
import org.example.domain.model.goods.Sku;
import org.example.domain.model.activity.TrialBalanceResult;
import org.example.domain.model.activity.context.TrialBalanceContext;
import org.example.domain.model.activity.repository.ActivityRepository;
import org.example.domain.model.goods.repository.SkuRepository;
import org.example.domain.service.discount.DiscountCalculator;
import org.example.domain.service.trial.loader.DiscountDataLoader;
import org.example.domain.service.trial.loader.SkuDataLoader;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * 折扣计算节点（带异步数据加载）
 * 职责：
 * 1. 异步加载活动配置、折扣配置、商品信息
 * 2. 计算折扣金额和实付金额
 */
@Slf4j
@Setter
public class DiscountCalculationNode extends AbstractAsyncDataFlowNode<TrialBalanceRequest, TrialBalanceContext, TrialBalanceResult> {

    private final ActivityRepository activityRepository;
    private final SkuRepository skuRepository;
    private final Map<String, DiscountCalculator> discountCalculatorMap;

    private ResultAssemblyNode resultAssemblyNode;
    private ErrorHandlingNode errorHandlingNode;

    public DiscountCalculationNode(
            ActivityRepository activityRepository,
            SkuRepository skuRepository,
            Map<String, DiscountCalculator> discountCalculatorMap,
            ExecutorService executorService) {
        this.activityRepository = activityRepository;
        this.skuRepository = skuRepository;
        this.discountCalculatorMap = discountCalculatorMap;
        this.executorService = executorService;
        this.dataLoadTimeoutSeconds = 5L;
    }

    @Override
    protected List<DataLoader<TrialBalanceRequest, TrialBalanceContext>> getDataLoaders() {
        // 加载商品信息和折扣配置
        // 此时 Activity 已经在人群标签校验节点加载完成，DiscountDataLoader 可以正常工作
        return List.of(
            new SkuDataLoader(skuRepository),
            new DiscountDataLoader(activityRepository)
        );
    }

    @Override
    protected TrialBalanceResult doExecute(TrialBalanceRequest request, TrialBalanceContext context) {
        log.info("【折扣计算节点】开始计算折扣，userId: {}, goodsId: {}",
                 request.getUserId(), request.getGoodsId());

        // 获取异步加载的数据（Activity 来自上一个节点，Discount 和 Sku 由当前节点加载）
        Activity activity = context.getActivity();
        Discount discount = context.getDiscount();
        Sku sku = context.getSku();

        // 数据完整性校验
        if (activity == null || discount == null || sku == null) {
            log.warn("【折扣计算节点】数据加载不完整，activity={}, discount={}, sku={}",
                     activity != null, discount != null, sku != null);
            return null; // 路由到错误节点
        }

        // 获取折扣计算器
        String marketPlan = discount.getMarketPlan();
        DiscountCalculator calculator = discountCalculatorMap.get(marketPlan);

        if (calculator == null) {
            log.error("【折扣计算节点】不支持的营销计划类型: {}", marketPlan);
            throw new BizException("不支持的营销计划类型: " + marketPlan);
        }

        // 计算实付金额
        BigDecimal originalPrice = sku.getOriginalPrice();
        BigDecimal payAmount = calculator.calculate(request.getUserId(), originalPrice, discount);
        BigDecimal deductionAmount = originalPrice.subtract(payAmount);

        // 写入上下文
        context.setPayAmount(payAmount);
        context.setDeductionAmount(deductionAmount);
        context.addExecutedNode(getNodeName());

        log.info("【折扣计算节点】计算完成，原价: {}, 折扣: {}, 实付: {}",
                 originalPrice, deductionAmount, payAmount);

        return null;
    }

    @Override
    public FlowNode<TrialBalanceRequest, TrialBalanceContext, TrialBalanceResult> route(
            TrialBalanceRequest request, TrialBalanceContext context) {

        // 如果数据加载失败或计算失败，路由到错误节点
        if (!context.hasActivity() || !context.hasSku() || !context.hasCalculatedDiscount()) {
            return errorHandlingNode;
        }

        return resultAssemblyNode;
    }
}
