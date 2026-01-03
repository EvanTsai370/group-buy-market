package org.example.domain.service.trial.node;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.example.common.pattern.flow.AbstractFlowNode;
import org.example.common.pattern.flow.FlowNode;
import org.example.domain.model.activity.Activity;
import org.example.domain.model.activity.TrialBalanceRequest;
import org.example.domain.model.activity.TrialBalanceResult;
import org.example.domain.model.activity.context.TrialBalanceContext;
import org.example.domain.model.tag.repository.CrowdTagRepository;

/**
 * 人群标签校验节点
 * 职责：判断用户是否在活动的目标人群范围内
 */
@Slf4j
@Setter
public class CrowdTagValidationNode extends AbstractFlowNode<TrialBalanceRequest, TrialBalanceContext, TrialBalanceResult> {

    private final CrowdTagRepository crowdTagRepository;
    private ResultAssemblyNode resultAssemblyNode;

    public CrowdTagValidationNode(CrowdTagRepository crowdTagRepository) {
        this.crowdTagRepository = crowdTagRepository;
    }

    @Override
    protected TrialBalanceResult doExecute(TrialBalanceRequest request, TrialBalanceContext context) {
        log.info("【人群标签校验节点】开始校验，userId: {}", request.getUserId());

        Activity activity = context.getActivity();
        String tagId = activity.getTagId();

        // 如果活动未配置人群标签，则所有用户可见可参与
        if (tagId == null || tagId.trim().isEmpty()) {
            context.setVisible(true);
            context.setParticipable(true);
            log.info("【人群标签校验节点】活动未配置人群标签，所有用户可访问");
        } else {
            // 检查用户是否在人群标签内
            Boolean isInTag = crowdTagRepository.checkUserInTag(request.getUserId(), tagId);

            if (Boolean.TRUE.equals(isInTag)) {
                context.setVisible(true);
                context.setParticipable(true);
                log.info("【人群标签校验节点】用户在人群标签内，userId: {}, tagId: {}",
                         request.getUserId(), tagId);
            } else {
                context.setVisible(false);
                context.setParticipable(false);
                log.info("【人群标签校验节点】用户不在人群标签内，userId: {}, tagId: {}",
                         request.getUserId(), tagId);
            }
        }

        context.addExecutedNode(getNodeName());
        return null;
    }

    @Override
    public FlowNode<TrialBalanceRequest, TrialBalanceContext, TrialBalanceResult> route(
            TrialBalanceRequest request, TrialBalanceContext context) {
        return resultAssemblyNode;
    }
}
