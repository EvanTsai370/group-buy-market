package org.example.domain.service.discount;

import org.example.domain.model.activity.Discount;

import java.math.BigDecimal;

/**
 * 折扣计算器接口
 * 定义折扣计算的领域服务
 */
public interface DiscountCalculator {

    /**
     * 计算折扣后的实付金额
     *
     * @param userId 用户ID
     * @param originalPrice 原价
     * @param discount 折扣配置
     * @return 实付金额
     */
    BigDecimal calculate(String userId, BigDecimal originalPrice, Discount discount);
}
