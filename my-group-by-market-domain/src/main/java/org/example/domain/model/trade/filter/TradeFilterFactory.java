package org.example.domain.model.trade.filter;

import org.example.common.pattern.chain.model2.ChainExecutor;
import org.example.domain.model.account.repository.AccountRepository;
import org.example.domain.model.activity.repository.ActivityRepository;
import org.example.domain.model.goods.repository.SkuRepository;
import org.example.domain.model.trade.repository.TradeOrderRepository;

/**
 * 交易规则过滤链工厂
 *
 * <p>
 * 职责：
 * <ul>
 * <li>创建和组装交易规则过滤链</li>
 * <li>定义handler的执行顺序</li>
 * </ul>
 *
 * <p>
 * 执行顺序：
 * <ol>
 * <li>FlowControlHandler - 流控校验（降级开关、切量灰度）</li>
 * <li>ActivityAvailabilityHandler - 活动可用性校验（加载Activity到上下文）</li>
 * <li>CrowdTagValidationHandler - 人群标签校验（依赖Activity信息）</li>
 * <li>UserParticipationLimitHandler - 用户参与限制校验（依赖Activity信息）</li>
 * <li>TeamSlotOccupyHandler - 组队名额占用校验（防止超卖）</li>
 * <li>InventoryOccupyHandler - 商品库存预占校验（防止库存超卖）</li>
 * </ol>
 *
 * @author 开发团队
 * @since 2026-01-04
 */
public class TradeFilterFactory {

    private final ActivityRepository activityRepository;
    private final AccountRepository accountRepository;
    private final TradeOrderRepository tradeOrderRepository;
    private final SkuRepository skuRepository;
    private final org.example.domain.service.validation.FlowControlService flowControlService;
    private final org.example.domain.service.validation.CrowdTagValidationService crowdTagValidationService;

    public TradeFilterFactory(ActivityRepository activityRepository,
            AccountRepository accountRepository,
            TradeOrderRepository tradeOrderRepository,
            SkuRepository skuRepository,
            org.example.domain.service.validation.FlowControlService flowControlService,
            org.example.domain.service.validation.CrowdTagValidationService crowdTagValidationService) {
        this.activityRepository = activityRepository;
        this.accountRepository = accountRepository;
        this.tradeOrderRepository = tradeOrderRepository;
        this.skuRepository = skuRepository;
        this.flowControlService = flowControlService;
        this.crowdTagValidationService = crowdTagValidationService;
    }

    /**
     * 创建交易规则过滤链
     *
     * @return 过滤链执行器
     */
    public ChainExecutor<TradeFilterRequest, TradeFilterContext, TradeFilterResponse> createFilterChain() {
        ChainExecutor<TradeFilterRequest, TradeFilterContext, TradeFilterResponse> executor = new ChainExecutor<>(
                "交易规则过滤链");

        // 按顺序添加handler
        executor.addHandler(new FlowControlHandler(flowControlService)) // 1. 流控检查（最早拦截）
                .addHandler(new ActivityAvailabilityHandler(activityRepository)) // 2. 活动可用性
                .addHandler(new CrowdTagValidationHandler(crowdTagValidationService)) // 3. 人群标签校验
                .addHandler(new UserParticipationLimitHandler(accountRepository)) // 4. 用户参与限制
                .addHandler(new TeamSlotOccupyHandler(tradeOrderRepository)) // 5. 组队名额占用
                .addHandler(new InventoryOccupyHandler(skuRepository)); // 6. 库存预占

        return executor;
    }
}
