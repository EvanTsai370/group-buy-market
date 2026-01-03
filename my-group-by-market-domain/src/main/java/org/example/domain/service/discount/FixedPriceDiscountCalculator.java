package org.example.domain.service.discount;

import lombok.extern.slf4j.Slf4j;
import org.example.domain.model.activity.Discount;
import org.example.domain.model.tag.repository.CrowdTagRepository;

import java.math.BigDecimal;

/**
 * N元购折扣计算器
 * 营销计划：N（N元购）
 * 表达式格式：固定价格，如 "9.9" 表示9.9元购买
 */
@Slf4j
public class FixedPriceDiscountCalculator extends AbstractDiscountCalculator {

    public FixedPriceDiscountCalculator(CrowdTagRepository crowdTagRepository) {
        super(crowdTagRepository);
    }

    @Override
    protected BigDecimal doCalculate(BigDecimal originalPrice, Discount discount) {
        log.info("【N元购折扣计算】原价: {}, 折扣表达式: {}", originalPrice, discount.getMarketExpr());

        // 折扣表达式 - 直接为优惠后的金额
        String marketExpr = discount.getMarketExpr();

        // n元购
        BigDecimal fixedPrice = new BigDecimal(marketExpr);

        log.info("【N元购折扣计算】计算完成，实付金额: {}", fixedPrice);
        return fixedPrice;
    }
}
