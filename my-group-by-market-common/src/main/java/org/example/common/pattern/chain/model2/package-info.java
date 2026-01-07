/**
 * 责任链模式 - Model2（多例链）
 *
 * <h2>设计理念</h2>
 * <p>适用于复杂业务，需要多条独立责任链的场景。处理器只负责业务逻辑，链路管理由执行器负责。
 *
 * <h2>核心组件</h2>
 * <ul>
 *   <li>{@link org.example.common.pattern.chain.model2.IChainHandler} - 责任链处理器接口</li>
 *   <li>{@link org.example.common.pattern.chain.model2.ChainExecutor} - 责任链执行器</li>
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
 * // 2. 实现处理器（只负责业务逻辑）
 * @Component
 * public class ValidationHandler implements IChainHandler<Request, Context, Response> {
 *     @Override
 *     public Response handle(Request request, Context context) throws Exception {
 *         if (request.getUserId() == null) {
 *             return Response.error("用户ID不能为空");
 *         }
 *         return pass(request, context); // 放行到下一个处理器
 *     }
 * }
 *
 * @Component
 * public class AuthHandler implements IChainHandler<Request, Context, Response> {
 *     @Override
 *     public Response handle(Request request, Context context) throws Exception {
 *         // 权限校验逻辑
 *         if (!checkAuth(request.getUserId())) {
 *             return Response.error("权限不足");
 *         }
 *         return pass(request, context);
 *     }
 * }
 *
 * @Component
 * public class BusinessHandler implements IChainHandler<Request, Context, Response> {
 *     @Override
 *     public Response handle(Request request, Context context) throws Exception {
 *         // 业务逻辑
 *         return Response.success("处理成功");
 *     }
 * }
 *
 * // 3. 组装责任链（在配置类或工厂中）
 * @Configuration
 * public class ChainConfig {
 *
 *     @Bean("normalChain")
 *     public ChainExecutor<Request, Context, Response> normalChain(
 *             ValidationHandler validation,
 *             AuthHandler auth,
 *             BusinessHandler business) {
 *         return new ChainExecutor<>("普通流程", validation, auth, business);
 *     }
 *
 *     @Bean("fastChain")
 *     public ChainExecutor<Request, Context, Response> fastChain(
 *             ValidationHandler validation,
 *             BusinessHandler business) {
 *         return new ChainExecutor<>("快速通道", validation, business);
 *     }
 * }
 *
 * // 4. 使用（在Service中注入）
 * @Service
 * public class OrderService {
 *
 *     @Resource(name = "normalChain")
 *     private ChainExecutor<Request, Context, Response> normalChain;
 *
 *     @Resource(name = "fastChain")
 *     private ChainExecutor<Request, Context, Response> fastChain;
 *
 *     public Response process(Request request, boolean useFastTrack) {
 *         ChainExecutor<Request, Context, Response> chain =
 *             useFastTrack ? fastChain : normalChain;
 *
 *         return chain.execute(request, new Context());
 *     }
 * }
 * }</pre>
 *
 * <h2>适用场景</h2>
 * <ul>
 *   <li>需要多条独立的责任链（如：普通流程、快速通道、VIP流程）</li>
 *   <li>链路需要动态组装或调整</li>
 *   <li>处理器需要被不同的链复用</li>
 *   <li>节点数量较多（5个以上）</li>
 * </ul>
 *
 * <h2>优缺点</h2>
 * <table border="1">
 *   <tr>
 *     <th>优点</th>
 *     <th>缺点</th>
 *   </tr>
 *   <tr>
 *     <td>职责清晰，处理器只负责业务逻辑</td>
 *     <td>代码量稍多，需要执行器管理</td>
 *   </tr>
 *   <tr>
 *     <td>支持多条链，灵活性高</td>
 *     <td>理解成本稍高</td>
 *   </tr>
 *   <tr>
 *     <td>处理器可复用，易于组合</td>
 *     <td>-</td>
 *   </tr>
 *   <tr>
 *     <td>支持动态调整链路</td>
 *     <td>-</td>
 *   </tr>
 * </table>
 *
 * <h2>与 Model1 的对比</h2>
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
 *   <tr>
 *     <td>适用场景</td>
 *     <td>简单流程，固定链路</td>
 *     <td>复杂业务，多条链路</td>
 *   </tr>
 *   <tr>
 *     <td>代码复杂度</td>
 *     <td>低</td>
 *     <td>中</td>
 *   </tr>
 * </table>
 *
 * @author 开发团队
 * @since 2026-01-04
 */
package org.example.common.pattern.chain.model2;
