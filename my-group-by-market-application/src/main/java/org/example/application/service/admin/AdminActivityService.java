package org.example.application.service.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.exception.BizException;
import org.example.common.model.PageResult;
import org.example.domain.model.activity.Activity;
import org.example.domain.model.activity.ActivityGoods;
import org.example.domain.model.activity.Discount;
import org.example.domain.model.activity.repository.ActivityRepository;
import org.example.domain.model.activity.repository.DiscountRepository;
import org.example.domain.model.activity.valueobject.DiscountType;
import org.example.domain.model.activity.valueobject.GroupType;
import org.example.domain.model.activity.valueobject.TagScope;
import org.example.domain.model.goods.Spu;
import org.example.domain.model.goods.repository.SpuRepository;
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
    private final DiscountRepository discountRepository;
    private final SpuRepository spuRepository;

    // ==================== 活动管理 ====================

    /**
     * 活动列表
     */
    public PageResult<Activity> listActivities(int page, int size) {
        log.info("【AdminActivity】查询活动列表, page: {}, size: {}", page, size);
        return activityRepository.findByPage(page, size);
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

    /**
     * 更新活动
     */
    @Transactional
    public Activity updateActivity(String activityId, UpdateActivityCmd cmd) {
        log.info("【AdminActivity】更新活动, activityId: {}", activityId);

        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new BizException("活动不存在"));

        activity.update(
                cmd.getActivityName(),
                cmd.getActivityDesc(),
                cmd.getDiscountId(),
                cmd.getTagId(),
                cmd.getTagScope(),
                cmd.getGroupType(),
                cmd.getTarget(),
                cmd.getValidTime(),
                cmd.getParticipationLimit(),
                cmd.getStartTime(),
                cmd.getEndTime());

        activityRepository.update(activity);
        return activity;
    }

    /**
     * 更新活动状态
     */
    @Transactional
    public void updateActivityStatus(String activityId, String status) {
        log.info("【AdminActivity】更新活动状态, activityId: {}, status: {}", activityId, status);

        if ("ACTIVE".equals(status)) {
            activateActivity(activityId);
        } else if ("CLOSED".equals(status)) {
            closeActivity(activityId);
        } else {
            throw new BizException("不支持的状态变更: " + status);
        }
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

    /**
     * 更新折扣配置
     */
    @Transactional
    public Discount updateDiscount(String discountId, CreateDiscountCmd cmd) {
        log.info("【AdminActivity】更新折扣, discountId: {}", discountId);

        Discount discount = activityRepository.queryDiscountById(discountId);
        if (discount == null) {
            throw new BizException("折扣配置不存在");
        }

        discount.setDiscountName(cmd.getDiscountName());
        discount.setDiscountDesc(cmd.getDiscountDesc());
        discount.setDiscountAmount(cmd.getDiscountAmount());
        discount.setDiscountType(cmd.getDiscountType() != null ? cmd.getDiscountType() : DiscountType.BASE);
        discount.setMarketPlan(cmd.getMarketPlan());
        discount.setMarketExpr(cmd.getMarketExpr());
        discount.setTagId(cmd.getTagId());
        discount.setUpdateTime(LocalDateTime.now());

        activityRepository.saveDiscount(discount);

        log.info("【AdminActivity】折扣更新成功, discountId: {}", discountId);
        return discount;
    }

    /**
     * 删除折扣配置
     */
    @Transactional
    public void deleteDiscount(String discountId) {
        log.info("【AdminActivity】删除折扣, discountId: {}", discountId);

        Discount discount = activityRepository.queryDiscountById(discountId);
        if (discount == null) {
            throw new BizException("折扣配置不存在");
        }

        discountRepository.deleteById(discountId);

        log.info("【AdminActivity】折扣删除成功, discountId: {}", discountId);
    }

    // ==================== 活动商品关联 ====================

    /**
     * 添加活动商品关联
     */
    @Transactional
    public void addActivityGoods(String activityId, String spuId,
            String source, String channel, String discountId) {
        log.info("【AdminActivity】添加活动商品关联, activityId: {}, spuId: {}", activityId, spuId);

        // 检查活动是否存在
        activityRepository.findById(activityId)
                .orElseThrow(() -> new BizException("活动不存在"));

        ActivityGoods activityGoods = new ActivityGoods(activityId, spuId, source, channel, discountId);
        activityRepository.saveActivityGoods(activityGoods);

        log.info("【AdminActivity】活动商品关联添加成功");
    }

    /**
     * 查询活动商品关联
     */
    public ActivityGoods getActivityGoods(String activityId, String spuId,
            String source, String channel) {
        return activityRepository.queryActivityGoods(activityId, spuId, source, channel);
    }

    /**
     * 查询活动关联的所有商品列表
     */
    public List<ActivityGoods> listActivityGoods(String activityId) {
        log.info("【AdminActivity】查询活动关联商品列表, activityId: {}", activityId);
        return activityRepository.listActivityGoods(activityId);
    }

    /**
     * 更新活动商品关联
     * 先删除旧关联，再创建新关联
     */
    @Transactional
    public void updateActivityGoods(String activityId, String spuId,
            String source, String channel, String discountId) {
        log.info("【AdminActivity】更新活动商品关联, activityId: {}, spuId: {}", activityId, spuId);

        // 检查活动是否存在
        activityRepository.findById(activityId)
                .orElseThrow(() -> new BizException("活动不存在"));

        // 删除旧关联（使用相同的 source 和 channel）
        List<ActivityGoods> existingGoods = activityRepository.listActivityGoods(activityId);
        for (ActivityGoods goods : existingGoods) {
            if (goods.getSource().equals(source) && goods.getChannel().equals(channel)) {
                activityRepository.deleteActivityGoods(
                        activityId, goods.getSpuId(), source, channel);
            }
        }

        // 创建新关联
        ActivityGoods activityGoods = new ActivityGoods(activityId, spuId, source, channel, discountId);
        activityRepository.saveActivityGoods(activityGoods);

        log.info("【AdminActivity】活动商品关联更新成功");
    }

    // ==================== 选择器接口 ====================

    /**
     * 获取所有折扣列表（用于下拉选择）
     */
    public List<Discount> listAllDiscounts() {
        log.info("【AdminActivity】查询所有折扣列表");
        return discountRepository.findAll();
    }

    /**
     * 分页查询折扣列表
     */
    public PageResult<Discount> listDiscounts(int page, int size) {
        log.info("【AdminActivity】分页查询折扣列表, page: {}, size: {}", page, size);
        return discountRepository.findAll(page, size);
    }

    /**
     * 获取所有 SPU 列表（用于下拉选择）
     * 注：管理后台应显示所有 SPU，包括下架的，以便为即将上架的商品创建活动
     */
    public List<Spu> listAllOnSaleSpu() {
        log.info("【AdminActivity】查询所有SPU列表（包含下架）");
        PageResult<Spu> pageResult = spuRepository.findAll(1, 1000); // 获取前1000个
        return pageResult.getList();
    }

    /**
     * 分页查询 SPU 列表
     */
    public PageResult<Spu> listSpuPage(int page, int size) {
        log.info("【AdminActivity】分页查询SPU列表, page: {}, size: {}", page, size);
        return spuRepository.findAll(page, size);
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
    public static class UpdateActivityCmd {
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
