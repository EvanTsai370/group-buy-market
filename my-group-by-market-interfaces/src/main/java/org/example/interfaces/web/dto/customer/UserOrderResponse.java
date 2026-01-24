package org.example.interfaces.web.dto.customer;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户订单列表响应
 * 
 */
@Data
public class UserOrderResponse {

    /** 交易订单ID */
    private String tradeOrderId;

    /** 外部交易单号 */
    private String outTradeNo;

    /** 商品ID */
    private String skuId;

    /** 商品名称 */
    private String goodsName;

    /** SPU名称 */
    private String spuName;

    /** SKU名称（规格） */
    private String skuName;

    /** 商品主图 */
    private String mainImage;

    /** 活动ID */
    private String activityId;

    /** 活动名称 */
    private String activityName;

    /** 拼团订单ID */
    private String orderId;

    /** 交易金额 */
    private BigDecimal tradeAmount;

    /** 支付价格 */
    private BigDecimal payPrice;

    /** 原价 */
    private BigDecimal originalPrice;

    /** 交易状态 */
    private String tradeStatus;

    /** 订单状态（用于前端显示） */
    private String status;

    /** 已完成人数 */
    private Integer completeCount;

    /** 目标人数 */
    private Integer targetCount;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 支付时间 */
    private LocalDateTime payTime;
}
