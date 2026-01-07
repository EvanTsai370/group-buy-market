package org.example.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.model.order.Order;
import org.example.domain.model.order.repository.OrderRepository;
import org.example.domain.model.order.valueobject.OrderStatus;
import org.example.domain.shared.IdGenerator;
import org.example.infrastructure.persistence.converter.OrderConverter;
import org.example.infrastructure.persistence.mapper.OrderMapper;
import org.example.infrastructure.persistence.po.OrderPO;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Order 仓储实现
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderMapper orderMapper;
    private final IdGenerator idGenerator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(Order order) {
        // 保存主单
        OrderPO orderPO = OrderConverter.INSTANCE.toPO(order);

        if (orderPO.getId() == null) {
            // 新增
            orderMapper.insert(orderPO);
            log.info("【OrderRepository】新增订单, orderId: {}", order.getOrderId());
        } else {
            // 更新（包含乐观锁）
            int rows = orderMapper.updateById(orderPO);
            if (rows == 0) {
                throw new RuntimeException("订单更新失败，版本冲突: orderId=" + order.getOrderId());
            }
            log.info("【OrderRepository】更新订单, orderId: {}, version: {}",
                    order.getOrderId(), order.getVersion());
        }
    }

    @Override
    public Optional<Order> findById(String orderId) {
        // 查询主单
        LambdaQueryWrapper<OrderPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderPO::getOrderId, orderId);

        OrderPO orderPO = orderMapper.selectOne(wrapper);
        if (orderPO == null) {
            return Optional.empty();
        }

        Order order = OrderConverter.INSTANCE.toDomain(orderPO);
        return Optional.of(order);
    }

    @Override
    public List<Order> findPendingOrdersByActivity(String activityId) {
        LambdaQueryWrapper<OrderPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderPO::getActivityId, activityId)
                .eq(OrderPO::getStatus, OrderStatus.PENDING.name());

        List<OrderPO> poList = orderMapper.selectList(wrapper);
        return poList.stream()
                .map(OrderConverter.INSTANCE::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Order> findTimeoutOrders() {
        List<OrderPO> poList = orderMapper.selectTimeoutOrders();
        return poList.stream()
                .map(OrderConverter.INSTANCE::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Order> findVirtualCompletableOrders() {
        List<OrderPO> poList = orderMapper.selectVirtualCompletableOrders();
        return poList.stream()
                .map(OrderConverter.INSTANCE::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void updateStatus(String orderId, OrderStatus status) {
        LambdaQueryWrapper<OrderPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderPO::getOrderId, orderId);

        OrderPO po = new OrderPO();
        po.setStatus(status.name());

        orderMapper.update(po, wrapper);
        log.info("【OrderRepository】更新订单状态, orderId: {}, status: {}", orderId, status);
    }

    @Override
    public int incrementLockCount(String orderId) {
        // 原子化更新：通过条件更新保证并发安全
        int rows = orderMapper.incrementLockCount(orderId);

        if (rows == 0) {
            log.warn("【OrderRepository】锁单量增加失败（拼团已满/已结束/已过期）, orderId: {}", orderId);
            return -1;
        }

        // 查询最新的 lockCount（确保内存与数据库一致）
        LambdaQueryWrapper<OrderPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderPO::getOrderId, orderId);
        OrderPO orderPO = orderMapper.selectOne(wrapper);

        log.info("【OrderRepository】锁单量增加成功, orderId: {}, lockCount: {}/{}",
                orderId, orderPO.getLockCount(), orderPO.getTargetCount());

        return orderPO.getLockCount();
    }

    @Override
    public boolean decrementLockCount(String orderId) {
        // 原子化更新：通过条件更新保证并发安全
        int rows = orderMapper.decrementLockCount(orderId);

        if (rows == 0) {
            log.warn("【OrderRepository】锁单量减少失败（lockCount已为0）, orderId: {}", orderId);
            return false;
        }

        log.info("【OrderRepository】锁单量减少成功, orderId: {}", orderId);
        return true;
    }

    @Override
    public int tryIncrementCompleteCount(String orderId) {
        // 原子化更新：通过条件更新保证并发安全
        int rows = orderMapper.incrementCompleteCount(orderId);

        if (rows == 0) {
            log.warn("【OrderRepository】原子更新失败（拼团已满/已结束/已过期）, orderId: {}", orderId);
            return -1;
        }

        // 更新成功，查询最新的完成人数
        LambdaQueryWrapper<OrderPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderPO::getOrderId, orderId);
        OrderPO orderPO = orderMapper.selectOne(wrapper);

        log.info("【OrderRepository】原子更新成功, orderId: {}, completeCount: {}/{}, status: {}",
                orderId, orderPO.getCompleteCount(), orderPO.getTargetCount(), orderPO.getStatus());

        return orderPO.getCompleteCount();
    }

    @Override
    public String nextId() {
        return "ORD" + idGenerator.nextId();
    }
}