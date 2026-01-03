package org.example.domain.service.trial.loader;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.example.common.pattern.flow.DataLoader;
import org.example.domain.model.activity.Activity;
import org.example.domain.model.activity.Discount;
import org.example.domain.model.activity.TrialBalanceRequest;
import org.example.domain.model.activity.context.TrialBalanceContext;
import org.example.domain.model.activity.repository.ActivityRepository;

/**
 * 折扣配置数据加载器
 * 依赖活动配置，需要在活动加载之后执行
 */
@Slf4j
@AllArgsConstructor
public class DiscountDataLoader implements DataLoader<TrialBalanceRequest, TrialBalanceContext> {

    private final ActivityRepository activityRepository;

    @Override
    public void loadData(TrialBalanceRequest request, TrialBalanceContext context) {
        log.info("【数据加载器】开始加载折扣配置");

        try {
            Activity activity = context.getActivity();
            if (activity == null) {
                log.info("【数据加载器】活动配置为空，跳过折扣加载");
                return;
            }

            String discountId = activity.getDiscountId();
            if (StringUtils.isBlank(discountId)) {
                log.warn("【数据加载器】活动未配置折扣，activityId: {}", activity.getActivityId());
                return;
            }

            // 查询折扣配置
            Discount discount = activityRepository.queryDiscountById(discountId);
            if (discount != null) {
                context.setDiscount(discount);
                log.info("【数据加载器】折扣配置加载完成，discountId: {}, discountName: {}",
                         discountId, discount.getDiscountName());
            } else {
                log.warn("【数据加载器】未找到折扣配置，discountId: {}", discountId);
            }

        } catch (Exception e) {
            log.error("【数据加载器】折扣配置加载失败", e);
            throw new RuntimeException("折扣配置加载失败", e);
        }
    }
}
