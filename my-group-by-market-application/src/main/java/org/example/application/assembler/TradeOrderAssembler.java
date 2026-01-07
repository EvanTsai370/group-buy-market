package org.example.application.assembler;

import org.example.application.service.trade.vo.TradeOrderVO;
import org.example.domain.model.trade.TradeOrder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 交易订单转换器
 *
 * <p>职责：
 * <ul>
 *   <li>Domain → VO转换（展示层数据转换）</li>
 *   <li>VO → Domain转换（更新场景的数据回填）</li>
 * </ul>
 *
 * <p>注意：
 * <ul>
 *   <li>不处理Cmd → Domain转换（由领域工厂方法和领域服务处理）</li>
 *   <li>不处理Query → Domain转换（Query用于查询参数，不创建Domain）</li>
 * </ul>
 *
 * @author 开发团队
 * @since 2026-01-04
 */
@Mapper(componentModel = "spring")
public interface TradeOrderAssembler {

    /**
     * Domain → VO转换
     *
     * @param tradeOrder 交易订单领域对象
     * @return TradeOrderVO
     */
    @Mapping(target = "status", expression = "java(tradeOrder.getStatus().getCode())")
    TradeOrderVO toVO(TradeOrder tradeOrder);

    /**
     * Domain列表 → VO列表转换
     *
     * @param tradeOrders 交易订单领域对象列表
     * @return TradeOrderVO列表
     */
    List<TradeOrderVO> toVOList(List<TradeOrder> tradeOrders);
}
