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
            result.setSkuId(sku.getSkuId());
            result.setGoodsName(sku.getGoodsName());
            result.setOriginalPrice(sku.getOriginalPrice());
            result.setMainImage(sku.getSkuImage());
            result.setAvailableStock(sku.getAvailableStock());

            // 查询关联的活动
            // SPU 重构：使用 spuId 查询活动
            Optional<Activity> activityOpt = activityRepository.findActiveBySpuId(sku.getSpuId());
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
     * 查询在售 SPU 列表（新版首页）
     *
     * @return SPU 列表
     */
    public List<SpuListResult> listSpuOnSale() {
        log.info("【CustomerGoodsService】查询在售 SPU 列表");

        List<Spu> spuList = spuRepository.findAllOnSale();
        List<SpuListResult> results = new ArrayList<>();

        for (Spu spu : spuList) {
            SpuListResult result = new SpuListResult();
            result.setSpuId(spu.getSpuId());
            result.setSpuName(spu.getSpuName());
            result.setMainImage(spu.getMainImage());

            // 查询关联的 SKU 以获取价格区间
            List<Sku> skus = skuRepository.findBySpuId(spu.getSpuId());
            if (skus.isEmpty()) {
                continue; // 忽略无 SKU 的 SPU
            }

            // 计算最低原价
            BigDecimal minOriginalPrice = skus.stream()
                    .map(Sku::getOriginalPrice)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
            result.setMinOriginalPrice(minOriginalPrice);

            // 查询关联活动
            Optional<Activity> activityOpt = activityRepository.findActiveBySpuId(spu.getSpuId());
            if (activityOpt.isPresent()) {
                Activity activity = activityOpt.get();
                result.setHasActivity(true);
                result.setActivityId(activity.getActivityId());

                // 计算最低拼团价
                Discount discount = activityRepository.queryDiscountById(activity.getDiscountId());
                if (discount != null) {
                    BigDecimal minGroupPrice = calculateDiscountPrice(discount, minOriginalPrice);
                    result.setMinGroupPrice(minGroupPrice);
                } else {
                    result.setMinGroupPrice(minOriginalPrice);
                }
            } else {
                result.setHasActivity(false);
                result.setMinGroupPrice(minOriginalPrice);
            }

            results.add(result);
        }

        log.info("【CustomerGoodsService】查询在售 SPU 列表完成，共{}条", results.size());
        return results;
    }

    /**
     * 查询商品详情
     *
     * @param skuId 商品ID
     * @return 商品详情
     */
    /**
     * 查询商品详情
     *
     * @param skuId 商品ID
     * @return 商品详情
     */
    public GoodsDetailResult getGoodsDetail(String skuId) {
        log.info("【CustomerGoodsService】查询商品详情，skuId: {}", skuId);

        // 1. 查询 SKU
        Sku sku = skuRepository.findBySkuId(skuId)
                .orElseThrow(() -> new BizException("商品不存在"));

        GoodsDetailResult result = new GoodsDetailResult();
        result.setSkuId(sku.getSkuId());
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
        // SPU 重构：使用 spuId 查询活动
        Optional<Activity> activityOpt = activityRepository.findActiveBySpuId(sku.getSpuId());
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

        log.info("【CustomerGoodsService】查询商品详情完成，skuId: {}, hasActivity: {}",
                skuId, result.getHasActivity());
        return result;
    }

    /**
     * 查询 SPU 详情（新版商品详情页）
     *
     * @param spuId SPU ID
     * @return SPU 详情
     */
    public SpuDetailResult getSpuDetail(String spuId) {
        log.info("【CustomerGoodsService】查询 SPU 详情，spuId: {}", spuId);

        // 1. 查询 SPU
        Spu spu = spuRepository.findBySpuId(spuId)
                .orElseThrow(() -> new BizException("商品不存在"));

        SpuDetailResult result = new SpuDetailResult();
        result.setSpuId(spu.getSpuId());
        result.setSpuName(spu.getSpuName());
        result.setDescription(spu.getDescription());
        result.setMainImage(spu.getMainImage());
        result.setDetailImages(spu.getDetailImages());

        // 2. 查询关联 SKU 列表
        List<Sku> skus = skuRepository.findBySpuId(spuId);
        List<GoodsDetailResult> skuResults = new ArrayList<>();

        Optional<Activity> activityOpt = activityRepository.findActiveBySpuId(spuId);
        Activity activity = activityOpt.orElse(null);
        Discount discount = null;
        if (activity != null) {
            discount = activityRepository.queryDiscountById(activity.getDiscountId());
        }

        for (Sku sku : skus) {
            GoodsDetailResult skuResult = new GoodsDetailResult();
            skuResult.setSkuId(sku.getSkuId());
            skuResult.setGoodsName(sku.getGoodsName());
            skuResult.setSpecInfo(sku.getSpecInfo());
            skuResult.setOriginalPrice(sku.getOriginalPrice());
            skuResult.setSkuImage(sku.getSkuImage());
            skuResult.setAvailableStock(sku.getAvailableStock());

            // 计算拼团价
            if (activity != null && discount != null) {
                BigDecimal groupPrice = calculateDiscountPrice(discount, sku.getOriginalPrice());
                skuResult.setGroupPrice(groupPrice);
                skuResult.setHasActivity(true);
            } else {
                skuResult.setHasActivity(false);
            }
            skuResults.add(skuResult);
        }
        result.setSkuList(skuResults);

        // 3. 填充活动信息
        if (activity != null) {
            result.setHasActivity(true);
            result.setActivityId(activity.getActivityId());
            result.setActivityName(activity.getActivityName());
            result.setTargetCount(activity.getTarget());
            result.setActivityEndTime(activity.getEndTime());
            result.setValidTime(activity.getValidTime());
        } else {
            result.setHasActivity(false);
        }

        log.info("【CustomerGoodsService】查询 SPU 详情完成，spuId: {}, skuCount: {}, hasActivity: {}",
                spuId, skuResults.size(), result.getHasActivity());
        return result;
    }

    /**
     * 价格试算
     *
     * @param query 试算查询条件
     * @return 试算结果
     */
    public PriceTrialResult trialPrice(PriceTrialQuery query) {
        log.info("【CustomerGoodsService】价格试算，skuId: {}, source: {}, channel: {}",
                query.getSkuId(), query.getSource(), query.getChannel());

        // 1. 查询 SKU
        Sku sku = skuRepository.findBySkuId(query.getSkuId())
                .orElseThrow(() -> new BizException("商品不存在"));

        PriceTrialResult result = new PriceTrialResult();
        result.setSkuId(query.getSkuId());
        result.setOriginalPrice(sku.getOriginalPrice());

        // 2. 根据来源渠道查询活动
        String activityId = null;
        if (query.getSource() != null && query.getChannel() != null) {
            activityId = activityRepository.queryActivityIdByGoodsSourceChannel(
                    query.getSkuId(), query.getSource(), query.getChannel());
        }

        // 3. 如果没有指定渠道，尝试查找任意有效活动
        if (activityId == null) {
            // SPU 重构：使用 spuId 查询活动
            Optional<Activity> activityOpt = activityRepository.findActiveBySpuId(sku.getSpuId());
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

        log.info("【CustomerGoodsService】价格试算完成，skuId: {}, 原价: {}, 折扣价: {}",
                query.getSkuId(), result.getOriginalPrice(), result.getDiscountPrice());
        return result;
    }

    /**
     * 查询SPU的拼团队伍列表（SPU维度）
     *
     * 注意：本系统采用SPU拼团模式，不同规格(SKU)的用户可以在同一队伍中一起拼团。
     * 例如：购买 iPhone 15 Pro 256GB 黑色 和 512GB 白色 的用户可以一起成团。
     *
     * @param spuId 商品SPU ID
     * @return 拼团队伍列表（SPU维度）
     */
    public List<TeamListResult> listGoodsTeams(String spuId) {
        log.info("【CustomerGoodsService】查询拼团队伍，spuId: {}", spuId);

        // 1. 确认 SPU 存在
        Spu spu = spuRepository.findBySpuId(spuId)
                .orElseThrow(() -> new BizException("商品不存在"));

        // 2. 查询 SPU 关联的活动
        Optional<Activity> activityOpt = activityRepository.findActiveBySpuId(spuId);
        if (activityOpt.isEmpty()) {
            log.info("【CustomerGoodsService】SPU 无活动，返回空列表，spuId: {}", spuId);
            return new ArrayList<>();
        }

        Activity activity = activityOpt.get();

        // 3. 查询进行中的拼团订单
        List<Order> orders = orderRepository.findPendingOrdersByActivity(activity.getActivityId());

        // 4. 转换为结果（添加 spuId 和 spuName）
        List<TeamListResult> results = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (Order order : orders) {
            TeamListResult teamResult = new TeamListResult();
            teamResult.setOrderId(order.getOrderId());
            teamResult.setSpuId(spu.getSpuId());       // 新增
            teamResult.setSpuName(spu.getSpuName());   // 新增
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

        log.info("【CustomerGoodsService】查询拼团队伍完成，spuId: {}, 共{}个拼团",
                spuId, results.size());
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
