package org.example.domain.service.trial.node;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.example.common.exception.BizException;
import org.example.common.pattern.flow.AbstractFlowNode;
import org.example.common.pattern.flow.FlowNode;
import org.example.domain.model.activity.TrialBalanceRequest;
import org.example.domain.model.activity.TrialBalanceResult;
import org.example.domain.model.activity.context.TrialBalanceContext;
import org.example.domain.model.activity.repository.ActivityRepository;

/**
 * 流量控制节点
 * 职责：降级开关检查、切量灰度控制
 */
@Slf4j
@Setter
public class FlowControlNode extends AbstractFlowNode<TrialBalanceRequest, TrialBalanceContext, TrialBalanceResult> {

    private final ActivityRepository activityRepository;
    private CrowdTagValidationNode crowdTagValidationNode;

    public FlowControlNode(ActivityRepository activityRepository) {
        this.activityRepository = activityRepository;
    }

    @Override
    protected TrialBalanceResult doExecute(TrialBalanceRequest request, TrialBalanceContext context) {
        log.info("【流量控制节点】开始检查，userId: {}", request.getUserId());

        // 1. 降级开关检查
        if (activityRepository.isDowngraded()) {
            log.warn("【流量控制节点】系统降级中，拒绝请求，userId: {}", request.getUserId());
            throw new BizException("系统繁忙，请稍后再试");
        }

        // 2. 切量灰度控制
        if (!activityRepository.isInCutRange(request.getUserId())) {
            log.info("【流量控制节点】用户不在切量范围内，userId: {}", request.getUserId());
            throw new BizException("活动暂未对您开放");
        }

        context.addExecutedNode(getNodeName());
        log.info("【流量控制节点】检查通过");
        return null;
    }

    @Override
    public FlowNode<TrialBalanceRequest, TrialBalanceContext, TrialBalanceResult> route(
            TrialBalanceRequest request, TrialBalanceContext context) {
        return crowdTagValidationNode;
    }
}
