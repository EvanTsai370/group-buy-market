// ============ 文件: domain/model/order/valueobject/OrderStatus.java ============
package org.example.domain.model.order.valueobject;

import lombok.Getter;

/**
 * 订单状态（值对象）
 */
@Getter
public enum OrderStatus {

    /** 进行中 */
    PENDING("进行中"),

    /** 已成团 */
    SUCCESS("已成团"),

    /** 已失败 */
    FAILED("已失败");

    private final String desc;

    OrderStatus(String desc) {
        this.desc = desc;
    }

}