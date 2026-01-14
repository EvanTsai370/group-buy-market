package org.example.domain.service.validation;

import lombok.extern.slf4j.Slf4j;
import org.example.common.exception.BizException;
import org.example.domain.model.activity.repository.ActivityRepository;

/**
 * 流控服务
 * 
 * <p>
 * 职责：
 * <ul>
 * <li>降级开关检查 - 系统降级时拒绝所有请求</li>
 * <li>切量灰度控制 - 根据用户ID判断是否在灰度范围内</li>
 * </ul>
 * 
 * <p>
 * 使用场景：
 * <ul>
 * <li>锁单流程 - 作为第一道防线，在用户参团时校验</li>
 * <li>价格试算 - 返回是否可参与标志，供前端展示</li>
 * </ul>
 * 
 * @author 开发团队
 * @since 2026-01-14
 */
@Slf4j
public class FlowControlService {

    private final ActivityRepository activityRepository;

    public FlowControlService(ActivityRepository activityRepository) {
        this.activityRepository = activityRepository;
    }

    /**
     * 校验流控
     * 
     * <p>
     * 业务规则：
     * <ol>
     * <li>降级开关打开 → 抛出异常（系统繁忙）</li>
     * <li>用户不在切量范围 → 抛出异常（活动未开放）</li>
     * <li>校验通过 → 正常返回</li>
     * </ol>
     * 
     * @param userId 用户ID
     * @throws BizException 流控校验不通过时抛出
     */
    public void validateFlowControl(String userId) {
        log.debug("【流控服务】开始校验，userId: {}", userId);

        // 1. 降级开关检查
        if (activityRepository.isDowngraded()) {
            log.warn("【流控服务】系统降级中，拒绝请求，userId: {}", userId);
            throw new BizException("系统繁忙，请稍后再试");
        }

        // 2. 切量灰度控制
        if (!activityRepository.isInCutRange(userId)) {
            log.info("【流控服务】用户不在切量范围内，userId: {}", userId);
            throw new BizException("活动暂未对您开放");
        }

        log.debug("【流控服务】校验通过，userId: {}", userId);
    }

    /**
     * 检查流控（不抛异常版本）
     * 
     * <p>
     * 用于价格试算等场景，只返回是否通过，不抛出异常
     * 
     * @param userId 用户ID
     * @return true - 流控通过；false - 流控不通过
     */
    public boolean checkFlowControl(String userId) {
        try {
            validateFlowControl(userId);
            return true;
        } catch (BizException e) {
            return false;
        }
    }
}
