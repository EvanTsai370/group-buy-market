package org.example.infrastructure.persistence.repository;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.example.common.cache.RedisKeyManager;
import org.example.domain.model.trade.TradeOrder;
import org.example.domain.model.trade.repository.TradeOrderRepository;
import org.example.infrastructure.cache.IRedisService;
import org.example.infrastructure.persistence.converter.TradeOrderConverter;
import org.example.infrastructure.persistence.mapper.TradeOrderMapper;
import org.example.infrastructure.persistence.po.TradeOrderPO;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * TradeOrder仓储实现
 *
 * <p>
 * 职责：
 * <ul>
 * <li>实现Domain层定义的TradeOrderRepository接口</li>
 * <li>处理Domain对象和PO对象的转换</li>
 * <li>封装MyBatis-Plus的数据访问逻辑</li>
 * </ul>
 *
 * @author 开发团队
 * @since 2026-01-04
 */
@Slf4j
@Repository
public class TradeOrderRepositoryImpl implements TradeOrderRepository {

    private final TradeOrderMapper tradeOrderMapper;
    private final TradeOrderConverter tradeOrderConverter;
    private final IRedisService redisService;

    public TradeOrderRepositoryImpl(TradeOrderMapper tradeOrderMapper,
            TradeOrderConverter tradeOrderConverter,
            IRedisService redisService) {
        this.tradeOrderMapper = tradeOrderMapper;
        this.tradeOrderConverter = tradeOrderConverter;
        this.redisService = redisService;
    }

    @Override
    public void save(TradeOrder tradeOrder) {
        TradeOrderPO po = tradeOrderConverter.toPO(tradeOrder);
        tradeOrderMapper.insert(po);
        log.debug("【TradeOrderRepository】保存交易订单, tradeOrderId: {}", tradeOrder.getTradeOrderId());
    }

    @Override
    public void update(TradeOrder tradeOrder) {
        TradeOrderPO po = tradeOrderConverter.toPO(tradeOrder);
        tradeOrderMapper.updateById(po);
        log.debug("【TradeOrderRepository】更新交易订单, tradeOrderId: {}", tradeOrder.getTradeOrderId());
    }

    @Override
    public Optional<TradeOrder> findByTradeOrderId(String tradeOrderId) {
        TradeOrderPO po = tradeOrderMapper.selectById(tradeOrderId);
        if (po == null) {
            return Optional.empty();
        }
        TradeOrder domain = tradeOrderConverter.toDomain(po);
        return Optional.of(domain);
    }

    @Override
    public Optional<TradeOrder> findByOutTradeNo(String outTradeNo) {
        TradeOrderPO po = tradeOrderMapper.selectByOutTradeNo(outTradeNo);
        if (po == null) {
            return Optional.empty();
        }
        TradeOrder domain = tradeOrderConverter.toDomain(po);
        return Optional.of(domain);
    }

    @Override
    public List<TradeOrder> findByUserIdAndActivityId(String userId, String activityId) {
        List<TradeOrderPO> poList = tradeOrderMapper.selectByUserIdAndActivityId(userId, activityId);
        return poList.stream()
                .map(tradeOrderConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<TradeOrder> findByTeamId(String teamId) {
        List<TradeOrderPO> poList = tradeOrderMapper.selectByTeamId(teamId);
        return poList.stream()
                .map(tradeOrderConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<TradeOrder> findByOrderId(String orderId) {
        List<TradeOrderPO> poList = tradeOrderMapper.selectByOrderId(orderId);
        return poList.stream()
                .map(tradeOrderConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public int countByUserIdAndActivityId(String userId, String activityId) {
        return tradeOrderMapper.countByUserIdAndActivityId(userId, activityId);
    }

    @Override
    public List<TradeOrder> findByUserId(String userId, int page, int size) {
        int offset = (page - 1) * size;
        List<TradeOrderPO> poList = tradeOrderMapper.selectByUserId(userId, offset, size);
        return poList.stream()
                .map(tradeOrderConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean occupyTeamStock(String teamStockKey, Integer target, Integer validTime) {
        log.debug("【TradeOrderRepository】占用队伍库存, teamStockKey: {}, target: {}", teamStockKey, target);

        // 从teamStockKey中提取orderId，然后使用RedisKeyManager生成完整的key
        String orderId = org.example.common.cache.RedisKeyManager.extractOrderIdFromTeamStockKey(teamStockKey);
        String availableKey = org.example.common.cache.RedisKeyManager.teamStockAvailableKey(orderId);
        String lockedKey = org.example.common.cache.RedisKeyManager.teamStockLockedKey(orderId);

        // 1. 初始化库存（仅首次，使用exists检查避免重复初始化）
        if (!redisService.exists(availableKey)) {
            redisService.setLong(availableKey, target.longValue(), validTime + 60L, TimeUnit.MINUTES);
            log.info("【TradeOrderRepository】初始化队伍库存, availableKey: {}, target: {}", availableKey, target);
        }

        // 2. 尝试扣减库存（DECR 返回扣减后的值）
        long remainingStock = redisService.decr(availableKey);

        log.debug("【TradeOrderRepository】扣减库存, availableKey: {}, remaining: {}", availableKey, remainingStock);

        // 3. 如果库存不足（扣减后小于0），回滚
        if (remainingStock < 0) {
            redisService.incr(availableKey); // 回滚
            log.warn("【TradeOrderRepository】队伍库存不足, availableKey: {}, remaining: {}", availableKey, remainingStock);
            return false;
        }

        // 4. 记录已锁定量（用于监控和审计，可选）
        long locked = redisService.incr(lockedKey);
        redisService.expire(lockedKey, validTime + 60L, TimeUnit.MINUTES);

        log.info("【TradeOrderRepository】队伍库存占用成功, availableKey: {}, remaining: {}, locked: {}",
                availableKey, remainingStock, locked);
        return true;
    }

    @Override
    public void recoveryTeamStock(String teamStockKey, Integer validTime) {
        if (StringUtils.isBlank(teamStockKey)) {
            log.warn("【TradeOrderRepository】恢复库存失败，teamStockKey为空");
            return;
        }

        // 从teamStockKey中提取orderId，然后使用RedisKeyManager生成完整的key
        String orderId = RedisKeyManager.extractOrderIdFromTeamStockKey(teamStockKey);
        String availableKey = RedisKeyManager.teamStockAvailableKey(orderId);

        // 直接增加可用库存（原子操作）
        long available = redisService.incr(availableKey);
        // 保持与初始化时一致的过期时间（validTime + 60分钟缓冲）
        redisService.expire(availableKey, validTime + 60L, TimeUnit.MINUTES);

        log.info("【TradeOrderRepository】恢复队伍库存, availableKey: {}, current available: {}",
                availableKey, available);
    }
}
