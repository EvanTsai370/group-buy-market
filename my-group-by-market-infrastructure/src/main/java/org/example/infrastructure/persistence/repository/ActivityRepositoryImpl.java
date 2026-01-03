package org.example.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

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
    private final StringRedisTemplate stringRedisTemplate;

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
                po.getDiscountId()
        );
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
    public boolean isDowngraded() {
        // 从 Redis 读取降级开关
        String key = "group_buy:downgrade:switch";
        String value = stringRedisTemplate.opsForValue().get(key);
        boolean isDowngraded = "true".equalsIgnoreCase(value);
        log.debug("【ActivityRepository】降级开关检查，key: {}, isDowngraded: {}", key, isDowngraded);
        return isDowngraded;
    }

    @Override
    public boolean isInCutRange(String userId) {
        // 从 Redis 读取切量配置（示例：使用用户ID的hash值进行切量）
        String key = "group_buy:cut:range";
        String range = stringRedisTemplate.opsForValue().get(key);

        if (range == null) {
            // 未配置切量，默认全部放行
            return true;
        }

        try {
            int cutPercentage = Integer.parseInt(range); // 如 "10" 表示10%
            int hash = Math.abs(userId.hashCode() % 100);
            boolean isInRange = hash < cutPercentage;
            log.debug("【ActivityRepository】切量检查，userId: {}, hash: {}, cutPercentage: {}, isInRange: {}",
                     userId, hash, cutPercentage, isInRange);
            return isInRange;
        } catch (NumberFormatException e) {
            log.error("【ActivityRepository】切量配置格式错误，range: {}", range, e);
            return true; // 配置错误时默认放行
        }
    }

    @Override
    public String nextId() {
        return "ACT" + idGenerator.nextId();
    }
}