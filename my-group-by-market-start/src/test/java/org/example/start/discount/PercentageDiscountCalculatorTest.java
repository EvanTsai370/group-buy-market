package org.example.start.discount;

import lombok.extern.slf4j.Slf4j;
import org.example.start.base.IntegrationTestBase;
import org.example.domain.model.activity.Discount;
import org.example.domain.service.discount.PercentageDiscountCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test 3: 百分比折扣计算器精度测试
 *
 * <p>
 * 测试目的：发现 PercentageDiscountCalculator 的精度丢失 Bug
 * <p>
 * 场景：测试价格计算的数值精度
 * <p>
 * 预期（TDD - 先写失败测试）：
 * - 9.99 * 0.8 = 7.99（不是 7）
 * - 100.01 * 0.33 = 33.00（向下取整到 2 位小数）
 * - 0.03 * 0.5 = 0.01（最小值保护）
 * <p>
 * 可能发现的 Bug：
 * - setScale(0, DOWN) 导致精度丢失 → 每笔损失 0.01~0.99 元
 * - 最小值保护逻辑有 off-by-one 错误
 *
 * @author 测试团队
 * @since 2026-01-13
 */
@Slf4j
@DisplayName("Test 3: 百分比折扣精度测试（发现价格精度 Bug）")
public class PercentageDiscountCalculatorTest extends IntegrationTestBase {

    @Autowired
    private PercentageDiscountCalculator calculator;

    @Test
    @DisplayName("Bug验证：9.99元打8折 - 应该是7.99元而非7元")
    void testPrecisionLoss_9_99_times_0_8() {
        // Given: 原价 9.99 元，打 8 折（0.8）
        BigDecimal originalPrice = new BigDecimal("9.99");
        Discount discount = createDiscount("0.8");  // 百分比折扣：80%

        // When: 计算折扣后价格
        BigDecimal result = calculator.calculate("USER001", originalPrice, discount);

        // Then: 应该是 7.99 元，而不是 7 元
        // 【预期失败】如果代码使用 setScale(0, DOWN)，这里会失败
        log.info("【价格精度测试】原价={}, 折扣=0.8, 计算结果={}", originalPrice, result);

        assertThat(result)
            .as("9.99 * 0.8 应该等于 7.99（保留2位小数），而不是 7")
            .isEqualByComparingTo(new BigDecimal("7.99"));
    }

    @Test
    @DisplayName("Bug验证：100.01元打33折 - 应该正确保留2位小数")
    void testPrecisionRounding_100_01_times_0_33() {
        // Given: 原价 100.01 元，打 33 折（0.33）
        BigDecimal originalPrice = new BigDecimal("100.01");
        Discount discount = createDiscount("0.33");

        // When: 计算折扣后价格
        BigDecimal result = calculator.calculate("USER001", originalPrice, discount);

        // Then: 100.01 * 0.33 = 33.0033 → 应该向下取整到 33.00
        log.info("【价格精度测试】原价={}, 折扣=0.33, 计算结果={}", originalPrice, result);

        assertThat(result)
            .as("100.01 * 0.33 应该等于 33.00（向下取整2位小数）")
            .isEqualByComparingTo(new BigDecimal("33.00"));
    }

    @Test
    @DisplayName("边界值测试：极小金额打5折 - 应触发最小值保护（0.01元）")
    void testMinimumPriceProtection() {
        // Given: 原价 0.03 元，打 5 折（0.5）
        BigDecimal originalPrice = new BigDecimal("0.03");
        Discount discount = createDiscount("0.5");

        // When: 计算折扣后价格
        BigDecimal result = calculator.calculate("USER001", originalPrice, discount);

        // Then: 0.03 * 0.5 = 0.015 → 应该被最小值保护拦截，返回 0.01
        log.info("【价格精度测试】原价={}, 折扣=0.5, 计算结果={}", originalPrice, result);

        assertThat(result)
            .as("极小金额折扣后应该触发最小值保护，返回 0.01 元")
            .isEqualByComparingTo(new BigDecimal("0.01"));
    }

    @Test
    @DisplayName("边界值测试：1元打1折 - 应该是0.01元（最小值）")
    void testMinimumPrice_1_yuan_times_0_01() {
        // Given: 原价 1 元，打 1 折（0.01）
        BigDecimal originalPrice = new BigDecimal("1.00");
        Discount discount = createDiscount("0.01");

        // When: 计算折扣后价格
        BigDecimal result = calculator.calculate("USER001", originalPrice, discount);

        // Then: 1 * 0.01 = 0.01（正好是最小值）
        log.info("【价格精度测试】原价={}, 折扣=0.01, 计算结果={}", originalPrice, result);

        assertThat(result)
            .as("1元打1折应该是0.01元")
            .isEqualByComparingTo(new BigDecimal("0.01"));
    }

    @Test
    @DisplayName("极限测试：999999.99元打9.9折 - 验证大数精度")
    void testLargeNumber() {
        // Given: 原价 999999.99 元，打 9.9 折（0.99）
        BigDecimal originalPrice = new BigDecimal("999999.99");
        Discount discount = createDiscount("0.99");

        // When: 计算折扣后价格
        BigDecimal result = calculator.calculate("USER001", originalPrice, discount);

        // Then: 999999.99 * 0.99 = 989999.9901 → 应该是 989999.99
        log.info("【价格精度测试】原价={}, 折扣=0.99, 计算结果={}", originalPrice, result);

        BigDecimal expected = new BigDecimal("989999.99");
        assertThat(result)
            .as("大额商品折扣计算应该保持精度")
            .isEqualByComparingTo(expected);
    }

    /**
     * 辅助方法：创建 Discount 对象
     */
    private Discount createDiscount(String percentage) {
        Discount discount = new Discount();
        discount.setDiscountId("DISCOUNT_TEST");
        discount.setMarketPlan("ZK");  // 百分比折扣
        discount.setMarketExpr(percentage);
        return discount;
    }
}
