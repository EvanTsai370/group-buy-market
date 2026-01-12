package org.example.domain.model.trade.filter;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.example.common.exception.BizException;
import org.example.common.pattern.chain.model2.IChainHandler;
import org.example.domain.model.goods.repository.SkuRepository;

/**
 * 商品库存预占规则处理器
 *
 * <p>
 * 职责：
 * <ul>
 * <li>在锁单前预占商品库存（增加 frozen_stock）</li>
 * <li>防止高并发场景下库存超卖</li>
 * <li>支持失败回滚（减少 frozen_stock）</li>
 * </ul>
 *
 * <p>
 * 业务逻辑：
 * <ol>
 * <li>获取请求中的 skuId</li>
 * <li>调用 SkuRepository.freezeStock() 原子冻结库存</li>
 * <li>冻结成功：将 skuId 放入上下文，用于失败回滚</li>
 * <li>冻结失败：返回 reject，阻止锁单继续</li>
 * </ol>
 *
 * <p>
 * 技术要点：
 * <ul>
 * <li>使用数据库原子操作（UPDATE ... WHERE available >= quantity）保证并发安全</li>
 * <li>与 TeamSlotOccupyHandler 配合，先占组队名额再冻结库存</li>
 * <li>失败回滚由 TradeOrderService.rollbackInventory() 执行</li>
 * </ul>
 *
 * <p>
 * 执行顺序（在 TradeFilterFactory 中）：
 * 
 * <pre>
 * 1. ActivityAvailabilityHandler - 活动可用性校验
 * 2. UserParticipationLimitHandler - 用户参与限制校验
 * 3. TeamSlotOccupyHandler - 组队名额占用校验
 * 4. InventoryOccupyHandler - 商品库存预占（本处理器）
 * </pre>
 *
 * @author 开发团队
 * @since 2026-01-11
 */
@Slf4j
public class InventoryOccupyHandler
        implements IChainHandler<TradeFilterRequest, TradeFilterContext, TradeFilterResponse> {

    /** 每次锁单预占的库存数量 */
    private static final int FREEZE_QUANTITY = 1;

    private final SkuRepository skuRepository;

    public InventoryOccupyHandler(SkuRepository skuRepository) {
        this.skuRepository = skuRepository;
    }

    @Override
    public TradeFilterResponse handle(TradeFilterRequest request, TradeFilterContext context) throws Exception {
        log.info("【交易规则过滤-库存预占】userId: {}, activityId: {}, skuId: {}",
                request.getUserId(), request.getActivityId(), request.getSkuId());

        // 1. 获取 skuId
        String skuId = request.getSkuId();
        if (StringUtils.isBlank(skuId)) {
            throw new BizException("商品ID不能为空");
        }

        // 2. 原子冻结库存
        int result = skuRepository.freezeStock(skuId, FREEZE_QUANTITY);

        if (result <= 0) {
            log.warn("【交易规则过滤-库存预占】库存不足, userId: {}, skuId: {}",
                    request.getUserId(), skuId);
            return TradeFilterResponse.reject("商品库存不足，请稍后再试");
        }

        // 3. 冻结成功，将 skuId 放入上下文，用于失败回滚
        context.setRecoverySkuId(skuId);

        log.info("【交易规则过滤-库存预占】冻结成功, userId: {}, skuId: {}, 冻结数量: {}",
                request.getUserId(), skuId, FREEZE_QUANTITY);

        return TradeFilterResponse.allow();
    }
}
