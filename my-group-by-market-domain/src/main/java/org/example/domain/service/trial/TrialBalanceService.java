package org.example.domain.service.trial;

import lombok.extern.slf4j.Slf4j;
import org.example.domain.model.activity.TrialBalanceRequest;
import org.example.domain.model.activity.TrialBalanceResult;
import org.example.domain.model.activity.context.TrialBalanceContext;
import org.example.domain.service.trial.node.*;

/**
 * 拼团试算领域服务
 * 职责：编排试算流程，协调各个节点执行
 */
@Slf4j
public class TrialBalanceService {

    private final ParameterValidationNode parameterValidationNode;

    public TrialBalanceService(
            ParameterValidationNode parameterValidationNode,
            FlowControlNode flowControlNode,
            DiscountCalculationNode discountCalculationNode,
            CrowdTagValidationNode crowdTagValidationNode,
            ResultAssemblyNode resultAssemblyNode,
            ErrorHandlingNode errorHandlingNode) {

        // 设置节点之间的依赖关系（责任链）
        parameterValidationNode.setFlowControlNode(flowControlNode);
        flowControlNode.setDiscountCalculationNode(discountCalculationNode);
        discountCalculationNode.setCrowdTagValidationNode(crowdTagValidationNode);
        discountCalculationNode.setErrorHandlingNode(errorHandlingNode);
        crowdTagValidationNode.setResultAssemblyNode(resultAssemblyNode);

        this.parameterValidationNode = parameterValidationNode;

        log.info("【试算服务】流程节点初始化完成");
    }

    /**
     * 执行拼团试算
     *
     * @param request 试算请求
     * @return 试算结果
     */
    public TrialBalanceResult execute(TrialBalanceRequest request) {
        log.info("【试算服务】开始执行试算，userId: {}, goodsId: {}",
                 request.getUserId(), request.getGoodsId());

        // 创建上下文
        TrialBalanceContext context = TrialBalanceContext.builder().build();

        // 从根节点开始执行流程
        TrialBalanceResult result = parameterValidationNode.execute(request, context);

        log.info("【试算服务】试算完成，执行节点: {}", context.getExecutedNodes());
        return result;
    }
}
