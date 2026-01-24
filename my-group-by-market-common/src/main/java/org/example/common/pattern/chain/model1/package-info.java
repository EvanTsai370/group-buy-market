/**
 * 责任链模式 - Model1（单例链）
 *
 * <h2>设计理念</h2>
 * <p>适用于简单流程，全局只有一条责任链的场景。链路节点本身既负责业务处理，又负责链路管理。
 *
 * <h2>核心组件</h2>
 * <ul>
 *   <li>{@link org.example.common.pattern.chain.model1.IChainNode} - 责任链节点接口</li>
 *   <li>{@link org.example.common.pattern.chain.model1.AbstractChainNode} - 抽象责任链节点</li>
 * </ul>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 1. 定义请求和响应
 * public class Request {
 *     private String userId;
 *     private String productId;
 * }
 *
 * public class Response {
 *     private boolean success;
 *     private String message;
 * }
 *
 * public class Context {
 *     private String activityId;
 * }
 *
 * // 2. 实现节点
 * public class ValidationNode extends AbstractChainNode<Request, Context, Response> {
 *     @Override
 *     public Response execute(Request request, Context context) throws Exception {
 *         if (request.getUserId() == null) {
 *             return Response.error("用户ID不能为空");
 *         }
 *         return nextNode(request, context);
 *     }
 * }
 *
 * public class BusinessNode extends AbstractChainNode<Request, Context, Response> {
 *     @Override
 *     public Response execute(Request request, Context context) throws Exception {
 *         // 业务逻辑
 *         return Response.success("处理成功");
 *     }
 * }
 *
 * // 3. 组装责任链
 * ValidationNode validationNode = new ValidationNode();
 * BusinessNode businessNode = new BusinessNode();
 * validationNode.appendNext(businessNode);
 *
 * // 4. 执行
 * Response response = validationNode.execute(request, context);
 * }</pre>
 *
 * <h2>适用场景</h2>
 * <ul>
 *   <li>流程固定，不需要动态调整</li>
 *   <li>全局只有一条责任链</li>
 *   <li>节点数量较少（3-5个）</li>
 * </ul>
 *
 * <h2>优缺点</h2>
 * <table border="1">
 *   <tr>
 *     <th>优点</th>
 *     <th>缺点</th>
 *   </tr>
 *   <tr>
 *     <td>实现简单，代码量少</td>
 *     <td>不支持多条链，灵活性差</td>
 *   </tr>
 *   <tr>
 *     <td>链路结构清晰，易于理解</td>
 *     <td>节点耦合链路管理，职责不清</td>
 *   </tr>
 *   <tr>
 *     <td>适合简单场景</td>
 *     <td>不适合复杂业务</td>
 *   </tr>
 * </table>
 *
 */
package org.example.common.pattern.chain.model1;
