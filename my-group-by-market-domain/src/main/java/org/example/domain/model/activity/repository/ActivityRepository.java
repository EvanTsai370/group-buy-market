package org.example.domain.model.activity.repository;

import org.example.domain.model.activity.Activity;

import java.util.Optional;

/**
 * Activity 仓储接口
 */
public interface ActivityRepository {

    /**
     * 保存活动
     *
     * @param activity 活动聚合
     */
    void save(Activity activity);

    /**
     * 根据ID查找活动
     *
     * @param activityId 活动ID
     * @return 活动聚合
     */
    Optional<Activity> findById(String activityId);

    /**
     * 根据来源和渠道查找活动
     *
     * @param source 来源
     * @param channel 渠道
     * @return 活动聚合
     */
    Optional<Activity> findBySourceAndChannel(String source, String channel);

    /**
     * 根据来源、渠道和商品ID查询活动ID
     * 用于试算场景：根据商品和渠道定位活动
     *
     * @param source 来源
     * @param channel 渠道
     * @param goodsId 商品ID
     * @return 活动ID（可能为空）
     */
    String queryActivityIdBySourceChannelGoods(String source, String channel, String goodsId);

    /**
     * 查询折扣配置
     *
     * @param discountId 折扣ID
     * @return 折扣配置
     */
    org.example.domain.model.activity.Discount queryDiscountById(String discountId);

    /**
     * 判断是否在降级开关范围内
     *
     * @return true-降级，false-正常
     */
    boolean isDowngraded();

    /**
     * 判断用户是否在切量范围内
     *
     * @param userId 用户ID
     * @return true-在切量范围内，false-不在
     */
    boolean isInCutRange(String userId);

    /**
     * 生成下一个活动ID
     *
     * @return 活动ID
     */
    String nextId();
}