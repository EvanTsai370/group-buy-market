// ============ 文件: OrderConverter.java ============
package org.example.infrastructure.persistence.converter;

import org.example.domain.model.order.Order;
import org.example.domain.model.order.valueobject.OrderStatus;
import org.example.infrastructure.persistence.po.OrderPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

/**
 * Order 转换器
 */
@Mapper
public interface OrderConverter {

    OrderConverter INSTANCE = Mappers.getMapper(OrderConverter.class);

    /**
     * PO 转 Domain Entity
     */
    @Mapping(source = "status", target = "status", qualifiedByName = "stringToOrderStatus")
    Order toDomain(OrderPO po);

    /**
     * Domain Entity 转 PO
     */
    @Mapping(source = "status", target = "status", qualifiedByName = "orderStatusToString")
    OrderPO toPO(Order order);

    /**
     * String 转 OrderStatus
     */
    @Named("stringToOrderStatus")
    default OrderStatus stringToOrderStatus(String status) {
        return status == null ? null : OrderStatus.valueOf(status);
    }

    /**
     * OrderStatus 转 String
     */
    @Named("orderStatusToString")
    default String orderStatusToString(OrderStatus status) {
        return status == null ? null : status.name();
    }
}