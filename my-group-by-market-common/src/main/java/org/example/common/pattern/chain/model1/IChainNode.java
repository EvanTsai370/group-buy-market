package org.example.common.pattern.chain.model1;

/**
 * 责任链节点接口（Model1 - 单例链）
 *
 * <p>设计说明：
 * <ul>
 *   <li>适用场景：简单流程，全局只有一条责任链</li>
 *   <li>特点：链路节点本身既负责业务处理，又负责链路管理</li>
 *   <li>使用方式：通过 appendNext 方法手动组装链路</li>
 * </ul>
 *
 * @param <REQUEST> 请求参数类型
 * @param <CONTEXT> 动态上下文类型（用于在节点间传递数据）
 * @param <RESPONSE> 响应结果类型
 *
 */
public interface IChainNode<REQUEST, CONTEXT, RESPONSE> {

    /**
     * 执行当前节点的业务逻辑
     *
     * <p>执行流程：
     * <ol>
     *   <li>处理当前节点的业务逻辑</li>
     *   <li>根据业务结果决定是否继续执行下一个节点</li>
     *   <li>如果需要继续，调用 nextNode() 方法</li>
     *   <li>如果需要中断，直接返回结果</li>
     * </ol>
     *
     * @param request 请求参数
     * @param context 动态上下文（可在节点间传递数据）
     * @return 执行结果（如果返回非null，则不再执行后续节点）
     * @throws Exception 业务异常
     */
    RESPONSE execute(REQUEST request, CONTEXT context) throws Exception;

    /**
     * 获取下一个节点
     *
     * @return 下一个节点，如果是最后一个节点则返回null
     */
    IChainNode<REQUEST, CONTEXT, RESPONSE> getNext();

    /**
     * 设置下一个节点（链式调用）
     *
     * @param next 下一个节点
     * @return 下一个节点（支持链式调用）
     */
    IChainNode<REQUEST, CONTEXT, RESPONSE> appendNext(IChainNode<REQUEST, CONTEXT, RESPONSE> next);
}
