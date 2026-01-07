package org.example.common.pattern.chain.model2;

/**
 * 责任链处理器接口（Model2 - 多例链）
 *
 * <p>设计说明：
 * <ul>
 *   <li>适用场景：复杂业务，需要多条独立的责任链</li>
 *   <li>特点：处理器只负责业务逻辑，链路管理由 ChainExecutor 负责</li>
 *   <li>优势：解耦业务逻辑和链路管理，支持动态组装多条链</li>
 *   <li>改进：响应对象必须实现 {@link IChainResponse} 接口，显式表达是否继续</li>
 * </ul>
 *
 * <p>与 Model1 的区别：
 * <table border="1">
 *   <tr>
 *     <th>维度</th>
 *     <th>Model1（单例链）</th>
 *     <th>Model2（多例链）</th>
 *   </tr>
 *   <tr>
 *     <td>链路管理</td>
 *     <td>节点自己管理 next 指针</td>
 *     <td>由 ChainExecutor 统一管理</td>
 *   </tr>
 *   <tr>
 *     <td>处理器职责</td>
 *     <td>业务逻辑 + 链路管理</td>
 *     <td>只负责业务逻辑</td>
 *   </tr>
 *   <tr>
 *     <td>链路复用</td>
 *     <td>不支持（全局单例）</td>
 *     <td>支持（可创建多条链）</td>
 *   </tr>
 * </table>
 *
 * @param <REQUEST> 请求参数类型
 * @param <CONTEXT> 动态上下文类型（用于在处理器间传递数据）
 * @param <RESPONSE> 响应结果类型（必须实现 {@link IChainResponse} 接口）
 *
 * @author 开发团队
 * @since 2026-01-04
 */
public interface IChainHandler<REQUEST, CONTEXT, RESPONSE extends IChainResponse> {

    /**
     * 执行当前处理器的业务逻辑
     *
     * <p>执行规则（改进后）：
     * <ol>
     *   <li>如果返回 response.shouldContinue() == true：表示当前处理器放行，继续执行下一个处理器</li>
     *   <li>如果返回 response.shouldContinue() == false：表示业务处理失败，中断责任链，直接返回结果</li>
     * </ol>
     *
     * <p>注意：
     * <ul>
     *   <li>处理器不需要关心下一个节点是谁（由 ChainExecutor 管理）</li>
     *   <li>处理器不需要调用 next()（由 ChainExecutor 自动调用）</li>
     *   <li>不推荐返回 null，应该显式返回 Response 对象</li>
     * </ul>
     *
     * @param request 请求参数
     * @param context 动态上下文（可在处理器间传递数据）
     * @return 执行结果（必须实现 {@link IChainResponse} 接口）
     * @throws Exception 业务异常
     */
    RESPONSE handle(REQUEST request, CONTEXT context) throws Exception;
}
