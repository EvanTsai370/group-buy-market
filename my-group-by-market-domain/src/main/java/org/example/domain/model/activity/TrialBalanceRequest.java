package org.example.domain.model.activity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 试算请求值对象
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TrialBalanceRequest {

    /** 活动ID（可选，如果不传则根据商品ID查询） */
    private String activityId;

    /** 用户ID */
    private String userId;

    /** 商品ID */
    private String goodsId;

    /** 来源（如APP、小程序） */
    private String source;

    /** 渠道（如抖音、淘宝） */
    private String channel;

    /** 链路追踪ID */
    private String traceId;
}
