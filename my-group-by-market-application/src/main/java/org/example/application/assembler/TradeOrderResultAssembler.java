package org.example.application.assembler;

import org.example.application.service.trade.result.TradeOrderResult;
import org.example.domain.model.trade.TradeOrder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 交易订单结果转换器
 *
 * <p>
 * 职责：Domain → Result 转换（领域模型 → 用例输出）
 *
 * <p>
 * 设计说明：
 * <ul>
 * <li>这是 Application 层的 Assembler，负责将领域对象转换为用例结果对象</li>
 * <li>不处理 Cmd → Domain 转换（由领域工厂方法和领域服务处理）</li>
 * <li>不处理 Query → Domain 转换（Query 用于查询参数，不创建 Domain）</li>
 * </ul>
 *
 * @author 开发团队
 * @since 2026-01-04
 */
@Mapper(componentModel = "spring")
public interface TradeOrderResultAssembler {

    /**
     * Domain → Result 转换
     *
     * @param tradeOrder 交易订单领域对象
     * @return TradeOrderResult
     */
    @Mapping(target = "status", expression = "java(tradeOrder.getStatus().getCode())")
    TradeOrderResult toResult(TradeOrder tradeOrder);

    /**
     * Domain 列表 → Result 列表转换
     *
     * @param tradeOrders 交易订单领域对象列表
     * @return TradeOrderResult 列表
     */
    List<TradeOrderResult> toResultList(List<TradeOrder> tradeOrders);
}
