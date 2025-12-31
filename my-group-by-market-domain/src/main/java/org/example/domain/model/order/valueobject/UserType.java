// ============ 文件: domain/model/order/valueobject/UserType.java ============
package org.example.domain.model.order.valueobject;

import lombok.Getter;

/**
 * 用户类型（值对象）
 */
@Getter
public enum UserType {

    /** 团长 */
    LEADER("团长"),

    /** 团员 */
    MEMBER("团员");

    private final String desc;

    UserType(String desc) {
        this.desc = desc;
    }

}