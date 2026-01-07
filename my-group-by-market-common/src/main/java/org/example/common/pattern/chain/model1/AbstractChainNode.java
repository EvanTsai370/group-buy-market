package org.example.common.pattern.chain.model1;

/**
 * 抽象责任链节点（Model1 - 单例链）
 *
 * <p>设计说明：
 * <ul>
 *   <li>封装了链路管理的通用逻辑（getNext、appendNext）</li>
 *   <li>提供了 nextNode() 方法简化调用下一个节点的代码</li>
 *   <li>子类只需实现 execute() 方法专注于业务逻辑</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 * public class ValidationNode extends AbstractChainNode<Request, Context, Response> {
 *     @Override
 *     public Response execute(Request request, Context context) throws Exception {
 *         // 1. 执行当前节点的业务逻辑
 *         if (!validateRequest(request)) {
 *             return Response.error("参数校验失败");
 *         }
 *
 *         // 2. 继续执行下一个节点
 *         return nextNode(request, context);
 *     }
 * }
 * }</pre>
 *
 * @param <REQUEST> 请求参数类型
 * @param <CONTEXT> 动态上下文类型
 * @param <RESPONSE> 响应结果类型
 *
 * @author 开发团队
 * @since 2026-01-04
 */
public abstract class AbstractChainNode<REQUEST, CONTEXT, RESPONSE>
        implements IChainNode<REQUEST, CONTEXT, RESPONSE> {

    /** 下一个节点 */
    private IChainNode<REQUEST, CONTEXT, RESPONSE> next;

    @Override
    public IChainNode<REQUEST, CONTEXT, RESPONSE> getNext() {
        return this.next;
    }

    @Override
    public IChainNode<REQUEST, CONTEXT, RESPONSE> appendNext(
            IChainNode<REQUEST, CONTEXT, RESPONSE> next) {
        this.next = next;
        return next;
    }

    /**
     * 执行下一个节点（便捷方法）
     *
     * <p>注意：如果当前节点是最后一个节点，则返回null
     *
     * @param request 请求参数
     * @param context 动态上下文
     * @return 下一个节点的执行结果，如果没有下一个节点则返回null
     * @throws Exception 业务异常
     */
    protected RESPONSE nextNode(REQUEST request, CONTEXT context) throws Exception {
        if (this.next == null) {
            return null;
        }
        return this.next.execute(request, context);
    }
}
