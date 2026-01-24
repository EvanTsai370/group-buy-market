package org.example.application.result;

import lombok.Data;
import org.example.domain.model.trade.valueobject.TradeStatus;

import java.time.LocalDateTime;

/**
 * 拼团成员信息结果
 *
 * 表示拼团订单中的单个成员信息,包括:
 * - 用户基础信息(ID、昵称、头像)
 * - 购买的商品(SKU ID和名称)
 * - 交易状态(CREATE/PAID/SETTLED/TIMEOUT/REFUND)
 * - 加入时间和是否团长
 *
 * 注意:本系统采用SPU拼团模式,不同SKU的用户可以在同一队伍中一起拼团
 *
 */
@Data
public class OrderMemberResult {

    /** 用户ID */
    private String userId;

    /** 昵称 */
    private String nickname;

    /** 头像URL */
    private String avatar;

    /** 购买的SKU ID */
    private String skuId;

    /** 购买的SKU名称(含规格信息) */
    private String skuName;

    /** 交易状态 CREATE-已锁单, PAID-已支付, SETTLED-已结算, TIMEOUT-已超时, REFUND-已退款 */
    private TradeStatus status;

    /** 加入时间(锁单时间) */
    private LocalDateTime joinTime;

    /** 是否团长(计算字段) */
    private Boolean isLeader;
}
