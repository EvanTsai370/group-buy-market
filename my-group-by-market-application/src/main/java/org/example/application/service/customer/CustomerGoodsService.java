package org.example.application.service.customer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.application.service.customer.query.PriceTrialQuery;
import org.example.application.service.customer.result.*;
import org.example.common.exception.BizException;
import org.example.domain.model.activity.Activity;
import org.example.domain.model.activity.Discount;
import org.example.domain.model.activity.repository.ActivityRepository;
import org.example.domain.model.goods.Sku;
import org.example.domain.model.goods.Spu;
import org.example.domain.model.goods.repository.SkuRepository;
import org.example.domain.model.goods.repository.SpuRepository;
import org.example.domain.model.order.Order;
import org.example.domain.model.order.repository.OrderRepository;
import org.example.domain.service.discount.DiscountCalculator;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * C端商品服务
 * 
 * 提供给前端用户的商品相关接口，包括：
 * - 商品列表
 * - 商品详情（含活动信息）
 * - 价格试算
 * - 拼团队伍列表
 * 
 * @author 开发团队
 * @since 2026-01-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerGoodsService {

    private final SkuRepository skuRepository;
    private final SpuRepository spuRepository;
    private final ActivityRepository activityRepository;
    private final OrderRepository orderRepository;
    private final Map<String, DiscountCalculator> discountCalculatorMap;

    /**
     * 查询在售商品列表（首页）
     *
     * @return 商品列表
     */
    public List<GoodsListResult> listOnSaleGoods() {
        log.info("【CustomerGoodsService】查询在售商品列表");

        List<Sku> skuList = skuRepository.findAllOnSale();
        List<GoodsListResult> results = new ArrayList<>();

        for (Sku sku : skuList) {
            GoodsListResult result = new GoodsListResult();
            result.setGoodsId(sku.getGoodsId());
            result.setGoodsName(sku.getGoodsName());
            result.setOriginalPrice(sku.getOriginalPrice());
            result.setMainImage(sku.getSkuImage());
            result.setAvailableStock(sku.getAvailableStock());

            // 查询关联的活动
            Optional<Activity> activityOpt = activityRepository.findActiveByGoodsId(sku.getGoodsId());
            if (activityOpt.isPresent()) {
                Activity activity = activityOpt.get();
                result.setHasActivity(true);
                result.setActivityId(activity.getActivityId());

                // 计算拼团价
                Discount discount = activityRepository.queryDiscountById(activity.getDiscountId());
                if (discount != null) {
                    BigDecimal groupPrice = calculateDiscountPrice(discount, sku.getOriginalPrice());
                    result.setGroupPrice(groupPrice);
                }
            } else {
                result.setHasActivity(false);
            }

            results.add(result);
        }

        log.info("【CustomerGoodsService】查询在售商品列表完成，共{}条", results.size());
        return results;
    }

    /**
     * 查询商品详情
     *
     * @param goodsId 商品ID
     * @return 商品详情
     */
    public GoodsDetailResult getGoodsDetail(String goodsId) {
        log.info("【CustomerGoodsService】查询商品详情，goodsId: {}", goodsId);

        // 1. 查询 SKU
        Sku sku = skuRepository.findByGoodsId(goodsId)
                .orElseThrow(() -> new BizException("商品不存在"));

        GoodsDetailResult result = new GoodsDetailResult();
        result.setGoodsId(sku.getGoodsId());
        result.setGoodsName(sku.getGoodsName());
        result.setSpecInfo(sku.getSpecInfo());
        result.setOriginalPrice(sku.getOriginalPrice());
        result.setSkuImage(sku.getSkuImage());
        result.setAvailableStock(sku.getAvailableStock());

        // 2. 查询 SPU（如有关联）
        if (sku.getSpuId() != null) {
            Optional<Spu> spuOpt = spuRepository.findBySpuId(sku.getSpuId());
            if (spuOpt.isPresent()) {
                Spu spu = spuOpt.get();
                result.setSpuId(spu.getSpuId());
                result.setSpuName(spu.getSpuName());
                result.setDescription(spu.getDescription());
                result.setMainImage(spu.getMainImage());
                result.setDetailImages(spu.getDetailImages());
            }
        }

        // 3. 查询关联活动
        Optional<Activity> activityOpt = activityRepository.findActiveByGoodsId(goodsId);
        if (activityOpt.isPresent()) {
            Activity activity = activityOpt.get();
            result.setHasActivity(true);
            result.setActivityId(activity.getActivityId());
            result.setActivityName(activity.getActivityName());
            result.setTargetCount(activity.getTarget());
            result.setActivityEndTime(activity.getEndTime());
            result.setValidTime(activity.getValidTime());

            // 计算拼团价
            Discount discount = activityRepository.queryDiscountById(activity.getDiscountId());
            if (discount != null) {
                BigDecimal groupPrice = calculateDiscountPrice(discount, sku.getOriginalPrice());
                result.setGroupPrice(groupPrice);
            }
        } else {
            result.setHasActivity(false);
        }

        log.info("【CustomerGoodsService】查询商品详情完成，goodsId: {}, hasActivity: {}",
                goodsId, result.getHasActivity());
        return result;
    }

    /**
     * 价格试算
     *
     * @param query 试算查询条件
     * @return 试算结果
     */
    public PriceTrialResult trialPrice(PriceTrialQuery query) {
        log.info("【CustomerGoodsService】价格试算，goodsId: {}, source: {}, channel: {}",
                query.getGoodsId(), query.getSource(), query.getChannel());

        // 1. 查询 SKU
        Sku sku = skuRepository.findByGoodsId(query.getGoodsId())
                .orElseThrow(() -> new BizException("商品不存在"));

        PriceTrialResult result = new PriceTrialResult();
        result.setGoodsId(query.getGoodsId());
        result.setOriginalPrice(sku.getOriginalPrice());

        // 2. 根据来源渠道查询活动
        String activityId = null;
        if (query.getSource() != null && query.getChannel() != null) {
            activityId = activityRepository.queryActivityIdByGoodsSourceChannel(
                    query.getGoodsId(), query.getSource(), query.getChannel());
        }

        // 3. 如果没有指定渠道，尝试查找任意有效活动
        if (activityId == null) {
            Optional<Activity> activityOpt = activityRepository.findActiveByGoodsId(query.getGoodsId());
            if (activityOpt.isPresent()) {
                activityId = activityOpt.get().getActivityId();
            }
        }

        // 4. 如果找到活动，计算折扣价
        if (activityId != null) {
            Optional<Activity> activityOpt = activityRepository.findById(activityId);
            if (activityOpt.isPresent() && activityOpt.get().isValid()) {
                Activity activity = activityOpt.get();
                result.setHitActivity(true);
                result.setActivityId(activityId);
                result.setActivityName(activity.getActivityName());

                Discount discount = activityRepository.queryDiscountById(activity.getDiscountId());
                if (discount != null) {
                    BigDecimal discountPrice = calculateDiscountPrice(discount, sku.getOriginalPrice());
                    result.setDiscountPrice(discountPrice);
                    result.setDiscountDesc(discount.getDiscountDesc());
                }
            } else {
                result.setHitActivity(false);
                result.setDiscountPrice(sku.getOriginalPrice());
            }
        } else {
            result.setHitActivity(false);
            result.setDiscountPrice(sku.getOriginalPrice());
        }

        log.info("【CustomerGoodsService】价格试算完成，goodsId: {}, 原价: {}, 折扣价: {}",
                query.getGoodsId(), result.getOriginalPrice(), result.getDiscountPrice());
        return result;
    }

    /**
     * 查询商品的拼团队伍列表
     *
     * @param goodsId 商品ID
     * @return 队伍列表
     */
    public List<TeamListResult> listGoodsTeams(String goodsId) {
        log.info("【CustomerGoodsService】查询商品拼团队伍，goodsId: {}", goodsId);

        // 1. 确认商品存在
        skuRepository.findByGoodsId(goodsId)
                .orElseThrow(() -> new BizException("商品不存在"));

        // 2. 查询关联活动
        Optional<Activity> activityOpt = activityRepository.findActiveByGoodsId(goodsId);
        if (activityOpt.isEmpty()) {
            log.info("【CustomerGoodsService】商品无活动，返回空列表，goodsId: {}", goodsId);
            return new ArrayList<>();
        }

        Activity activity = activityOpt.get();

        // 3. 查询进行中的拼团订单
        List<Order> orders = orderRepository.findPendingOrdersByActivity(activity.getActivityId());

        // 4. 转换为结果
        List<TeamListResult> results = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (Order order : orders) {
            // 只返回该商品的拼团
            if (!goodsId.equals(order.getGoodsId())) {
                continue;
            }

            TeamListResult teamResult = new TeamListResult();
            teamResult.setOrderId(order.getOrderId());
            teamResult.setCurrentCount(order.getCompleteCount());
            teamResult.setTargetCount(order.getTargetCount());
            teamResult.setLeaderUserId(order.getLeaderUserId());

            // 计算剩余时间
            if (order.getDeadlineTime() != null && order.getDeadlineTime().isAfter(now)) {
                Duration duration = Duration.between(now, order.getDeadlineTime());
                teamResult.setRemainingSeconds(duration.getSeconds());
            } else {
                teamResult.setRemainingSeconds(0L);
            }

            // TODO: 团长昵称和头像需要从 User 仓储查询，这里暂时不填充
            // 可以在后续版本中添加 UserRepository 依赖来获取

            results.add(teamResult);
        }

        log.info("【CustomerGoodsService】查询商品拼团队伍完成，goodsId: {}, 共{}个拼团",
                goodsId, results.size());
        return results;
    }

    /**
     * 计算折扣价格
     * 使用策略模式，根据 Discount 的 marketPlan 选择对应的计算器
     *
     * @param discount      Discount 配置
     * @param originalPrice 原价
     * @return 折扣后价格
     */
    private BigDecimal calculateDiscountPrice(Discount discount, BigDecimal originalPrice) {
        if (discount == null || discount.getMarketPlan() == null) {
            return originalPrice;
        }

        DiscountCalculator calculator = discountCalculatorMap.get(discount.getMarketPlan());
        if (calculator == null) {
            log.warn("【CustomerGoodsService】不支持的营销计划类型: {}", discount.getMarketPlan());
            return originalPrice;
        }

        // DiscountCalculator.calculate 需要 userId，这里传空值即可
        return calculator.calculate(null, originalPrice, discount);
    }
}
