package org.example.domain.service.trial.loader;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.example.common.pattern.flow.DataLoader;
import org.example.domain.model.activity.Activity;
import org.example.domain.model.activity.TrialBalanceRequest;
import org.example.domain.model.activity.context.TrialBalanceContext;
import org.example.domain.model.activity.repository.ActivityRepository;

/**
 * 活动配置数据加载器
 * 负责异步加载活动配置信息
 */
@Slf4j
@AllArgsConstructor
public class ActivityDataLoader implements DataLoader<TrialBalanceRequest, TrialBalanceContext> {

    private final ActivityRepository activityRepository;

    @Override
    public void loadData(TrialBalanceRequest request, TrialBalanceContext context) {
        log.info("【数据加载器】开始加载活动配置，skuId: {}, activityId: {}",
                 request.getSkuId(), request.getActivityId());

        try {
            String activityId = request.getActivityId();

            // 如果没有指定活动ID，根据商品ID、来源、渠道查询
            if (StringUtils.isBlank(activityId)) {
                activityId = activityRepository.queryActivityIdByGoodsSourceChannel(
                    request.getSkuId(),
                    request.getSource(),
                    request.getChannel()
                );
            }

            if (StringUtils.isNotBlank(activityId)) {
                // 查询活动详情
                Activity activity = activityRepository.findById(activityId)
                    .orElse(null);

                if (activity != null) {
                    context.setActivity(activity);
                    log.info("【数据加载器】活动配置加载完成，activityId: {}, activityName: {}",
                             activityId, activity.getActivityName());
                } else {
                    log.warn("【数据加载器】未找到活动配置，activityId: {}", activityId);
                }
            } else {
                log.info("【数据加载器】商品无关联活动，skuId: {}", request.getSkuId());
            }

        } catch (Exception e) {
            log.error("【数据加载器】活动配置加载失败，skuId: {}", request.getSkuId(), e);
            throw new RuntimeException("活动配置加载失败", e);
        }
    }
}
