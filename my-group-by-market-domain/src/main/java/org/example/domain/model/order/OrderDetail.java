package org.example.domain.model.order;

import lombok.Data;
import org.example.domain.model.order.valueobject.UserType;
import org.example.domain.shared.IdGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * OrderDetail 内部实体（订单明细）
 * 生命周期由 Order 聚合根管理
 */
@Data
public class OrderDetail {

    /** 明细ID */
    private String detailId;

    /** 用户ID */
    private String userId;

    /** 用户类型（团长/团员） */
    private UserType userType;

    /** 外部交易单号（幂等性保证） */
    private String outTradeNo;

    /** 实际支付金额 */
    private BigDecimal payAmount;

    /** 创建时间 */
    private LocalDateTime createTime;

    /**
     * 工厂方法
     */
    public static OrderDetail create(
            String userId,
            UserType userType,
            String outTradeNo,
            BigDecimal payAmount,
            IdGenerator idGenerator) {

        OrderDetail detail = new OrderDetail();
        detail.detailId = generateDetailId(idGenerator);
        detail.userId = userId;
        detail.userType = userType;
        detail.outTradeNo = outTradeNo;
        detail.payAmount = payAmount;
        detail.createTime = LocalDateTime.now();

        return detail;
    }

    /**
     * 生成明细ID（使用IdGenerator）
     */
    private static String generateDetailId(IdGenerator idGenerator) {
        return "D" + idGenerator.nextId();
    }
}