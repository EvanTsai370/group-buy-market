package org.example.domain.service.trial.node;

import lombok.extern.slf4j.Slf4j;
import org.example.common.pattern.flow.AbstractFlowNode;
import org.example.common.pattern.flow.FlowNode;
import org.example.domain.model.activity.Activity;
import org.example.domain.model.activity.TrialBalanceRequest;
import org.example.domain.model.activity.TrialBalanceResult;
import org.example.domain.model.activity.context.TrialBalanceContext;
import org.example.domain.model.goods.Sku;

/**
 * 结果组装节点
 * 职责：将上下文中的数据组装成最终返回结果
 */
@Slf4j
public class ResultAssemblyNode extends AbstractFlowNode<TrialBalanceRequest, TrialBalanceContext, TrialBalanceResult> {

    @Override
    protected TrialBalanceResult doExecute(TrialBalanceRequest request, TrialBalanceContext context) {
        log.info("【结果组装节点】开始组装结果，userId: {}", request.getUserId());

        Activity activity = context.getActivity();
        Sku sku = context.getSku();

        // 组装返回结果
        TrialBalanceResult result = TrialBalanceResult.builder()
                .goodsId(sku.getGoodsId())
                .goodsName(sku.getGoodsName())
                .originalPrice(sku.getOriginalPrice())
                .deductionAmount(context.getDeductionAmount())
                .payAmount(context.getPayAmount())
                .targetCount(activity.getTarget())
                .startTime(activity.getStartTime())
                .endTime(activity.getEndTime())
                .visible(context.isVisible())
                .participable(context.isParticipable())
                .activityId(activity.getActivityId())
                .activityName(activity.getActivityName())
                .build();

        context.addExecutedNode(getNodeName());
        log.info("【结果组装节点】组装完成，实付金额: {}", result.getPayAmount());

        return result;
    }

    @Override
    public FlowNode<TrialBalanceRequest, TrialBalanceContext, TrialBalanceResult> route(
            TrialBalanceRequest request, TrialBalanceContext context) {
        return null; // 流程结束
    }
}
