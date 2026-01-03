package org.example.domain.service.discount;

import lombok.extern.slf4j.Slf4j;
import org.example.domain.model.activity.Discount;
import org.example.domain.model.tag.repository.CrowdTagRepository;

import java.math.BigDecimal;

/**
 * 满减折扣计算器
 * 营销计划：MJ（满减）
 * 表达式格式："满足金额,减免金额"，如 "100,20" 表示满100减20
 */
@Slf4j
public class FullReductionDiscountCalculator extends AbstractDiscountCalculator {

    public FullReductionDiscountCalculator(CrowdTagRepository crowdTagRepository) {
        super(crowdTagRepository);
    }

    @Override
    protected BigDecimal doCalculate(BigDecimal originalPrice, Discount discount) {
        log.info("【满减折扣计算】原价: {}, 折扣表达式: {}", originalPrice, discount.getMarketExpr());

        // 折扣表达式 - 100,10 满100减10元
        String marketExpr = discount.getMarketExpr();
        String[] split = marketExpr.split(",");
        if (split.length != 2) {
            log.error("【满减折扣计算】表达式格式错误，应为 '满足金额,减免金额'，实际: {}", marketExpr);
            return originalPrice;
        }

        BigDecimal threshold = new BigDecimal(split[0].trim());
        BigDecimal reductionAmount = new BigDecimal(split[1].trim());

        // 不满足最低满减约束，则按照原价
        if (originalPrice.compareTo(threshold) < 0) {
            log.info("【满减折扣计算】不满足条件（原价 < {}），无折扣", threshold);
            return originalPrice;
        }

        // 折扣价格
        BigDecimal deductionPrice = originalPrice.subtract(reductionAmount);

        // 判断折扣后金额，最低支付1分钱
        if (deductionPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return new BigDecimal("0.01");
        }

        log.info("【满减折扣计算】计算完成，实付金额: {}", deductionPrice);
        return deductionPrice;
    }
}
