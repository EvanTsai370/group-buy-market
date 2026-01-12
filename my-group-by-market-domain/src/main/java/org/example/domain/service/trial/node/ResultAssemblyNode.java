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

        // 判断是否跳过了折扣计算（不可见或仅可见用户）
        boolean skippedDiscountCalculation = (sku == null);

        TrialBalanceResult.TrialBalanceResultBuilder resultBuilder = TrialBalanceResult.builder()
                .visible(context.isVisible())
                .participable(context.isParticipable())
                .activityId(activity.getActivityId())
                .activityName(activity.getActivityName())
                .targetCount(activity.getTarget())
                .startTime(activity.getStartTime())
                .endTime(activity.getEndTime());

        if (skippedDiscountCalculation) {
            // 跳过了折扣计算：仅返回活动基本信息，价格相关字段为 null
            log.info("【结果组装节点】跳过折扣计算，仅返回活动基本信息，userId: {}", request.getUserId());
            resultBuilder
                    .skuId(request.getSkuId())  // 使用请求中的 skuId
                    .goodsName(null)
                    .originalPrice(null)
                    .deductionAmount(null)
                    .payAmount(null);
        } else {
            // 正常流程：返回完整信息（包括价格）
            resultBuilder
                    .skuId(sku.getSkuId())
                    .goodsName(sku.getGoodsName())
                    .originalPrice(sku.getOriginalPrice())
                    .deductionAmount(context.getDeductionAmount())
                    .payAmount(context.getPayAmount());
        }

        TrialBalanceResult result = resultBuilder.build();

        context.addExecutedNode(getNodeName());
        log.info("【结果组装节点】组装完成，跳过折扣计算: {}, 实付金额: {}",
                 skippedDiscountCalculation, result.getPayAmount());

        return result;
    }

    @Override
    public FlowNode<TrialBalanceRequest, TrialBalanceContext, TrialBalanceResult> route(
            TrialBalanceRequest request, TrialBalanceContext context) {
        return null; // 流程结束
    }
}
