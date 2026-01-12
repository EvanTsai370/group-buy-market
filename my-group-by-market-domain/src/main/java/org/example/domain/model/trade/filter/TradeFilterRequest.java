package org.example.domain.model.trade.filter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 交易规则过滤请求对象
 *
 * <p>封装锁单前需要校验的参数信息
 *
 * @author 开发团队
 * @since 2026-01-04
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeFilterRequest {

    /** 用户ID */
    private String userId;

    /** 活动ID */
    private String activityId;

    /** 商品ID */
    private String skuId;

    /** 拼团订单ID（如果是加入已有队伍） */
    private String orderId;
}
