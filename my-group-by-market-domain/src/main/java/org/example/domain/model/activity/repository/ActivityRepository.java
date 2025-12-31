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
     * 生成下一个活动ID
     *
     * @return 活动ID
     */
    String nextId();
}