package org.example.common.pattern.flow;

/**
 * 流程路由器接口
 * 负责决定下一个要执行的节点
 *
 * @param <REQUEST> 请求参数类型
 * @param <CONTEXT> 流程上下文类型
 * @param <RESPONSE> 响应结果类型
 */
public interface FlowRouter<REQUEST, CONTEXT, RESPONSE> {

    /**
     * 根据当前状态选择下一个节点
     *
     * @param request 请求参数
     * @param context 流程上下文
     * @return 下一个节点，如果为null表示流程结束
     */
    FlowNode<REQUEST, CONTEXT, RESPONSE> route(REQUEST request, CONTEXT context);
}