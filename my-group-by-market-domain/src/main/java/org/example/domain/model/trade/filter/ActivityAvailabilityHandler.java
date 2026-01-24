package org.example.domain.model.trade.filter;

import lombok.extern.slf4j.Slf4j;
import org.example.common.pattern.chain.model2.IChainHandler;
import org.example.domain.model.activity.Activity;
import org.example.domain.model.activity.repository.ActivityRepository;

import java.util.Optional;

/**
 * 活动可用性校验处理器
 *
 * <p>
 * 职责：
 * <ul>
 * <li>校验活动是否存在</li>
 * <li>校验活动状态是否为ACTIVE</li>
 * <li>校验活动是否在有效期内</li>
 * </ul>
 *
 */
@Slf4j
public class ActivityAvailabilityHandler
        implements IChainHandler<TradeFilterRequest, TradeFilterContext, TradeFilterResponse> {

    private final ActivityRepository activityRepository;

    public ActivityAvailabilityHandler(ActivityRepository activityRepository) {
        this.activityRepository = activityRepository;
    }

    @Override
    public TradeFilterResponse handle(TradeFilterRequest request, TradeFilterContext context) throws Exception {
        String activityId = request.getActivityId();

        // 1. 查询活动信息（如果上下文中已有则复用）
        Activity activity = context.getActivity();
        if (activity == null) {
            Optional<Activity> activityOpt = activityRepository.findById(activityId);
            if (activityOpt.isEmpty()) {
                log.warn("【活动可用性校验】活动不存在, activityId: {}", activityId);
                return TradeFilterResponse.reject("活动不存在");
            }
            activity = activityOpt.get();
            context.setActivity(activity); // 缓存到上下文，供后续handler使用
        }

        // 2. 委托给聚合根进行校验（充血模型）
        // 如果活动不可用，聚合根会抛出带详细信息的 BizException
        activity.assertAvailable();

        log.info("【活动可用性校验】校验通过, activityId: {}", activityId);
        return TradeFilterResponse.allow();
    }
}
