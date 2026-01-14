package org.example.domain.service.discount;

import lombok.extern.slf4j.Slf4j;
import org.example.common.util.LogDesensitizer;
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
        log.info("【N元购折扣计算】原价: {}, 折扣表达式: {}", LogDesensitizer.maskPrice(originalPrice, log), discount.getMarketExpr());

        // 折扣表达式 - 直接为优惠后的金额
        String marketExpr = discount.getMarketExpr();

        // 解析固定价格，处理非法表达式
        BigDecimal fixedPrice;
        try {
            fixedPrice = new BigDecimal(marketExpr);
        } catch (NumberFormatException e) {
            log.error("【N元购折扣计算】折扣表达式格式错误: {}, 返回原价", marketExpr, e);
            return originalPrice; // 优雅降级
        }

        // 验证固定价格不超过原价（保护用户）
        if (fixedPrice.compareTo(originalPrice) > 0) {
            log.warn("【N元购折扣计算】固定价格({})超过原价({}), 返回原价",
                    LogDesensitizer.maskPrice(fixedPrice, log),
                    LogDesensitizer.maskPrice(originalPrice, log));
            return originalPrice; // 优雅降级
        }

        // 验证固定价格为正数
        if (fixedPrice.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("【N元购折扣计算】固定价格({})为负数或零, 返回原价",
                    LogDesensitizer.maskPrice(fixedPrice, log));
            return originalPrice; // 优雅降级
        }

        log.info("【N元购折扣计算】计算完成，实付金额: {}", LogDesensitizer.maskPrice(fixedPrice, log));
        return fixedPrice;
    }
}
