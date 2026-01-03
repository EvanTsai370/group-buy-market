package org.example.domain.service.discount;

import lombok.extern.slf4j.Slf4j;
import org.example.domain.model.activity.Discount;
import org.example.domain.model.activity.valueobject.DiscountType;
import org.example.domain.model.tag.repository.CrowdTagRepository;

import java.math.BigDecimal;

/**
 * 折扣计算器抽象类
 * 提供人群标签过滤的通用逻辑
 */
@Slf4j
public abstract class AbstractDiscountCalculator implements DiscountCalculator {

    protected final CrowdTagRepository crowdTagRepository;

    protected AbstractDiscountCalculator(CrowdTagRepository crowdTagRepository) {
        this.crowdTagRepository = crowdTagRepository;
    }

    @Override
    public BigDecimal calculate(String userId, BigDecimal originalPrice, Discount discount) {
        // 1. 人群标签过滤
        if (DiscountType.TAG.equals(discount.getDiscountType())) {
            boolean isInCrowdRange = filterByTag(userId, discount.getTagId());
            if (!isInCrowdRange) {
                log.info("【折扣计算】用户不在优惠人群标签范围内，userId: {}, tagId: {}",
                         userId, discount.getTagId());
                return originalPrice;
            }
        }

        // 2. 折扣优惠计算
        return doCalculate(originalPrice, discount);
    }

    /**
     * 人群过滤 - 限定人群优惠
     */
    private boolean filterByTag(String userId, String tagId) {
        if (tagId == null || tagId.isEmpty()) {
            return false;
        }
        Boolean result = crowdTagRepository.checkUserInTag(userId, tagId);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 执行具体的折扣计算逻辑（子类实现）
     *
     * @param originalPrice 原价
     * @param discount 折扣配置
     * @return 实付金额
     */
    protected abstract BigDecimal doCalculate(BigDecimal originalPrice, Discount discount);
}
