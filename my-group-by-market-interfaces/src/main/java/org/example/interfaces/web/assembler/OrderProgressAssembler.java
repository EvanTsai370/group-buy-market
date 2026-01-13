package org.example.interfaces.web.assembler;

import org.example.application.result.OrderMemberResult;
import org.example.application.result.OrderProgressResult;
import org.example.domain.model.order.valueobject.OrderStatus;
import org.example.domain.model.trade.valueobject.TradeStatus;
import org.example.interfaces.web.dto.customer.OrderMemberDTO;
import org.example.interfaces.web.response.OrderProgressResponse;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 拼团进度 MapStruct 转换器
 *
 * 负责将 Application 层的 Result 转换为 Interfaces 层的 Response/DTO
 *
 * @author 开发团队
 * @since 2026-01-13
 */
@Mapper(componentModel = "spring")
public interface OrderProgressAssembler {

    /**
     * Result → Response
     */
    OrderProgressResponse toResponse(OrderProgressResult result);

    /**
     * 成员列表转换
     */
    List<OrderMemberDTO> toMemberDTOList(List<OrderMemberResult> results);

    /**
     * 单个成员转换
     */
    OrderMemberDTO toMemberDTO(OrderMemberResult result);

    /**
     * 状态枚举转换为字符串
     */
    default String mapOrderStatus(OrderStatus status) {
        return status != null ? status.name() : null;
    }

    default String mapTradeStatus(TradeStatus status) {
        return status != null ? status.name() : null;
    }
}
