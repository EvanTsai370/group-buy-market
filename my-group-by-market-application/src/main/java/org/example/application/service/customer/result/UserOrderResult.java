package org.example.application.service.customer.result;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户订单列表项结果
 * 
 * @author 开发团队
 * @since 2026-01-11
 */
@Data
public class UserOrderResult {

    /** 交易订单ID */
    private String tradeOrderId;

    /** 外部交易单号 */
    private String outTradeNo;

    /** 商品ID */
    private String skuId;

    /** 商品名称 */
    private String goodsName;

    /** 活动ID */
    private String activityId;

    /** 活动名称 */
    private String activityName;

    /** 拼团订单ID */
    private String orderId;

    /** 交易金额 */
    private BigDecimal tradeAmount;

    /** 交易状态 */
    private String tradeStatus;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 支付时间 */
    private LocalDateTime payTime;
}
