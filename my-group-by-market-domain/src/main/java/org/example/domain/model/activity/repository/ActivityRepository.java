package org.example.domain.model.activity.repository;

import org.example.common.model.PageResult;
import org.example.domain.model.activity.Activity;
import org.example.domain.model.activity.ActivityGoods;
import org.example.domain.model.activity.Discount;

import java.util.List;
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
     * 更新活动
     *
     * @param activity 活动聚合
     */
    void update(Activity activity);

    /**
     * 根据ID查找活动
     *
     * @param activityId 活动ID
     * @return 活动聚合
     */
    Optional<Activity> findById(String activityId);

    /**
     * 分页查询活动列表
     *
     * @param page 页码（从1开始）
     * @param size 每页数量
     * @return 活动列表分页结果
     */
    PageResult<Activity> findByPage(int page, int size);

    /**
     * 根据SPU ID、来源、渠道查询活动ID
     * 用于试算场景：根据商品和渠道定位活动
     *
     * @param spuId   商品SPU ID
     * @param source  来源
     * @param channel 渠道
     * @return 活动ID（可能为空）
     */
    String queryActivityIdByGoodsSourceChannel(String spuId, String source, String channel);

    /**
     * 根据活动ID和商品信息查询活动商品关联
     *
     * @param activityId 活动ID
     * @param spuId      商品SPU ID
     * @param source     来源
     * @param channel    渠道
     * @return 活动商品关联信息
     */
    ActivityGoods queryActivityGoods(String activityId, String spuId, String source, String channel);

    /**
     * 查询折扣配置
     *
     * @param discountId 折扣ID
     * @return 折扣配置
     */
    Discount queryDiscountById(String discountId);

    /**
     * 保存折扣配置
     *
     * @param discount 折扣配置
     */
    void saveDiscount(Discount discount);

    /**
     * 保存活动商品关联
     *
     * @param activityGoods 活动商品关联
     */
    void saveActivityGoods(ActivityGoods activityGoods);

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

    /**
     * 生成下一个折扣ID
     *
     * @return 折扣ID
     */
    String nextDiscountId();

    /**
     * 根据SPU ID查找当前有效的活动
     * 用于 C 端商品详情页：展示商品关联的拼团活动
     *
     * @param spuId 商品SPU ID
     * @return 有效的活动（可能为空）
     */
    Optional<Activity> findActiveBySpuId(String spuId);

    /**
     * 统计当前活跃的活动数量
     *
     * @param now 当前时间
     * @return 活跃活动数量
     */
    long countActive(java.time.LocalDateTime now);
}