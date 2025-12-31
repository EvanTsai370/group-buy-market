// ============ 文件: OrderConverter.java ============
package org.example.infrastructure.persistence.converter;

import org.example.domain.model.order.Order;
import org.example.domain.model.order.OrderDetail;
import org.example.domain.model.order.valueobject.OrderStatus;
import org.example.domain.model.order.valueobject.UserType;
import org.example.infrastructure.persistence.po.OrderDetailPO;
import org.example.infrastructure.persistence.po.OrderPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

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
    @Mapping(target = "details", ignore = true)  // 明细单独处理
    Order toDomain(OrderPO po);

    /**
     * Domain Entity 转 PO
     */
    @Mapping(source = "status", target = "status", qualifiedByName = "orderStatusToString")
    OrderPO toPO(Order order);

    /**
     * OrderDetail PO 转 Domain Entity
     */
    @Mapping(source = "userType", target = "userType", qualifiedByName = "stringToUserType")
    OrderDetail detailToDomain(OrderDetailPO po);

    /**
     * OrderDetail Domain Entity 转 PO
     */
    @Mapping(source = "userType", target = "userType", qualifiedByName = "userTypeToString")
    @Mapping(target = "orderId", ignore = true)  // 由外部设置
    @Mapping(target = "activityId", ignore = true)  // 由外部设置
    OrderDetailPO detailToPO(OrderDetail detail);

    /**
     * 批量转换明细
     */
    List<OrderDetail> detailsToDomain(List<OrderDetailPO> poList);

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

    /**
     * String 转 UserType
     */
    @Named("stringToUserType")
    default UserType stringToUserType(String userType) {
        return userType == null ? null : UserType.valueOf(userType);
    }

    /**
     * UserType 转 String
     */
    @Named("userTypeToString")
    default String userTypeToString(UserType userType) {
        return userType == null ? null : userType.name();
    }
}