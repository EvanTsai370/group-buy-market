package org.example.interfaces.web.assembler;

import org.example.application.service.admin.result.OrderResult;
import org.example.application.service.admin.result.SystemInfoResult;
import org.example.application.service.admin.result.TradeOrderResult;
import org.example.interfaces.web.dto.admin.AdminTradeOrderResponse;
import org.example.interfaces.web.dto.admin.OrderResponse;
import org.example.interfaces.web.dto.admin.SystemInfoResponse;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 订单管理转换器（Interfaces 层）
 *
 * <p>
 * 职责：Result → Response 转换
 *
 * @author 开发团队
 * @since 2026-01-11
 */
@Mapper(componentModel = "spring")
public interface AdminOrderAssembler {

    /**
     * OrderResult → OrderResponse
     */
    OrderResponse toResponse(OrderResult result);

    /**
     * OrderResult 列表转换
     */
    List<OrderResponse> toOrderResponseList(List<OrderResult> results);

    /**
     * TradeOrderResult → AdminTradeOrderResponse
     */
    AdminTradeOrderResponse toResponse(TradeOrderResult result);

    /**
     * TradeOrderResult 列表转换
     */
    List<AdminTradeOrderResponse> toTradeOrderResponseList(List<TradeOrderResult> results);

    /**
     * SystemInfoResult → SystemInfoResponse
     */
    SystemInfoResponse toResponse(SystemInfoResult result);
}
