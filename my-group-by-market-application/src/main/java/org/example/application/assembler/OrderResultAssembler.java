package org.example.application.assembler;

import org.example.application.service.admin.result.OrderResult;
import org.example.application.service.admin.result.TradeOrderResult;
import org.example.domain.model.order.Order;
import org.example.domain.model.trade.TradeOrder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 订单结果转换器（Application 层）
 *
 * <p>
 * 职责：领域模型 → 视图模型转换
 *
 * @author 开发团队
 * @since 2026-01-11
 */
@Mapper(componentModel = "spring")
public interface OrderResultAssembler {

    /**
     * Order → OrderResult 转换
     */
    @Mapping(target = "status", expression = "java(order.getStatus() != null ? order.getStatus().name() : null)")
    OrderResult toResult(Order order);

    /**
     * Order 列表转换
     */
    List<OrderResult> toOrderResultList(List<Order> orders);

    /**
     * TradeOrder → TradeOrderResult 转换
     */
    @Mapping(target = "status", expression = "java(tradeOrder.getStatus() != null ? tradeOrder.getStatus().name() : null)")
    TradeOrderResult toResult(TradeOrder tradeOrder);

    /**
     * TradeOrder 列表转换
     */
    List<TradeOrderResult> toTradeOrderResultList(List<TradeOrder> tradeOrders);
}
