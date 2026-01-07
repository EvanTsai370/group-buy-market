package org.example.common.pattern.chain.model2;

/**
 * 责任链响应接口
 *
 * <p>设计说明：
 * <ul>
 *   <li>所有责任链的响应对象必须实现此接口</li>
 *   <li>通过 {@link #shouldContinue()} 明确表达是否继续执行责任链</li>
 *   <li>避免使用 null 来判断是否继续（显式优于隐式）</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 * // 定义响应类
 * public class ValidationResponse implements IChainResponse {
 *     private boolean passed;
 *     private String reason;
 *
 *     @Override
 *     public boolean shouldContinue() {
 *         return passed;  // 校验通过则继续
 *     }
 *
 *     @Override
 *     public String getReason() {
 *         return reason;
 *     }
 * }
 *
 * // 在处理器中使用
 * public ValidationResponse handle(Request req, Context ctx) {
 *     if (校验失败) {
 *         return ValidationResponse.reject("失败原因");  // 中断链路
 *     }
 *     return ValidationResponse.allow();  // 继续下一个处理器
 * }
 * }</pre>
 *
 * @author 开发团队
 * @since 2026-01-05
 */
public interface IChainResponse {

    /**
     * 判断是否应该继续执行责任链
     *
     * <p>执行规则：
     * <ul>
     *   <li>返回 true：当前处理器放行，继续执行下一个处理器</li>
     *   <li>返回 false：当前处理器拒绝，中断责任链，立即返回结果</li>
     * </ul>
     *
     * @return true=继续执行, false=中断链路
     */
    boolean shouldContinue();

    /**
     * 获取拒绝原因（中断时使用）
     *
     * <p>注意：
     * <ul>
     *   <li>当 {@link #shouldContinue()} 返回 false 时，此方法应返回具体的拒绝原因</li>
     *   <li>当 {@link #shouldContinue()} 返回 true 时，此方法可返回 null</li>
     * </ul>
     *
     * @return 拒绝原因（继续时返回null）
     */
    default String getReason() {
        return null;
    }
}
