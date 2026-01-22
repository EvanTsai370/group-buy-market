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

import java.util.Collections;
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
        // MyBatis-Plus 会根据主键是否存在自动判断 INSERT/UPDATE
        boolean success = tradeOrderMapper.insertOrUpdate(po);
        if (!success) {
            log.warn("【TradeOrderRepository】保存交易订单失败, tradeOrderId: {}", tradeOrder.getTradeOrderId());
        } else {
            log.debug("【TradeOrderRepository】保存交易订单成功, tradeOrderId: {}", tradeOrder.getTradeOrderId());
        }
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
    public boolean occupyTeamSlot(String teamSlotKey, Integer target, Integer validTime) {
        log.debug("【TradeOrderRepository】占用队伍名额, teamSlotKey: {}, target: {}", teamSlotKey, target);

        // 从teamSlotKey中提取orderId，然后使用RedisKeyManager生成完整的key
        String orderId = RedisKeyManager.extractOrderIdFromTeamSlotKey(teamSlotKey);
        String availableKey = RedisKeyManager.teamSlotAvailableKey(orderId);
        String lockedKey = RedisKeyManager.teamSlotLockedKey(orderId);

        // 两种方法实现初始化原子性以及扣减原子性：setNx和lua脚本
        // long remainingSlot = initAndDecrSlot(target, validTime, availableKey);
        long remainingSlot = initAndDecrSlotWithScript(target, validTime, availableKey);

        log.debug("【TradeOrderRepository】扣减名额, availableKey: {}, remaining: {}", availableKey, remainingSlot);

        // 如果名额不足（扣减后小于0），回滚
        if (remainingSlot < 0) {
            redisService.incr(availableKey); // 回滚
            log.warn("【TradeOrderRepository】队伍名额不足, availableKey: {}, remaining: {}", availableKey, remainingSlot);
            return false;
        }

        // 记录已锁定量（用于监控和审计，可选）
        long locked = redisService.incr(lockedKey);
        redisService.expire(lockedKey, validTime + 3600L, TimeUnit.SECONDS);

        log.info("【TradeOrderRepository】队伍名额占用成功, availableKey: {}, remaining: {}, locked: {}",
                availableKey, remainingSlot, locked);
        return true;
    }

    private long initAndDecrSlot(Integer target, Integer validTime, String availableKey) {
        // 初始化名额（仅首次）
        // validTime单位是秒，加上1小时(3600秒)的缓冲时间
        Boolean isInit = redisService.setNx(availableKey, target, validTime + 3600L, TimeUnit.SECONDS);

        if (isInit) {
            log.info("【TradeOrderRepository】成功初始化队伍名额（我是第一个）, availableKey: {}", availableKey);
        }

        // 尝试扣减名额（DECR 返回扣减后的值）
        return redisService.decr(availableKey);
    }

    private long initAndDecrSlotWithScript(Integer target, Integer validTime, String availableKey) {
        String luaScript = "if redis.call('exists', KEYS[1]) == 0 then " +
                "    redis.call('setex', KEYS[1], ARGV[2], ARGV[1]) " +
                "end " +
                "return redis.call('decr', KEYS[1])";
        long expireSeconds = TimeUnit.MINUTES.toSeconds(validTime + 60L);
        return redisService.executeScript(
                luaScript,
                Collections.singletonList(availableKey), // 对应 KEYS[1]
                target, // 对应 ARGV[1] (初始库存)
                expireSeconds // 对应 ARGV[2] (过期时间)
        );

        // 也可以把“回滚”也省掉
        // String advancedLuaScript =
        // // 1. 如果 key 不存在，初始化
        // "if redis.call('exists', KEYS[1]) == 0 then " +
        // " redis.call('setex', KEYS[1], ARGV[2], ARGV[1]) " +
        // "end " +
        //
        // // 2. 获取当前值（注意：redis取出的是string，lua需要转number）
        // "local current = tonumber(redis.call('get', KEYS[1])) " +
        //
        // // 3. 如果当前值 <= 0，直接返回 -1 (代表库存不足)
        // "if current <= 0 then " +
        // " return -1 " +
        // "end " +
        //
        // // 4. 执行扣减并返回剩余值
        // "return redis.call('decr', KEYS[1])";
    }

    @Override
    public void recoveryTeamSlot(String teamSlotKey, Integer validTime) {
        if (StringUtils.isBlank(teamSlotKey)) {
            log.warn("【TradeOrderRepository】恢复名额失败，teamSlotKey为空");
            return;
        }

        // 从teamSlotKey中提取orderId，然后使用RedisKeyManager生成完整的key
        String orderId = RedisKeyManager.extractOrderIdFromTeamSlotKey(teamSlotKey);
        String availableKey = RedisKeyManager.teamSlotAvailableKey(orderId);

        // 直接增加可用名额（原子操作）
        long available = redisService.incr(availableKey);
        // 保持与初始化时一致的过期时间（validTime + 3600秒缓冲）
        redisService.expire(availableKey, validTime + 3600L, TimeUnit.SECONDS);

        log.info("【TradeOrderRepository】恢复队伍名额, availableKey: {}, current available: {}",
                availableKey, available);
    }

    @Override
    public java.math.BigDecimal sumPayPriceByPayTimeBetween(java.time.LocalDateTime start,
            java.time.LocalDateTime end) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TradeOrderPO> wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.select(TradeOrderPO::getPayPrice);
        wrapper.ge(TradeOrderPO::getPayTime, start);
        wrapper.le(TradeOrderPO::getPayTime, end);
        wrapper.eq(TradeOrderPO::getStatus, "PAID"); // 仅统计已支付

        List<TradeOrderPO> list = tradeOrderMapper.selectList(wrapper);
        return list.stream()
                .map(TradeOrderPO::getPayPrice)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
    }

    @Override
    public long countByCreateTimeBetween(java.time.LocalDateTime start, java.time.LocalDateTime end) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TradeOrderPO> wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.ge(TradeOrderPO::getCreateTime, start);
        wrapper.le(TradeOrderPO::getCreateTime, end);
        return tradeOrderMapper.selectCount(wrapper);
    }

    @Override
    public List<TradeOrder> findLatest(int limit) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TradeOrderPO> wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.orderByDesc(TradeOrderPO::getCreateTime);
        wrapper.last("LIMIT " + limit);

        List<TradeOrderPO> list = tradeOrderMapper.selectList(wrapper);
        return list.stream()
                .map(tradeOrderConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public org.example.common.model.PageResult<TradeOrder> findByPage(int page, int size, String keyword, String status,
            java.time.LocalDateTime startDate, java.time.LocalDateTime endDate) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<TradeOrderPO> pageParam = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(
                page, size);
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TradeOrderPO> wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();

        // Keyword search (Order ID or User ID)
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(w -> w.eq(TradeOrderPO::getTradeOrderId, keyword)
                    .or().eq(TradeOrderPO::getUserId, keyword));
        }

        // Status filter
        if (StringUtils.isNotBlank(status)) {
            wrapper.eq(TradeOrderPO::getStatus, status);
        }

        // Date range filter
        if (startDate != null) {
            wrapper.ge(TradeOrderPO::getCreateTime, startDate);
        }
        if (endDate != null) {
            wrapper.le(TradeOrderPO::getCreateTime, endDate);
        }

        wrapper.orderByDesc(TradeOrderPO::getCreateTime);

        com.baomidou.mybatisplus.core.metadata.IPage<TradeOrderPO> resultPage = tradeOrderMapper.selectPage(pageParam,
                wrapper);

        List<TradeOrder> list = resultPage.getRecords().stream()
                .map(tradeOrderConverter::toDomain)
                .collect(Collectors.toList());

        return new org.example.common.model.PageResult<>(list, resultPage.getTotal(), page, size);
    }
}
