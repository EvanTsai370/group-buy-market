package org.example.application.service.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.exception.BizException;
import org.example.domain.model.order.Order;
import org.example.domain.model.order.repository.OrderRepository;
import org.example.domain.model.trade.TradeOrder;
import org.example.domain.model.trade.repository.TradeOrderRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 订单管理服务
 * 
 * @author 开发团队
 * @since 2026-01-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOrderService {

    private final OrderRepository orderRepository;
    private final TradeOrderRepository tradeOrderRepository;

    /**
     * 获取拼团订单详情
     */
    public Order getOrderDetail(String orderId) {
        log.info("【AdminOrder】查询拼团订单详情, orderId: {}", orderId);
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new BizException("拼团订单不存在"));
    }

    /**
     * 获取拼团订单（可选）
     */
    public Optional<Order> findOrderById(String orderId) {
        return orderRepository.findById(orderId);
    }

    /**
     * 获取交易订单详情
     */
    public TradeOrder getTradeOrderDetail(String tradeOrderId) {
        log.info("【AdminOrder】查询交易订单详情, tradeOrderId: {}", tradeOrderId);
        return tradeOrderRepository.findByTradeOrderId(tradeOrderId)
                .orElseThrow(() -> new BizException("交易订单不存在"));
    }

    /**
     * 根据拼团订单ID获取交易订单列表
     */
    public List<TradeOrder> listTradeOrdersByOrderId(String orderId) {
        log.info("【AdminOrder】查询拼团订单的交易明细, orderId: {}", orderId);
        return tradeOrderRepository.findByOrderId(orderId);
    }

    /**
     * 根据活动ID获取拼团订单列表
     */
    public List<Order> listOrdersByActivityId(String activityId) {
        log.info("【AdminOrder】查询活动的拼团订单, activityId: {}", activityId);
        return orderRepository.findPendingOrdersByActivity(activityId);
    }
}
