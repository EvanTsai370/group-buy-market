package org.example.common.pattern.flow;

/**
 * 流程节点接口
 * 定义流程节点的基本行为
 *
 * @param <REQUEST> 请求参数类型
 * @param <CONTEXT> 流程上下文类型
 * @param <RESPONSE> 响应结果类型
 */
public interface FlowNode<REQUEST, CONTEXT, RESPONSE> {

    /**
     * 执行节点逻辑
     *
     * @param request 请求参数
     * @param context 流程上下文
     * @return 执行结果
     */
    RESPONSE execute(REQUEST request, CONTEXT context);

    /**
     * 获取节点名称（用于日志追踪）
     *
     * @return 节点名称
     */
    default String getNodeName() {
        return this.getClass().getSimpleName();
    }
}