package org.example.domain.model.trade.filter;

import lombok.extern.slf4j.Slf4j;
import org.example.common.pattern.chain.model2.IChainHandler;
import org.example.domain.model.account.Account;
import org.example.domain.model.account.repository.AccountRepository;
import org.example.domain.model.activity.Activity;

/**
 * 用户参与限制校验处理器
 *
 * <p>
 * 职责：
 * <ul>
 * <li>校验用户在该活动下的参与次数是否超过限制</li>
 * <li>如果活动未设置参与限制（participationLimit=null或0），则跳过校验</li>
 * </ul>
 *
 * <p>
 * 设计说明：
 * <ul>
 * <li>使用 AccountRepository 加载 Account 聚合根</li>
 * <li>委托给 Account.assertHasAvailableCount() 进行校验（充血模型）</li>
 * <li>数据来源统一：使用 Account.participationCount，而不是
 * TradeOrderRepository.count()</li>
 * </ul>
 *
 */
@Slf4j
public class UserParticipationLimitHandler
        implements IChainHandler<TradeFilterRequest, TradeFilterContext, TradeFilterResponse> {

    private final AccountRepository accountRepository;

    public UserParticipationLimitHandler(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public TradeFilterResponse handle(TradeFilterRequest request, TradeFilterContext context) throws Exception {
        String userId = request.getUserId();
        String activityId = request.getActivityId();

        // 1. 获取活动信息（必须由前置handler已加载）
        Activity activity = context.getActivity();
        if (activity == null) {
            throw new IllegalStateException("活动信息未加载，请确保ActivityAvailabilityHandler在此handler之前执行");
        }

        // 2. 加载或创建 Account 聚合根（首次参与活动时自动创建）
        Account account = accountRepository.findByUserAndActivity(userId, activityId)
                .orElseGet(() -> {
                    String accountId = accountRepository.nextId();
                    Account newAccount = Account.create(accountId, userId, activityId, null);
                    accountRepository.save(newAccount);
                    log.info("【用户参与限制校验】首次参与活动，创建新账户, accountId: {}, userId: {}, activityId: {}",
                            accountId, userId, activityId);
                    return newAccount;
                });

        // 3. 委托给聚合根进行校验（充血模型）
        // 如果参与次数已达上限，聚合根会抛出带详细信息的 BizException
        account.assertHasAvailableCount(activity);

        log.info("【用户参与限制校验】校验通过, userId: {}, activityId: {}, used: {}, limit: {}",
                userId, activityId, account.getParticipationCount(), activity.getParticipationLimit());
        return TradeFilterResponse.allow();
    }
}
