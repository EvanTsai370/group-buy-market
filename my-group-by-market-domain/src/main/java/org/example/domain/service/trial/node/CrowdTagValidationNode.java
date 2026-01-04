package org.example.domain.service.trial.node;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.example.common.pattern.flow.AbstractAsyncDataFlowNode;
import org.example.common.pattern.flow.DataLoader;
import org.example.common.pattern.flow.FlowNode;
import org.example.domain.model.activity.Activity;
import org.example.domain.model.activity.TrialBalanceRequest;
import org.example.domain.model.activity.TrialBalanceResult;
import org.example.domain.model.activity.context.TrialBalanceContext;
import org.example.domain.model.activity.repository.ActivityRepository;
import org.example.domain.model.activity.valueobject.TagScope;
import org.example.domain.model.tag.repository.CrowdTagRepository;
import org.example.domain.service.trial.loader.ActivityDataLoader;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * 人群标签校验节点
 * 职责：
 * 1. 异步加载活动配置
 * 2. 判断用户是否在活动的目标人群范围内
 */
@Slf4j
@Setter
public class CrowdTagValidationNode extends AbstractAsyncDataFlowNode<TrialBalanceRequest, TrialBalanceContext, TrialBalanceResult> {

    private final ActivityRepository activityRepository;
    private final CrowdTagRepository crowdTagRepository;
    private DiscountCalculationNode discountCalculationNode;
    private ResultAssemblyNode resultAssemblyNode;

    public CrowdTagValidationNode(
            ActivityRepository activityRepository,
            CrowdTagRepository crowdTagRepository,
            ExecutorService executorService) {
        this.activityRepository = activityRepository;
        this.crowdTagRepository = crowdTagRepository;
        this.executorService = executorService;
        this.dataLoadTimeoutSeconds = 5L;
    }

    @Override
    protected List<DataLoader<TrialBalanceRequest, TrialBalanceContext>> getDataLoaders() {
        // 只加载活动配置，人群标签校验只需要 Activity
        return List.of(new ActivityDataLoader(activityRepository));
    }

    @Override
    protected TrialBalanceResult doExecute(TrialBalanceRequest request, TrialBalanceContext context) {
        log.info("【人群标签校验节点】开始校验，userId: {}", request.getUserId());

        Activity activity = context.getActivity();
        String tagId = activity.getTagId();
        TagScope tagScope = activity.getTagScope();

        // 规则1: 如果活动未配置人群标签，则所有用户可见可参与
        if (tagId == null || tagId.trim().isEmpty()) {
            context.setVisible(true);
            context.setParticipable(true);
            log.info("【人群标签校验节点】活动未配置人群标签，所有用户可访问");
            context.addExecutedNode(getNodeName());
            return null;
        }

        // 规则2: 配置了人群标签，检查用户是否在标签内
        Boolean isInTag = crowdTagRepository.checkUserInTag(request.getUserId(), tagId);

        if (Boolean.TRUE.equals(isInTag)) {
            // 用户在人群标签内：直接放行，可见可参与
            context.setVisible(true);
            context.setParticipable(true);
            log.info("【人群标签校验节点】用户在目标人群内，可见可参与，userId: {}, tagId: {}",
                     request.getUserId(), tagId);
        } else {
            // 用户不在人群标签内：根据 tagScope 决定可见性和参与性
            TagScope scope = tagScope != null ? tagScope : TagScope.STRICT;  // 默认严格模式

            switch (scope) {
                case STRICT:
                    // 严格模式：不可见不可参与
                    context.setVisible(false);
                    context.setParticipable(false);
                    log.info("【人群标签校验节点】用户不在目标人群且为严格模式，不可见不可参与，userId: {}, tagId: {}",
                             request.getUserId(), tagId);
                    break;

                case VISIBLE_ONLY:
                    // 可见模式：可见但不可参与
                    context.setVisible(true);
                    context.setParticipable(false);
                    log.info("【人群标签校验节点】用户不在目标人群但可见，不可参与，userId: {}, tagId: {}",
                             request.getUserId(), tagId);
                    break;

                case OPEN:
                    // 开放模式：可见可参与（慎用）
                    context.setVisible(true);
                    context.setParticipable(true);
                    log.warn("【人群标签校验节点】用户不在目标人群但为开放模式，可见可参与，userId: {}, tagId: {}",
                             request.getUserId(), tagId);
                    break;

                default:
                    // 防御性编程：未知模式默认为严格模式
                    context.setVisible(false);
                    context.setParticipable(false);
                    log.warn("【人群标签校验节点】未知tagScope: {}，默认严格模式，userId: {}, tagId: {}",
                             scope, request.getUserId(), tagId);
            }
        }

        context.addExecutedNode(getNodeName());
        return null;
    }

    @Override
    public FlowNode<TrialBalanceRequest, TrialBalanceContext, TrialBalanceResult> route(
            TrialBalanceRequest request, TrialBalanceContext context) {
        // 路由规则：
        // 1. 不可见用户（visible=false）→ 直接返回结果，不计算折扣
        // 2. 仅可见用户（visible=true, participable=false）→ 直接返回结果，不计算折扣
        // 3. 可参与用户（visible=true, participable=true）→ 继续计算折扣

        if (!context.isVisible()) {
            // 不可见：直接跳过折扣计算，返回结果
            log.info("【人群标签校验节点】用户不可见活动，跳过折扣计算，userId: {}", request.getUserId());
            return resultAssemblyNode;
        }

        if (!context.isParticipable()) {
            // 仅可见不可参与：跳过折扣计算，只返回活动基本信息
            log.info("【人群标签校验节点】用户仅可见活动，跳过折扣计算，userId: {}", request.getUserId());
            return resultAssemblyNode;
        }

        // 可见可参与：继续到折扣计算节点
        log.info("【人群标签校验节点】用户可参与活动，继续计算折扣，userId: {}", request.getUserId());
        return discountCalculationNode;
    }
}
