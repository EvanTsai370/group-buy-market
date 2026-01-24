package org.example.start.discount;

import lombok.extern.slf4j.Slf4j;
import org.example.start.base.IntegrationTestBase;
import org.example.domain.model.activity.Discount;
import org.example.domain.service.discount.FixedPriceDiscountCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test 13: 固定价格折扣计算器验证测试
 *
 * <p>
 * 测试目的：发现 FixedPriceDiscountCalculator 的价格上限校验 Bug
 * <p>
 * 场景：测试固定价格是否超过原价的边界情况
 * <p>
 * 预期（TDD - 先写失败测试）：
 * - 固定价 50 元，原价 100 元 → 返回 50 元（正常）
 * - 固定价 100 元，原价 50 元 → 返回 50 元（原价保护）
 * - 固定价 50 元，原价 50 元 → 返回 50 元（边界值）
 * - 固定价 0.01 元 → 返回 0.01 元（最小值）
 * <p>
 * 可能发现的 Bug：
 * - 无价格上限校验 → 固定价可能高于原价，多收用户钱
 * - 业务逻辑：固定价超过原价时，应返回原价（优雅降级）
 *
 */
@Slf4j
@DisplayName("Test 13: 固定价格折扣验证测试（发现价格上限 Bug）")
public class FixedPriceDiscountCalculatorTest extends IntegrationTestBase {

    @Autowired
    private FixedPriceDiscountCalculator calculator;

    @Test
    @DisplayName("正常场景：固定价低于原价 - 应返回固定价")
    void testNormalCase_FixedPriceLowerThanOriginal() {
        // Given: 原价 100 元，固定价 50 元
        BigDecimal originalPrice = new BigDecimal("100.00");
        Discount discount = createDiscount("50.00");

        // When: 计算折扣后价格
        BigDecimal result = calculator.calculate("USER001", originalPrice, discount);

        // Then: 应该返回 50 元
        log.info("【固定价格测试】原价={}, 固定价=50, 计算结果={}", originalPrice, result);

        assertThat(result)
                .as("固定价低于原价时，应返回固定价")
                .isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    @DisplayName("Bug验证：固定价超过原价 - 应返回原价而非固定价")
    void testFixedPriceExceedsOriginalPrice() {
        // Given: 原价 50 元，固定价 100 元（异常配置）
        BigDecimal originalPrice = new BigDecimal("50.00");
        Discount discount = createDiscount("100.00");

        // When: 计算折扣后价格
        BigDecimal result = calculator.calculate("USER001", originalPrice, discount);

        // Then: 应该返回原价 50 元，而不是固定价 100 元
        // 【预期失败】如果代码没有价格上限校验，这里会失败
        log.info("【固定价格测试】原价={}, 固定价=100, 计算结果={}", originalPrice, result);

        assertThat(result)
                .as("固定价超过原价时，应返回原价（优雅降级），而不是多收用户钱")
                .isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    @DisplayName("边界值测试：固定价等于原价 - 应返回固定价")
    void testFixedPriceEqualsOriginalPrice() {
        // Given: 原价 50 元，固定价 50 元
        BigDecimal originalPrice = new BigDecimal("50.00");
        Discount discount = createDiscount("50.00");

        // When: 计算折扣后价格
        BigDecimal result = calculator.calculate("USER001", originalPrice, discount);

        // Then: 应该返回 50 元
        log.info("【固定价格测试】原价={}, 固定价=50, 计算结果={}", originalPrice, result);

        assertThat(result)
                .as("固定价等于原价时，应返回固定价")
                .isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    @DisplayName("边界值测试：固定价为最小值 - 应返回0.01元")
    void testMinimumFixedPrice() {
        // Given: 原价 100 元，固定价 0.01 元（最小值）
        BigDecimal originalPrice = new BigDecimal("100.00");
        Discount discount = createDiscount("0.01");

        // When: 计算折扣后价格
        BigDecimal result = calculator.calculate("USER001", originalPrice, discount);

        // Then: 应该返回 0.01 元
        log.info("【固定价格测试】原价={}, 固定价=0.01, 计算结果={}", originalPrice, result);

        assertThat(result)
                .as("固定价为最小值时，应正确返回 0.01 元")
                .isEqualByComparingTo(new BigDecimal("0.01"));
    }

    @Test
    @DisplayName("极限测试：固定价远超原价 - 应返回原价")
    void testFixedPriceFarExceedsOriginalPrice() {
        // Given: 原价 9.9 元，固定价 999.99 元（极端异常配置）
        BigDecimal originalPrice = new BigDecimal("9.90");
        Discount discount = createDiscount("999.99");

        // When: 计算折扣后价格
        BigDecimal result = calculator.calculate("USER001", originalPrice, discount);

        // Then: 应该返回原价 9.9 元
        log.info("【固定价格测试】原价={}, 固定价=999.99, 计算结果={}", originalPrice, result);

        assertThat(result)
                .as("固定价远超原价时，应返回原价保护用户")
                .isEqualByComparingTo(new BigDecimal("9.90"));
    }

    /**
     * 辅助方法：创建 Discount 对象
     */
    private Discount createDiscount(String fixedPrice) {
        Discount discount = new Discount();
        discount.setDiscountId("DISCOUNT_TEST");
        discount.setMarketPlan("N"); // N元购
        discount.setMarketExpr(fixedPrice);
        return discount;
    }
}
