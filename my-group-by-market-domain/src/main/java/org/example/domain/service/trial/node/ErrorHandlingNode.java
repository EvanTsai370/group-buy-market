package org.example.domain.service.trial.node;

import lombok.extern.slf4j.Slf4j;
import org.example.common.exception.BizException;
import org.example.common.pattern.flow.AbstractFlowNode;
import org.example.common.pattern.flow.FlowNode;
import org.example.domain.model.activity.TrialBalanceRequest;
import org.example.domain.model.activity.TrialBalanceResult;
import org.example.domain.model.activity.context.TrialBalanceContext;

/**
 * 错误处理节点
 * 职责：统一处理流程中的异常情况
 */
@Slf4j
public class ErrorHandlingNode extends AbstractFlowNode<TrialBalanceRequest, TrialBalanceContext, TrialBalanceResult> {

    @Override
    protected TrialBalanceResult doExecute(TrialBalanceRequest request, TrialBalanceContext context) {
        log.error("【错误处理节点】流程执行异常，userId: {}, skuId: {}",
                  request.getUserId(), request.getSkuId());

        // 判断具体错误原因
        if (!context.hasActivity()) {
            log.error("【错误处理节点】商品无关联活动，skuId: {}", request.getSkuId());
            throw new BizException("该商品暂无拼团活动");
        }

        if (!context.hasSku()) {
            log.error("【错误处理节点】商品信息不存在，skuId: {}", request.getSkuId());
            throw new BizException("商品信息不存在");
        }

        if (!context.hasDiscount()) {
            log.error("【错误处理节点】活动折扣配置缺失，activityId: {}",
                      context.getActivity().getActivityId());
            throw new BizException("活动配置异常");
        }

        // 通用错误
        throw new BizException("试算失败，请稍后再试");
    }

    @Override
    public FlowNode<TrialBalanceRequest, TrialBalanceContext, TrialBalanceResult> route(
            TrialBalanceRequest request, TrialBalanceContext context) {
        return null; // 错误节点是终止节点
    }
}
