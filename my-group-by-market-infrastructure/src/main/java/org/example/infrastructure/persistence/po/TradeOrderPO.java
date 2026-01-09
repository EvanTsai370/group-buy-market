package org.example.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易订单持久化对象
 *
 * <p>
 * 对应数据库表：trade_order
 *
 * @author 开发团队
 * @since 2026-01-04
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("trade_order")
public class TradeOrderPO {

    /** 交易订单ID */
    @TableId
    private String tradeOrderId;

    /** 拼团队伍ID */
    private String teamId;

    /** 拼团订单ID */
    private String orderId;

    /** 活动ID */
    private String activityId;

    /** 用户ID */
    private String userId;

    /** 商品ID */
    private String goodsId;

    /** 商品名称 */
    private String goodsName;

    /** 原始价格 */
    private BigDecimal originalPrice;

    /** 减免金额 */
    private BigDecimal deductionPrice;

    /** 实付金额 */
    private BigDecimal payPrice;

    /** 交易状态 */
    private String status;

    /** 外部交易单号 */
    private String outTradeNo;

    /** 支付时间 */
    private LocalDateTime payTime;

    /** 结算时间 */
    private LocalDateTime settlementTime;

    /** 来源 */
    private String source;

    /** 渠道 */
    private String channel;

    /** 通知类型（HTTP/MQ） */
    private String notifyType;

    /** HTTP回调地址 */
    private String notifyUrl;

    /** MQ主题 */
    private String notifyMq;

    /** 通知状态 */
    private String notifyStatus;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 退款原因 */
    private String refundReason;

    /** 退款时间 */
    private LocalDateTime refundTime;
}
