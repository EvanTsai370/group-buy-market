package org.example.domain.service.discount;

import lombok.extern.slf4j.Slf4j;
import org.example.common.util.LogDesensitizer;
import org.example.domain.model.activity.Discount;
import org.example.domain.model.tag.repository.CrowdTagRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 折扣优惠计算器
 * 营销计划：ZK（折扣）
 * 表达式格式：折扣百分比，如 "0.8" 表示8折
 */
@Slf4j
public class PercentageDiscountCalculator extends AbstractDiscountCalculator {

    public PercentageDiscountCalculator(CrowdTagRepository crowdTagRepository) {
        super(crowdTagRepository);
    }

    @Override
    protected BigDecimal doCalculate(BigDecimal originalPrice, Discount discount) {
        log.info("【百分比折扣计算】原价: {}, 折扣表达式: {}", LogDesensitizer.maskPrice(originalPrice, log), discount.getMarketExpr());

        // 折扣表达式 - 百分比
        String marketExpr = discount.getMarketExpr();

        // 解析百分比，处理非法表达式
        BigDecimal percentage;
        try {
            percentage = new BigDecimal(marketExpr);
        } catch (NumberFormatException e) {
            log.error("【百分比折扣计算】折扣表达式格式错误: {}, 返回原价", marketExpr, e);
            return originalPrice; // 优雅降级
        }

        // 百分比折扣
        BigDecimal discountedPrice = originalPrice.multiply(percentage)
                .setScale(2, RoundingMode.DOWN);

        // 最小值保护：折扣后价格不能低于 0.01 元
        if (discountedPrice.compareTo(new BigDecimal("0.01")) < 0) {
            return new BigDecimal("0.01");
        }

        log.info("【百分比折扣计算】计算完成，实付金额: {}", LogDesensitizer.maskPrice(discountedPrice, log));
        return discountedPrice;
    }
}
