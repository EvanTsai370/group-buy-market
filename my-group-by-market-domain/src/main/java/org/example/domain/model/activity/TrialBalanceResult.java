package org.example.domain.model.activity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 试算结果值对象
 * 用于返回给用户展示拼团可获得的优惠信息
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TrialBalanceResult {

    /** 商品ID */
    private String skuId;

    /** 商品名称 */
    private String goodsName;

    /** 原价 */
    private BigDecimal originalPrice;

    /** 折扣金额 */
    private BigDecimal deductionAmount;

    /** 实付金额 */
    private BigDecimal payAmount;

    /** 拼团目标人数 */
    private Integer targetCount;

    /** 活动开始时间 */
    private LocalDateTime startTime;

    /** 活动结束时间 */
    private LocalDateTime endTime;

    /** 是否可见拼团活动 */
    private Boolean visible;

    /** 是否可参与拼团 */
    private Boolean participable;

    /** 活动ID */
    private String activityId;

    /** 活动名称 */
    private String activityName;
}
