package org.example.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.application.result.OrderMemberResult;
import org.example.application.result.OrderProgressResult;
import org.example.common.exception.BizException;
import org.example.domain.model.activity.Activity;
import org.example.domain.model.activity.repository.ActivityRepository;
import org.example.domain.model.goods.Sku;
import org.example.domain.model.goods.Spu;
import org.example.domain.model.goods.repository.SkuRepository;
import org.example.domain.model.goods.repository.SpuRepository;
import org.example.domain.model.order.Order;
import org.example.domain.model.order.repository.OrderRepository;
import org.example.domain.model.trade.TradeOrder;
import org.example.domain.model.trade.repository.TradeOrderRepository;
import org.example.domain.model.trade.valueobject.TradeStatus;
import org.example.domain.model.user.User;
import org.example.domain.model.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 拼团进度查询服务
 *
 * 提供拼团订单的实时进度查询功能,包括:
 * - 拼团基本信息(活动、商品、目标人数等)
 * - 实时进度(已完成人数、锁定人数、剩余时间)
 * - 成员列表(所有参与用户的详情,含购买的SKU)
 *
 * 业务规则:
 * - 仅显示 CREATE 和 PAID 状态的成员,过滤 REFUND/TIMEOUT/SETTLED
 * - 成员列表按团长优先 → 加入时间升序排序
 * - 不脱敏,显示完整昵称和头像(增强社交氛围)
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderProgressService {

    private final OrderRepository orderRepository;
    private final TradeOrderRepository tradeOrderRepository;
    private final UserRepository userRepository;
    private final SkuRepository skuRepository;
    private final SpuRepository spuRepository;
    private final ActivityRepository activityRepository;

    /**
     * 查询拼团进度
     *
     * @param orderId 拼团订单ID
     * @return 拼团进度详情
     */
    public OrderProgressResult queryOrderProgress(String orderId) {
        log.info("【OrderProgressService】查询拼团进度, orderId: {}", orderId);

        // 1. 查询 Order 聚合
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BizException("拼团订单不存在"));

        // 2. 查询 TradeOrder 列表,仅保留 CREATE 和 PAID 状态
        List<TradeOrder> tradeOrders = tradeOrderRepository.findByOrderId(orderId);
        List<TradeOrder> validTradeOrders = tradeOrders.stream()
                .filter(to -> to.getStatus() == TradeStatus.CREATE || to.getStatus() == TradeStatus.PAID)
                .toList();

        log.info("【OrderProgressService】查询到{}条交易订单,过滤后{}条有效订单",
                tradeOrders.size(), validTradeOrders.size());

        // 3. 批量查询 User 信息
        Set<String> userIds = validTradeOrders.stream()
                .map(TradeOrder::getUserId)
                .collect(Collectors.toSet());
        userIds.add(order.getLeaderUserId()); // 确保包含团长

        Map<String, User> userMap = new HashMap<>();
        for (String userId : userIds) {
            userRepository.findByUserId(userId).ifPresent(user -> userMap.put(userId, user));
        }

        // 4. 批量查询 Sku 信息
        Set<String> skuIds = validTradeOrders.stream()
                .map(TradeOrder::getSkuId)
                .collect(Collectors.toSet());

        Map<String, Sku> skuMap = new HashMap<>();
        for (String skuId : skuIds) {
            skuRepository.findBySkuId(skuId).ifPresent(sku -> skuMap.put(skuId, sku));
        }

        // 5. 查询 Spu 信息
        Spu spu = spuRepository.findBySpuId(order.getSpuId())
                .orElse(null);

        // 6. 查询 Activity 信息
        Activity activity = activityRepository.findById(order.getActivityId())
                .orElse(null);

        // 7. 组装成员列表
        List<OrderMemberResult> members = validTradeOrders.stream()
                .map(tradeOrder -> {
                    OrderMemberResult member = new OrderMemberResult();
                    member.setUserId(tradeOrder.getUserId());
                    member.setSkuId(tradeOrder.getSkuId());
                    member.setStatus(tradeOrder.getStatus());
                    member.setJoinTime(tradeOrder.getCreateTime());
                    member.setIsLeader(tradeOrder.getUserId().equals(order.getLeaderUserId()));

                    // 填充 User 信息
                    User user = userMap.get(tradeOrder.getUserId());
                    if (user != null) {
                        member.setNickname(user.getNickname());
                        member.setAvatar(user.getAvatar());
                    }

                    // 填充 Sku 信息
                    Sku sku = skuMap.get(tradeOrder.getSkuId());
                    if (sku != null) {
                        member.setSkuName(sku.getGoodsName());
                    }

                    return member;
                })
                .sorted(Comparator
                        .comparing(OrderMemberResult::getIsLeader, Comparator.reverseOrder()) // 团长优先
                        .thenComparing(OrderMemberResult::getJoinTime)) // 按加入时间升序
                .collect(Collectors.toList());

        // 8. 计算进度信息
        Integer remainingCount = order.getTargetCount() - order.getCompleteCount();
        Integer progress = order.getTargetCount() > 0
                ? (order.getCompleteCount() * 100 / order.getTargetCount())
                : 0;

        LocalDateTime now = LocalDateTime.now();
        long remainingSeconds = 0L;
        if (order.getDeadlineTime() != null && order.getDeadlineTime().isAfter(now)) {
            Duration duration = Duration.between(now, order.getDeadlineTime());
            remainingSeconds = duration.getSeconds();
        }

        // 9. 查询团长信息
        User leader = userMap.get(order.getLeaderUserId());

        // 10. 组装结果
        OrderProgressResult result = new OrderProgressResult();
        result.setOrderId(order.getOrderId());
        result.setTeamId(order.getTeamId());
        result.setActivityId(order.getActivityId());
        result.setActivityName(activity != null ? activity.getActivityName() : null);
        result.setSpuId(order.getSpuId());
        result.setSpuName(spu != null ? spu.getSpuName() : null);
        result.setStatus(order.getStatus());
        result.setTargetCount(order.getTargetCount());
        result.setCompleteCount(order.getCompleteCount());
        result.setLockCount(order.getLockCount());
        result.setRemainingCount(remainingCount);
        result.setProgress(progress);
        result.setLeaderUserId(order.getLeaderUserId());
        result.setLeaderNickname(leader != null ? leader.getNickname() : null);
        result.setLeaderAvatar(leader != null ? leader.getAvatar() : null);
        result.setDeadline(order.getDeadlineTime());
        result.setRemainingSeconds(remainingSeconds);
        result.setCreateTime(order.getStartTime());
        result.setCompletedTime(order.getCompletedTime());
        result.setMembers(members);

        log.info("【OrderProgressService】查询拼团进度完成, orderId: {}, 进度: {}/{}",
                orderId, order.getCompleteCount(), order.getTargetCount());

        return result;
    }
}
