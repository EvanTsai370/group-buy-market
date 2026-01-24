package org.example.domain.model.trade.filter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.common.pattern.chain.model2.IChainResponse;

/**
 * 交易规则过滤响应对象
 *
 * <p>封装过滤结果（允许或拒绝）
 *
 * <p>改进说明：
 * <ul>
 *   <li>实现 {@link IChainResponse} 接口，通过 {@link #shouldContinue()} 明确表达是否继续责任链</li>
 *   <li>替代原有的 null 判断逻辑，提升代码可读性和类型安全</li>
 * </ul>
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeFilterResponse implements IChainResponse {

    /** 是否允许交易（true=允许继续, false=拒绝中断） */
    private boolean allowed;

    /** 拒绝原因（allowed=false时必填） */
    private String reason;

    /**
     * 判断是否应该继续执行责任链
     *
     * @return true=继续下一个处理器, false=中断责任链
     */
    @Override
    public boolean shouldContinue() {
        return allowed;
    }

    /**
     * 获取拒绝原因
     *
     * @return 拒绝原因（允许时返回null）
     */
    @Override
    public String getReason() {
        return reason;
    }

    /**
     * 创建允许响应（继续下一个处理器）
     *
     * @return 允许响应
     */
    public static TradeFilterResponse allow() {
        return TradeFilterResponse.builder()
                .allowed(true)
                .build();
    }

    /**
     * 创建拒绝响应（中断责任链）
     *
     * @param reason 拒绝原因
     * @return 拒绝响应
     */
    public static TradeFilterResponse reject(String reason) {
        return TradeFilterResponse.builder()
                .allowed(false)
                .reason(reason)
                .build();
    }
}
