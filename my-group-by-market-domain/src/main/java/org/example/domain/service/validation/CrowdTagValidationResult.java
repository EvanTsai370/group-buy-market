package org.example.domain.service.validation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 人群标签校验结果
 * 
 * <p>
 * 封装人群标签校验的结果，包括：
 * <ul>
 * <li>可见性（visible）- 活动是否对用户可见</li>
 * <li>可参与性（participable）- 用户是否可以参与活动</li>
 * <li>原因（reason）- 不可参与的原因说明</li>
 * </ul>
 * 
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrowdTagValidationResult {

    /**
     * 活动是否可见
     * true - 用户可以看到活动信息
     * false - 活动对用户隐藏（严格模式下人群标签不匹配）
     */
    private boolean visible;

    /**
     * 是否可参与
     * true - 用户可以参与拼团
     * false - 用户不可参与（可能因为人群标签不匹配、流控限制等）
     */
    private boolean participable;

    /**
     * 不可参与的原因
     * 仅当 participable=false 时有值
     */
    private String reason;

    /**
     * 创建允许参与的结果
     */
    public static CrowdTagValidationResult allowed() {
        return CrowdTagValidationResult.builder()
                .visible(true)
                .participable(true)
                .build();
    }

    /**
     * 创建不允许参与的结果
     * 
     * @param visible 是否可见
     * @param reason  原因
     */
    public static CrowdTagValidationResult denied(boolean visible, String reason) {
        return CrowdTagValidationResult.builder()
                .visible(visible)
                .participable(false)
                .reason(reason)
                .build();
    }
}
