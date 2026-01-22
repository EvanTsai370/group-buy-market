package org.example.domain.service.validation;

import lombok.extern.slf4j.Slf4j;
import org.example.domain.model.activity.Activity;
import org.example.domain.model.activity.valueobject.TagScope;
import org.example.domain.model.tag.repository.CrowdTagRepository;

/**
 * 人群标签校验服务
 * 
 * <p>
 * 职责：
 * <ul>
 * <li>判断用户是否在活动的目标人群范围内</li>
 * <li>根据 TagScope 决定活动的可见性和可参与性</li>
 * </ul>
 * 
 * <p>
 * TagScope 模式：
 * <ul>
 * <li>STRICT - 严格模式：不在人群标签内则不可见不可参与</li>
 * <li>VISIBLE_ONLY - 可见模式：不在人群标签内可见但不可参与</li>
 * <li>OPEN - 开放模式：所有用户可见可参与（慎用）</li>
 * </ul>
 * 
 * @author 开发团队
 * @since 2026-01-14
 */
@Slf4j
public class CrowdTagValidationService {

    private final CrowdTagRepository crowdTagRepository;

    public CrowdTagValidationService(CrowdTagRepository crowdTagRepository) {
        this.crowdTagRepository = crowdTagRepository;
    }

    /**
     * 校验用户是否可参与活动
     * 
     * <p>
     * 业务规则：
     * <ol>
     * <li>活动未配置人群标签 → 所有用户可见可参与</li>
     * <li>用户在人群标签内 → 可见可参与</li>
     * <li>用户不在人群标签内 → 根据 TagScope 决定：
     * <ul>
     * <li>STRICT - 不可见不可参与</li>
     * <li>VISIBLE_ONLY - 可见但不可参与</li>
     * <li>OPEN - 可见可参与</li>
     * </ul>
     * </li>
     * </ol>
     * 
     * @param userId   用户ID
     * @param activity 活动信息
     * @return 校验结果
     */
    public CrowdTagValidationResult validate(String userId, Activity activity) {
        log.debug("【人群标签校验服务】开始校验，userId: {}, activityId: {}",
                userId, activity.getActivityId());

        String tagId = activity.getTagId();
        TagScope tagScope = activity.getTagScope();

        // 规则1: 如果活动未配置人群标签，则所有用户可见可参与
        if (tagId == null || tagId.trim().isEmpty()) {
            log.debug("【人群标签校验服务】活动未配置人群标签，所有用户可访问");
            return CrowdTagValidationResult.allowed();
        }

        // 规则2: 检查用户是否在人群标签内
        Boolean isInTag = crowdTagRepository.checkUserInTag(userId, tagId);

        if (Boolean.TRUE.equals(isInTag)) {
            // 用户在人群标签内：直接放行，可见可参与
            log.debug("【人群标签校验服务】用户在目标人群内，可见可参与，userId: {}, tagId: {}",
                    userId, tagId);
            return CrowdTagValidationResult.allowed();
        }

        // 规则3: 用户不在人群标签内，根据 TagScope 决定
        TagScope scope = tagScope != null ? tagScope : TagScope.STRICT; // 默认严格模式

        switch (scope) {
            case STRICT:
                // 严格模式：不可见不可参与
                log.info("【人群标签校验服务】用户不在目标人群且为严格模式，不可见不可参与，userId: {}, tagId: {}",
                        userId, tagId);
                return CrowdTagValidationResult.denied(false, "活动暂未对您开放");

            case VISIBLE_ONLY:
                // 可见模式：可见但不可参与
                log.info("【人群标签校验服务】用户不在目标人群但可见，不可参与，userId: {}, tagId: {}",
                        userId, tagId);
                return CrowdTagValidationResult.denied(true, "您不符合活动参与条件");

            case OPEN:
                // 开放模式：可见可参与（慎用）
                log.warn("【人群标签校验服务】用户不在目标人群但为开放模式，可见可参与，userId: {}, tagId: {}",
                        userId, tagId);
                return CrowdTagValidationResult.allowed();

            default:
                // 防御性编程：未知模式默认为严格模式
                log.warn("【人群标签校验服务】未知tagScope: {}，默认严格模式，userId: {}, tagId: {}",
                        scope, userId, tagId);
                return CrowdTagValidationResult.denied(false, "活动暂未对您开放");
        }
    }

    /**
     * 校验标签规则格式
     *
     * @param tagRule 标签规则JSON
     * @return 是否有效
     */
    public boolean validateRuleFormat(String tagRule) {
        if (tagRule == null || tagRule.trim().isEmpty()) {
            return false;
        }
        try {
            // 简单校验是否为合法的 JSON 格式
            // 这里可以使用 Jackson 或 Fastjson 进行解析校验，为了简单起见，暂时假设所有非空字符串都经过了前端校验
            // 在实际生产中，建议引入 ObjectMapper 进行 readTree 校验
            return tagRule.startsWith("{") && tagRule.endsWith("}");
        } catch (Exception e) {
            log.warn("【人群标签校验服务】规则格式无效, rule: {}", tagRule);
            return false;
        }
    }
}
