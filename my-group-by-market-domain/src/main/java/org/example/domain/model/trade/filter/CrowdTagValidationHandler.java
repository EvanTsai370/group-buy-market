package org.example.domain.model.trade.filter;

import lombok.extern.slf4j.Slf4j;
import org.example.common.exception.BizException;
import org.example.common.pattern.chain.model2.IChainHandler;
import org.example.domain.model.activity.Activity;
import org.example.domain.service.validation.CrowdTagValidationResult;
import org.example.domain.service.validation.CrowdTagValidationService;

/**
 * 人群标签校验过滤器
 * 
 * <p>
 * 职责：
 * <ul>
 * <li>检查用户是否在活动的目标人群范围内</li>
 * <li>不可参与时返回拒绝响应，阻止锁单</li>
 * </ul>
 * 
 * <p>
 * 执行顺序：第三个执行（在活动可用性校验之后）
 * 
 * <p>
 * 前置条件：
 * <ul>
 * <li>Context 中必须已加载 Activity 信息（由 ActivityAvailabilityHandler 提供）</li>
 * </ul>
 * 
 */
@Slf4j
public class CrowdTagValidationHandler
        implements IChainHandler<TradeFilterRequest, TradeFilterContext, TradeFilterResponse> {

    private final CrowdTagValidationService crowdTagValidationService;

    public CrowdTagValidationHandler(CrowdTagValidationService crowdTagValidationService) {
        this.crowdTagValidationService = crowdTagValidationService;
    }

    @Override
    public TradeFilterResponse handle(TradeFilterRequest request, TradeFilterContext context) throws Exception {
        log.info("【人群标签校验过滤器】开始执行，userId: {}, activityId: {}",
                request.getUserId(), request.getActivityId());

        // 从上下文获取 Activity（由前面的 ActivityAvailabilityHandler 加载）
        Activity activity = context.getActivity();
        if (activity == null) {
            throw new BizException("活动信息未加载，无法进行人群标签校验");
        }

        // 调用人群标签校验服务
        CrowdTagValidationResult validationResult = crowdTagValidationService.validate(request.getUserId(), activity);

        // 如果不可参与，返回拒绝响应阻止锁单
        if (!validationResult.isParticipable()) {
            log.warn("【人群标签校验过滤器】用户不可参与活动，userId: {}, activityId: {}, reason: {}",
                    request.getUserId(), request.getActivityId(), validationResult.getReason());
            return TradeFilterResponse.reject(validationResult.getReason());
        }

        log.info("【人群标签校验过滤器】校验通过，userId: {}, activityId: {}",
                request.getUserId(), request.getActivityId());

        // 返回允许继续执行
        return TradeFilterResponse.allow();
    }
}
