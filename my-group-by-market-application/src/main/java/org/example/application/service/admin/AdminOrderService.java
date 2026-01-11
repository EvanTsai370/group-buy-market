package org.example.application.service.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.application.assembler.OrderResultAssembler;
import org.example.application.service.admin.result.OrderResult;
import org.example.application.service.admin.result.TradeOrderResult;
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
    private final OrderResultAssembler orderResultAssembler;

    /**
     * 获取拼团订单详情
     */
    public OrderResult getOrderDetail(String orderId) {
        log.info("【AdminOrder】查询拼团订单详情, orderId: {}", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BizException("拼团订单不存在"));
        return orderResultAssembler.toResult(order);
    }

    /**
     * 获取拼团订单（可选）
     */
    public Optional<OrderResult> findOrderById(String orderId) {
        return orderRepository.findById(orderId)
                .map(orderResultAssembler::toResult);
    }

    /**
     * 获取交易订单详情
     */
    public TradeOrderResult getTradeOrderDetail(String tradeOrderId) {
        log.info("【AdminOrder】查询交易订单详情, tradeOrderId: {}", tradeOrderId);
        TradeOrder tradeOrder = tradeOrderRepository.findByTradeOrderId(tradeOrderId)
                .orElseThrow(() -> new BizException("交易订单不存在"));
        return orderResultAssembler.toResult(tradeOrder);
    }

    /**
     * 根据拼团订单ID获取交易订单列表
     */
    public List<TradeOrderResult> listTradeOrdersByOrderId(String orderId) {
        log.info("【AdminOrder】查询拼团订单的交易明细, orderId: {}", orderId);
        List<TradeOrder> tradeOrders = tradeOrderRepository.findByOrderId(orderId);
        return orderResultAssembler.toTradeOrderResultList(tradeOrders);
    }

    /**
     * 根据活动ID获取拼团订单列表
     */
    public List<OrderResult> listOrdersByActivityId(String activityId) {
        log.info("【AdminOrder】查询活动的拼团订单, activityId: {}", activityId);
        List<Order> orders = orderRepository.findPendingOrdersByActivity(activityId);
        return orderResultAssembler.toOrderResultList(orders);
    }
}
