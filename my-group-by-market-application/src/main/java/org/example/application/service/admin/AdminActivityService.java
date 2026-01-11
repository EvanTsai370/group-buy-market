package org.example.application.service.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.exception.BizException;
import org.example.domain.model.activity.Activity;
import org.example.domain.model.activity.ActivityGoods;
import org.example.domain.model.activity.Discount;
import org.example.domain.model.activity.repository.ActivityRepository;
import org.example.domain.model.activity.valueobject.DiscountType;
import org.example.domain.model.activity.valueobject.GroupType;
import org.example.domain.model.activity.valueobject.TagScope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 活动管理服务
 * 
 * @author 开发团队
 * @since 2026-01-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminActivityService {

    private final ActivityRepository activityRepository;

    // ==================== 活动管理 ====================

    /**
     * 获取活动列表
     */
    public List<Activity> listActivities(int page, int size) {
        log.info("【AdminActivity】查询活动列表, page: {}, size: {}", page, size);
        return activityRepository.findAll(page, size);
    }

    /**
     * 获取活动详情
     */
    public Activity getActivityDetail(String activityId) {
        log.info("【AdminActivity】查询活动详情, activityId: {}", activityId);
        return activityRepository.findById(activityId)
                .orElseThrow(() -> new BizException("活动不存在"));
    }

    /**
     * 创建活动
     */
    @Transactional
    public Activity createActivity(CreateActivityCmd cmd) {
        log.info("【AdminActivity】创建活动, name: {}", cmd.getActivityName());

        String activityId = activityRepository.nextId();

        Activity activity = Activity.create(
                activityId,
                cmd.getActivityName(),
                cmd.getDiscountId(),
                cmd.getTagId(),
                cmd.getTagScope() != null ? cmd.getTagScope() : TagScope.STRICT,
                cmd.getGroupType() != null ? cmd.getGroupType() : GroupType.REAL,
                cmd.getTarget(),
                cmd.getValidTime(),
                cmd.getParticipationLimit(),
                cmd.getStartTime(),
                cmd.getEndTime());
        activity.setActivityDesc(cmd.getActivityDesc());

        activityRepository.save(activity);

        log.info("【AdminActivity】活动创建成功, activityId: {}", activityId);
        return activity;
    }

    /**
     * 上架活动
     */
    @Transactional
    public void activateActivity(String activityId) {
        log.info("【AdminActivity】上架活动, activityId: {}", activityId);
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new BizException("活动不存在"));

        activity.activate();
        activityRepository.update(activity);

        log.info("【AdminActivity】活动已上架, activityId: {}", activityId);
    }

    /**
     * 下架活动
     */
    @Transactional
    public void closeActivity(String activityId) {
        log.info("【AdminActivity】下架活动, activityId: {}", activityId);
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new BizException("活动不存在"));

        activity.close();
        activityRepository.update(activity);

        log.info("【AdminActivity】活动已下架, activityId: {}", activityId);
    }

    // ==================== 折扣管理 ====================

    /**
     * 获取折扣详情
     */
    public Discount getDiscount(String discountId) {
        log.info("【AdminActivity】查询折扣详情, discountId: {}", discountId);
        Discount discount = activityRepository.queryDiscountById(discountId);
        if (discount == null) {
            throw new BizException("折扣配置不存在");
        }
        return discount;
    }

    /**
     * 创建折扣配置
     */
    @Transactional
    public Discount createDiscount(CreateDiscountCmd cmd) {
        log.info("【AdminActivity】创建折扣, name: {}", cmd.getDiscountName());

        String discountId = activityRepository.nextDiscountId();

        Discount discount = new Discount();
        discount.setDiscountId(discountId);
        discount.setDiscountName(cmd.getDiscountName());
        discount.setDiscountDesc(cmd.getDiscountDesc());
        discount.setDiscountAmount(cmd.getDiscountAmount());
        discount.setDiscountType(cmd.getDiscountType() != null ? cmd.getDiscountType() : DiscountType.BASE);
        discount.setMarketPlan(cmd.getMarketPlan());
        discount.setMarketExpr(cmd.getMarketExpr());
        discount.setTagId(cmd.getTagId());
        discount.setCreateTime(LocalDateTime.now());
        discount.setUpdateTime(LocalDateTime.now());

        activityRepository.saveDiscount(discount);

        log.info("【AdminActivity】折扣创建成功, discountId: {}", discountId);
        return discount;
    }

    // ==================== 活动商品关联 ====================

    /**
     * 添加活动商品关联
     */
    @Transactional
    public void addActivityGoods(String activityId, String goodsId,
            String source, String channel, String discountId) {
        log.info("【AdminActivity】添加活动商品关联, activityId: {}, goodsId: {}", activityId, goodsId);

        // 检查活动是否存在
        activityRepository.findById(activityId)
                .orElseThrow(() -> new BizException("活动不存在"));

        ActivityGoods activityGoods = new ActivityGoods(activityId, goodsId, source, channel, discountId);
        activityRepository.saveActivityGoods(activityGoods);

        log.info("【AdminActivity】活动商品关联添加成功");
    }

    /**
     * 查询活动商品关联
     */
    public ActivityGoods getActivityGoods(String activityId, String goodsId,
            String source, String channel) {
        return activityRepository.queryActivityGoods(activityId, goodsId, source, channel);
    }

    // ==================== 命令对象 ====================

    @lombok.Data
    public static class CreateActivityCmd {
        private String activityName;
        private String activityDesc;
        private String discountId;
        private String tagId;
        private TagScope tagScope;
        private GroupType groupType;
        private Integer target;
        private Integer validTime;
        private Integer participationLimit;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
    }

    @lombok.Data
    public static class CreateDiscountCmd {
        private String discountName;
        private String discountDesc;
        private BigDecimal discountAmount;
        private DiscountType discountType;
        private String marketPlan; // ZJ/ZK/N/MJ
        private String marketExpr; // 例如 "0.9" 表示9折
        private String tagId;
    }
}
