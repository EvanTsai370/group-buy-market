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
        log.info("【折扣计算】原价: {}, 折扣表达式: {}", LogDesensitizer.maskPrice(originalPrice, log), discount.getMarketExpr());

        // 折扣表达式 - 折扣百分比
        String marketExpr = discount.getMarketExpr();

        // 折扣价格 + 四舍五入
        BigDecimal deductionPrice = originalPrice.multiply(new BigDecimal(marketExpr))
                .setScale(0, RoundingMode.DOWN);

        // 判断折扣后金额，最低支付1分钱
        if (deductionPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return new BigDecimal("0.01");
        }

        log.info("【折扣计算】计算完成，实付金额: {}", LogDesensitizer.maskPrice(deductionPrice, log));
        return deductionPrice;
    }
}
