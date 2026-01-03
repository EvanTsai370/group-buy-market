package org.example.domain.service.discount;

import lombok.extern.slf4j.Slf4j;
import org.example.domain.model.activity.Discount;
import org.example.domain.model.tag.repository.CrowdTagRepository;

import java.math.BigDecimal;

/**
 * 直减折扣计算器
 * 营销计划：ZJ（直减）
 * 表达式格式：直接折扣金额，如 "10" 表示减10元
 */
@Slf4j
public class DirectDiscountCalculator extends AbstractDiscountCalculator {

    public DirectDiscountCalculator(CrowdTagRepository crowdTagRepository) {
        super(crowdTagRepository);
    }

    @Override
    protected BigDecimal doCalculate(BigDecimal originalPrice, Discount discount) {
        log.info("【直减折扣计算】原价: {}, 折扣表达式: {}", originalPrice, discount.getMarketExpr());

        // 折扣表达式 - 直减为扣减金额
        String marketExpr = discount.getMarketExpr();

        // 折扣价格
        BigDecimal deductionPrice = originalPrice.subtract(new BigDecimal(marketExpr));

        // 判断折扣后金额，最低支付1分钱
        if (deductionPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return new BigDecimal("0.01");
        }

        log.info("【直减折扣计算】计算完成，实付金额: {}", deductionPrice);
        return deductionPrice;
    }
}
