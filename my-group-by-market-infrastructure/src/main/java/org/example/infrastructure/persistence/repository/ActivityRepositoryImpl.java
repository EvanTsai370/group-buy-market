package org.example.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.model.activity.Activity;
import org.example.domain.model.activity.Discount;
import org.example.domain.model.activity.repository.ActivityRepository;
import org.example.domain.shared.IdGenerator;
import org.example.domain.model.activity.ActivityGoods;
import org.example.infrastructure.persistence.converter.ActivityConverter;
import org.example.infrastructure.persistence.converter.DiscountConverter;
import org.example.infrastructure.persistence.mapper.ActivityGoodsMapper;
import org.example.infrastructure.persistence.mapper.ActivityMapper;
import org.example.infrastructure.persistence.mapper.DiscountMapper;
import org.example.infrastructure.persistence.po.ActivityGoodsPO;
import org.example.infrastructure.persistence.po.ActivityPO;
import org.example.infrastructure.persistence.po.DiscountPO;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Activity 仓储实现
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ActivityRepositoryImpl implements ActivityRepository {

    private final ActivityMapper activityMapper;
    private final ActivityGoodsMapper activityGoodsMapper;
    private final DiscountMapper discountMapper;
    private final IdGenerator idGenerator;
    private final Environment environment;

    @Override
    public void save(Activity activity) {
        ActivityPO po = ActivityConverter.INSTANCE.toPO(activity);

        if (po.getId() == null) {
            // 新增
            activityMapper.insert(po);
            log.info("【ActivityRepository】新增活动, activityId: {}", activity.getActivityId());
        } else {
            // 更新
            activityMapper.updateById(po);
            log.info("【ActivityRepository】更新活动, activityId: {}", activity.getActivityId());
        }
    }

    @Override
    public void update(Activity activity) {
        LambdaQueryWrapper<ActivityPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ActivityPO::getActivityId, activity.getActivityId());

        ActivityPO existingPo = activityMapper.selectOne(wrapper);
        if (existingPo == null) {
            throw new RuntimeException("活动不存在: " + activity.getActivityId());
        }

        ActivityPO po = ActivityConverter.INSTANCE.toPO(activity);
        po.setId(existingPo.getId());
        activityMapper.updateById(po);
        log.info("【ActivityRepository】更新活动, activityId: {}", activity.getActivityId());
    }

    @Override
    public Optional<Activity> findById(String activityId) {
        LambdaQueryWrapper<ActivityPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ActivityPO::getActivityId, activityId);

        ActivityPO po = activityMapper.selectOne(wrapper);
        if (po == null) {
            return Optional.empty();
        }

        Activity activity = ActivityConverter.INSTANCE.toDomain(po);
        return Optional.of(activity);
    }

    @Override
    public List<Activity> findAll(int page, int size) {
        Page<ActivityPO> pageParam = new Page<>(page, size);
        Page<ActivityPO> resultPage = activityMapper.selectPage(pageParam, null);

        return resultPage.getRecords().stream()
                .map(ActivityConverter.INSTANCE::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public String queryActivityIdByGoodsSourceChannel(String goodsId, String source, String channel) {
        String activityId = activityGoodsMapper.selectActivityIdByGoodsSourceChannel(goodsId, source, channel);
        log.info("【ActivityRepository】查询活动ID，goodsId: {}, source: {}, channel: {}, result: {}",
                goodsId, source, channel, activityId);
        return activityId;
    }

    @Override
    public ActivityGoods queryActivityGoods(String activityId, String goodsId, String source, String channel) {
        ActivityGoodsPO po = activityGoodsMapper.selectByActivityGoods(activityId, goodsId, source, channel);
        if (po == null) {
            log.warn("【ActivityRepository】活动商品关联不存在，activityId: {}, goodsId: {}, source: {}, channel: {}",
                    activityId, goodsId, source, channel);
            return null;
        }

        ActivityGoods activityGoods = new ActivityGoods(
                po.getActivityId(),
                po.getGoodsId(),
                po.getSource(),
                po.getChannel(),
                po.getDiscountId());
        log.info("【ActivityRepository】查询活动商品关联，activityId: {}, goodsId: {}, discountId: {}",
                activityId, goodsId, activityGoods.getDiscountId());
        return activityGoods;
    }

    @Override
    public Discount queryDiscountById(String discountId) {
        LambdaQueryWrapper<DiscountPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DiscountPO::getDiscountId, discountId);

        DiscountPO po = discountMapper.selectOne(wrapper);
        if (po == null) {
            log.warn("【ActivityRepository】折扣配置不存在，discountId: {}", discountId);
            return null;
        }

        Discount discount = DiscountConverter.INSTANCE.toDomain(po);
        log.info("【ActivityRepository】查询折扣配置，discountId: {}, discountName: {}",
                discountId, discount.getDiscountName());
        return discount;
    }

    @Override
    public void saveDiscount(Discount discount) {
        DiscountPO po = DiscountConverter.INSTANCE.toPO(discount);

        LambdaQueryWrapper<DiscountPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DiscountPO::getDiscountId, discount.getDiscountId());
        DiscountPO existingPo = discountMapper.selectOne(wrapper);

        if (existingPo == null) {
            discountMapper.insert(po);
            log.info("【ActivityRepository】新增折扣配置, discountId: {}", discount.getDiscountId());
        } else {
            po.setId(existingPo.getId());
            discountMapper.updateById(po);
            log.info("【ActivityRepository】更新折扣配置, discountId: {}", discount.getDiscountId());
        }
    }

    @Override
    public void saveActivityGoods(ActivityGoods activityGoods) {
        ActivityGoodsPO po = new ActivityGoodsPO();
        po.setActivityId(activityGoods.getActivityId());
        po.setGoodsId(activityGoods.getGoodsId());
        po.setSource(activityGoods.getSource());
        po.setChannel(activityGoods.getChannel());
        po.setDiscountId(activityGoods.getDiscountId());

        activityGoodsMapper.insert(po);
        log.info("【ActivityRepository】新增活动商品关联, activityId: {}, goodsId: {}",
                activityGoods.getActivityId(), activityGoods.getGoodsId());
    }

    @Override
    public boolean isDowngraded() {
        // 使用动态配置读取降级开关
        boolean isDowngraded = environment.getProperty("activity.downgrade.switch", Boolean.class, false);
        log.debug("【ActivityRepository】降级开关检查，isDowngraded: {}", isDowngraded);
        return isDowngraded;
    }

    @Override
    public boolean isInCutRange(String userId) {
        // 从动态配置读取切量配置（100表示100%全量，10表示10%切量）
        int cutPercentage = environment.getProperty("activity.cut.range", Integer.class, 100);

        int hash = Math.abs(userId.hashCode() % 100);
        boolean isInRange = hash < cutPercentage;
        log.debug("【ActivityRepository】切量检查，userId: {}, hash: {}, cutPercentage: {}, isInRange: {}",
                userId, hash, cutPercentage, isInRange);
        return isInRange;
    }

    @Override
    public String nextId() {
        return "ACT" + idGenerator.nextId();
    }

    @Override
    public String nextDiscountId() {
        return "DSC" + idGenerator.nextId();
    }

    @Override
    public Optional<Activity> findActiveByGoodsId(String goodsId) {
        // 1. 通过商品ID查询关联的活动ID列表
        List<String> activityIds = activityGoodsMapper.selectActiveActivityIdsByGoodsId(goodsId);
        if (activityIds == null || activityIds.isEmpty()) {
            log.debug("【ActivityRepository】商品无关联活动，goodsId: {}", goodsId);
            return Optional.empty();
        }

        // 2. 遍历活动ID，找到第一个有效的活动（状态为ACTIVE且在有效期内）
        for (String activityId : activityIds) {
            LambdaQueryWrapper<ActivityPO> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ActivityPO::getActivityId, activityId)
                    .eq(ActivityPO::getStatus, "ACTIVE")
                    .le(ActivityPO::getStartTime, java.time.LocalDateTime.now())
                    .ge(ActivityPO::getEndTime, java.time.LocalDateTime.now());

            ActivityPO po = activityMapper.selectOne(wrapper);
            if (po != null) {
                Activity activity = ActivityConverter.INSTANCE.toDomain(po);
                log.info("【ActivityRepository】查询商品关联活动，goodsId: {}, activityId: {}",
                        goodsId, activityId);
                return Optional.of(activity);
            }
        }

        log.debug("【ActivityRepository】商品无有效活动，goodsId: {}", goodsId);
        return Optional.empty();
    }
}