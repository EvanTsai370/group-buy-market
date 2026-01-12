package org.example.domain.service.trial.node;

import com.alibaba.fastjson2.JSON;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.example.common.exception.BizException;
import org.example.common.pattern.flow.AbstractFlowNode;
import org.example.common.pattern.flow.FlowNode;
import org.example.domain.model.activity.TrialBalanceRequest;
import org.example.domain.model.activity.TrialBalanceResult;
import org.example.domain.model.activity.context.TrialBalanceContext;

/**
 * 参数校验节点
 * 职责：校验请求参数的完整性和合法性
 */
@Slf4j
@Setter
public class ParameterValidationNode extends AbstractFlowNode<TrialBalanceRequest, TrialBalanceContext, TrialBalanceResult> {

    private FlowControlNode flowControlNode;

    @Override
    protected TrialBalanceResult doExecute(TrialBalanceRequest request, TrialBalanceContext context) {
        log.info("【参数校验节点】开始校验，request: {}", JSON.toJSONString(request));

        // 校验必填参数
        if (StringUtils.isBlank(request.getUserId())) {
            throw new BizException("用户ID不能为空");
        }

        if (StringUtils.isBlank(request.getSkuId())) {
            throw new BizException("商品ID不能为空");
        }

        if (StringUtils.isBlank(request.getSource())) {
            throw new BizException("来源不能为空");
        }

        if (StringUtils.isBlank(request.getChannel())) {
            throw new BizException("渠道不能为空");
        }

        // 初始化上下文追踪信息
        context.setTraceId(request.getTraceId());
        context.addExecutedNode(getNodeName());

        log.info("【参数校验节点】校验通过");
        return null; // 继续流程
    }

    @Override
    public FlowNode<TrialBalanceRequest, TrialBalanceContext, TrialBalanceResult> route(
            TrialBalanceRequest request, TrialBalanceContext context) {
        return flowControlNode;
    }
}
