package org.example.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.example.common.exception.BizException;
import org.example.common.exception.ErrorCode;
import org.example.domain.model.account.Account;
import org.example.domain.model.account.repository.AccountRepository;
import org.example.domain.model.order.Order;
import org.example.domain.model.order.OrderDetail;
import org.example.domain.model.order.repository.OrderRepository;
import org.example.domain.model.order.valueobject.OrderStatus;
import org.example.domain.shared.IdGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 加入拼团领域服务
 * 职责：协调 Account 和 Order 两个聚合，完成用户加入拼团的业务流程
 *
 * 设计说明：
 * - 领域模型（Order.validateJoin）负责业务规则校验
 * - Repository（tryIncrementCompleteCount）负责并发安全
 * - 领域服务负责协调流程和补偿逻辑
 *
 * 注意：这是纯领域服务，不包含任何Spring注解
 */
@Slf4j
public class JoinOrderService {

    private final AccountRepository accountRepository;
    private final OrderRepository orderRepository;
    private final IdGenerator idGenerator;

    /**
     * 构造函数注入依赖
     */
    public JoinOrderService(
            AccountRepository accountRepository,
            OrderRepository orderRepository,
            IdGenerator idGenerator) {
        this.accountRepository = accountRepository;
        this.orderRepository = orderRepository;
        this.idGenerator = idGenerator;
    }

    /**
     * 用户加入拼团（核心领域服务）
     *
     * 流程：
     * 1. 加载订单聚合，执行业务规则校验（快速失败）
     * 2. 扣减用户参团次数
     * 3. 原子化更新拼团人数（数据库层面保证并发安全）
     * 4. 保存订单明细
     * 5. 如果步骤3失败，补偿参团次数
     *
     * 并发安全说明：
     * - 使用条件更新代替乐观锁，避免高并发下的误杀问题
     * - 在 complete_count < target_count 条件下更新，保证不会超卖
     */
    public OrderDetail joinOrder(
            String userId,
            String orderId,
            String outTradeNo,
            BigDecimal payAmount) {

        log.info("【领域服务】用户加入拼团开始, userId: {}, orderId: {}", userId, orderId);

        // 1. 加载订单聚合，执行业务规则校验（快速失败）
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BizException(ErrorCode.ORDER_NOT_FOUND));

        // 领域模型负责业务规则校验
        order.validateJoin(userId);

        // 2. 查询并扣减用户参团次数
        Account account = accountRepository.findByUserAndActivity(userId, order.getActivityId())
                .orElseThrow(() -> new BizException(ErrorCode.ACCOUNT_NOT_FOUND));

        account.deductCount();
        accountRepository.save(account);
        log.info("【领域服务】参团次数扣减成功, userId: {}", userId);

        // 3. 原子化更新拼团人数（数据库层面保证并发安全）
        int newCompleteCount = orderRepository.tryIncrementCompleteCount(orderId);

        if (newCompleteCount < 0) {
            // 更新失败，执行补偿
            log.warn("【领域服务】原子更新失败，开始补偿, userId: {}, orderId: {}", userId, orderId);
            account.compensateCount();
            accountRepository.save(account);

            // 重新加载订单，判断失败原因
            Order latestOrder = orderRepository.findById(orderId)
                    .orElseThrow(() -> new BizException(ErrorCode.ORDER_NOT_FOUND));
            throw determineFailureReason(latestOrder);
        }

        // 4. 创建并保存订单明细
        OrderDetail detail = order.createDetail(userId, outTradeNo, payAmount, idGenerator);
        orderRepository.saveDetail(orderId, detail);

        log.info("【领域服务】用户加入拼团成功, userId: {}, orderId: {}, progress: {}/{}",
                userId, orderId, newCompleteCount, order.getTargetCount());

        return detail;
    }

    /**
     * 根据订单最新状态判断失败原因
     */
    private BizException determineFailureReason(Order order) {
        if (order.getCompleteCount() >= order.getTargetCount()) {
            return new BizException(ErrorCode.ORDER_ALREADY_FULL);
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            return new BizException(ErrorCode.ORDER_CLOSED);
        }
        if (LocalDateTime.now().isAfter(order.getDeadlineTime())) {
            return new BizException(ErrorCode.ORDER_EXPIRED);
        }
        // 其他未知原因
        return new BizException(ErrorCode.ORDER_JOIN_FAILED);
    }
}