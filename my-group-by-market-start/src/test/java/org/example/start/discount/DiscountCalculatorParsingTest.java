package org.example.start.discount;

import lombok.extern.slf4j.Slf4j;
import org.example.start.base.IntegrationTestBase;
import org.example.domain.model.activity.Discount;
import org.example.domain.service.discount.FixedPriceDiscountCalculator;
import org.example.domain.service.discount.PercentageDiscountCalculator;
import org.example.domain.service.discount.DirectDiscountCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test 14: 折扣计算器非法表达式处理测试
 *
 * <p>
 * 测试目的：发现折扣计算器在解析非法表达式时的崩溃 Bug
 * <p>
 * 场景：测试各种非法的 marketExpr 输入
 * <p>
 * 预期（TDD - 先写失败测试）：
 * - marketExpr = "abc" → 返回原价（优雅降级）
 * - marketExpr = "" → 返回原价
 * - marketExpr = null → 返回原价
 * - marketExpr = "10.5.5" → 返回原价
 * - marketExpr = "-10" → 返回原价（负数保护）
 * <p>
 * 可能发现的 Bug：
 * - NumberFormatException 未捕获 → 整个锁单流程崩溃
 * - 业务逻辑：解析失败时应记录日志并返回原价（优雅降级）
 *
 */
@Slf4j
@DisplayName("Test 14: 折扣计算器非法表达式测试（发现解析崩溃 Bug）")
public class DiscountCalculatorParsingTest extends IntegrationTestBase {

    @Autowired
    private PercentageDiscountCalculator percentageCalculator;

    @Autowired
    private FixedPriceDiscountCalculator fixedPriceCalculator;

    @Autowired
    private DirectDiscountCalculator directCalculator;

    @Test
    @DisplayName("Bug验证：百分比折扣 - 非数字字符串应返回原价而非崩溃")
    void testPercentageCalculator_InvalidExpression_NonNumericString() {
        // Given: 原价 100 元，折扣表达式为非法字符串 "abc"
        BigDecimal originalPrice = new BigDecimal("100.00");
        Discount discount = createDiscount("ZK", "abc");

        // When: 计算折扣后价格
        // 【预期失败】如果代码未捕获 NumberFormatException，这里会抛异常
        BigDecimal result = percentageCalculator.calculate("USER001", originalPrice, discount);

        // Then: 应该返回原价（优雅降级）
        log.info("【非法表达式测试】百分比折扣 - 原价={}, 表达式=abc, 计算结果={}", originalPrice, result);

        assertThat(result)
                .as("非法表达式应返回原价，而不是抛出 NumberFormatException")
                .isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("Bug验证：固定价格 - 非数字字符串应返回原价而非崩溃")
    void testFixedPriceCalculator_InvalidExpression_NonNumericString() {
        // Given: 原价 100 元，固定价表达式为非法字符串 "xyz"
        BigDecimal originalPrice = new BigDecimal("100.00");
        Discount discount = createDiscount("N", "xyz");

        // When: 计算折扣后价格
        BigDecimal result = fixedPriceCalculator.calculate("USER001", originalPrice, discount);

        // Then: 应该返回原价
        log.info("【非法表达式测试】固定价格 - 原价={}, 表达式=xyz, 计算结果={}", originalPrice, result);

        assertThat(result)
                .as("非法表达式应返回原价")
                .isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("Bug验证：直减折扣 - 非数字字符串应返回原价而非崩溃")
    void testDirectCalculator_InvalidExpression_NonNumericString() {
        // Given: 原价 100 元，直减表达式为非法字符串 "invalid"
        BigDecimal originalPrice = new BigDecimal("100.00");
        Discount discount = createDiscount("ZJ", "invalid");

        // When: 计算折扣后价格
        BigDecimal result = directCalculator.calculate("USER001", originalPrice, discount);

        // Then: 应该返回原价
        log.info("【非法表达式测试】直减折扣 - 原价={}, 表达式=invalid, 计算结果={}", originalPrice, result);

        assertThat(result)
                .as("非法表达式应返回原价")
                .isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("边界值测试：空字符串应返回原价")
    void testEmptyExpression() {
        // Given: 原价 100 元，折扣表达式为空字符串
        BigDecimal originalPrice = new BigDecimal("100.00");
        Discount discount = createDiscount("ZK", "");

        // When: 计算折扣后价格
        BigDecimal result = percentageCalculator.calculate("USER001", originalPrice, discount);

        // Then: 应该返回原价
        log.info("【非法表达式测试】空字符串 - 原价={}, 表达式='', 计算结果={}", originalPrice, result);

        assertThat(result)
                .as("空字符串应返回原价")
                .isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("边界值测试：格式错误的小数应返回原价")
    void testMalformedDecimal() {
        // Given: 原价 100 元，折扣表达式为格式错误的小数 "10.5.5"
        BigDecimal originalPrice = new BigDecimal("100.00");
        Discount discount = createDiscount("N", "10.5.5");

        // When: 计算折扣后价格
        BigDecimal result = fixedPriceCalculator.calculate("USER001", originalPrice, discount);

        // Then: 应该返回原价
        log.info("【非法表达式测试】格式错误小数 - 原价={}, 表达式=10.5.5, 计算结果={}", originalPrice, result);

        assertThat(result)
                .as("格式错误的小数应返回原价")
                .isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("边界值测试：特殊字符应返回原价")
    void testSpecialCharacters() {
        // Given: 原价 100 元，折扣表达式包含特殊字符
        BigDecimal originalPrice = new BigDecimal("100.00");
        Discount discount = createDiscount("ZK", "0.8@#$");

        // When: 计算折扣后价格
        BigDecimal result = percentageCalculator.calculate("USER001", originalPrice, discount);

        // Then: 应该返回原价
        log.info("【非法表达式测试】特殊字符 - 原价={}, 表达式=0.8@#$, 计算结果={}", originalPrice, result);

        assertThat(result)
                .as("包含特殊字符的表达式应返回原价")
                .isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("边界值测试：负数表达式应返回原价")
    void testNegativeExpression() {
        // Given: 原价 100 元，折扣表达式为负数 "-10"
        BigDecimal originalPrice = new BigDecimal("100.00");
        Discount discount = createDiscount("N", "-10");

        // When: 计算折扣后价格
        BigDecimal result = fixedPriceCalculator.calculate("USER001", originalPrice, discount);

        // Then: 应该返回原价（负数价格无意义）
        log.info("【非法表达式测试】负数 - 原价={}, 表达式=-10, 计算结果={}", originalPrice, result);

        assertThat(result)
                .as("负数表达式应返回原价")
                .isEqualByComparingTo(new BigDecimal("100.00"));
    }

    /**
     * 辅助方法：创建 Discount 对象
     */
    private Discount createDiscount(String marketPlan, String marketExpr) {
        Discount discount = new Discount();
        discount.setDiscountId("DISCOUNT_TEST");
        discount.setMarketPlan(marketPlan);
        discount.setMarketExpr(marketExpr);
        return discount;
    }
}
