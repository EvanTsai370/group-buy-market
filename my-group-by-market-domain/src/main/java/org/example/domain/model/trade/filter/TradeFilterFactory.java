package org.example.domain.model.trade.filter;

import org.example.common.pattern.chain.model2.ChainExecutor;
import org.example.domain.model.account.repository.AccountRepository;
import org.example.domain.model.activity.repository.ActivityRepository;
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
 * <li>ActivityAvailabilityHandler - 活动可用性校验（加载Activity到上下文）</li>
 * <li>UserParticipationLimitHandler - 用户参与限制校验（依赖Activity信息）</li>
 * <li>TeamStockOccupyHandler - 组队库存占用校验（防止超卖）</li>
 * </ol>
 *
 * @author 开发团队
 * @since 2026-01-04
 */
public class TradeFilterFactory {

    private final ActivityRepository activityRepository;
    private final AccountRepository accountRepository;
    private final TradeOrderRepository tradeOrderRepository;

    public TradeFilterFactory(ActivityRepository activityRepository,
            AccountRepository accountRepository,
            TradeOrderRepository tradeOrderRepository) {
        this.activityRepository = activityRepository;
        this.accountRepository = accountRepository;
        this.tradeOrderRepository = tradeOrderRepository;
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
        executor.addHandler(new ActivityAvailabilityHandler(activityRepository))
                .addHandler(new UserParticipationLimitHandler(accountRepository))
                .addHandler(new TeamStockOccupyHandler(tradeOrderRepository));

        return executor;
    }
}
