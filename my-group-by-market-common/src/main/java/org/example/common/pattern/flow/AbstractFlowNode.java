package org.example.common.pattern.flow;

import lombok.extern.slf4j.Slf4j;

/**
 * 抽象流程节点
 * 提供流程节点和路由器的统一实现
 *
 * @param <REQUEST> 请求参数类型
 * @param <CONTEXT> 流程上下文类型
 * @param <RESPONSE> 响应结果类型
 */
@Slf4j
public abstract class AbstractFlowNode<REQUEST, CONTEXT, RESPONSE> 
        implements FlowNode<REQUEST, CONTEXT, RESPONSE>, 
                   FlowRouter<REQUEST, CONTEXT, RESPONSE> {

    /**
     * 默认的终止节点（返回null表示流程结束）
     */
    protected static final FlowNode DEFAULT_END_NODE = (request, context) -> null;

    @Override
    public RESPONSE execute(REQUEST request, CONTEXT context) {
        log.info("【流程节点】执行节点: {}", getNodeName());

        try {
            // 1. 执行节点业务逻辑
            RESPONSE response = doExecute(request, context);

            // 2. 路由到下一个节点
            FlowNode<REQUEST, CONTEXT, RESPONSE> nextNode = route(request, context);
            if (nextNode != null) {
                return nextNode.execute(request, context);
            }

            // 3. 返回结果
            return response;

        } catch (Exception e) {
            log.error("【流程节点】节点执行异常: {}", getNodeName(), e);
            return handleException(request, context, e);
        }
    }

    /**
     * 执行节点业务逻辑（子类实现）
     *
     * @param request 请求参数
     * @param context 流程上下文
     * @return 执行结果
     */
    protected abstract RESPONSE doExecute(REQUEST request, CONTEXT context);

    /**
     * 路由到下一个节点（子类实现）
     * 默认返回null，表示流程结束
     *
     * @param request 请求参数
     * @param context 流程上下文
     * @return 下一个节点
     */
    @Override
    public FlowNode<REQUEST, CONTEXT, RESPONSE> route(REQUEST request, CONTEXT context) {
        return null;
    }

    /**
     * 异常处理（子类可覆盖）
     *
     * @param request 请求参数
     * @param context 流程上下文
     * @param e 异常
     * @return 异常处理后的结果
     */
    protected RESPONSE handleException(REQUEST request, CONTEXT context, Exception e) {
        throw new RuntimeException(e);
    }
}