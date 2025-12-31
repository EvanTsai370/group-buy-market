package org.example.domain.model.order.valueobject;

import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Money 值对象（金额）
 * 封装价格相关的业务逻辑
 *
 * 值对象特性：
 * 1. 不可变性 - 创建后状态不可改变
 * 2. 值相等性 - 通过属性值判断相等
 * 3. 无副作用 - 所有操作返回新对象
 */
@Getter
public final class Money {

    /** 原始价格 */
    private final BigDecimal originalPrice;

    /** 折扣价格 */
    private final BigDecimal deductionPrice;

    /**
     * 私有无参构造函数，仅供 ORM/序列化框架使用
     */
    private Money() {
        this.originalPrice = BigDecimal.ZERO;
        this.deductionPrice = BigDecimal.ZERO;
    }

    /**
     * 构造函数 - 所有必需状态在此一次性传入
     */
    public Money(BigDecimal originalPrice, BigDecimal deductionPrice) {
        this.originalPrice = Objects.requireNonNull(originalPrice, "原始价格不能为空");
        this.deductionPrice = Objects.requireNonNull(deductionPrice, "折扣价格不能为空");
    }

    /**
     * 静态工厂方法
     */
    public static Money of(BigDecimal originalPrice, BigDecimal deductionPrice) {
        return new Money(originalPrice, deductionPrice);
    }

    /**
     * 计算折扣金额
     */
    public BigDecimal getDiscountAmount() {
        return originalPrice.subtract(deductionPrice);
    }

    /**
     * 计算折扣率
     */
    public BigDecimal getDiscountRate() {
        if (originalPrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return deductionPrice.divide(originalPrice, 2, RoundingMode.HALF_UP);
    }

    /**
     * 值相等性判断 - 基于属性值而非引用
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return Objects.equals(originalPrice, money.originalPrice)
                && Objects.equals(deductionPrice, money.deductionPrice);
    }

    @Override
    public int hashCode() {
        return Objects.hash(originalPrice, deductionPrice);
    }

    @Override
    public String toString() {
        return "Money{" +
                "originalPrice=" + originalPrice +
                ", deductionPrice=" + deductionPrice +
                '}';
    }
}
