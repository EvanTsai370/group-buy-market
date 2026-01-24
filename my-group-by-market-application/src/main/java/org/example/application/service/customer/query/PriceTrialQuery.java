package org.example.application.service.customer.query;

import lombok.Data;

/**
 * 价格试算查询对象
 * 
 */
@Data
public class PriceTrialQuery {

    /** 商品ID */
    private String skuId;

    /** 来源（APP/H5/PC） */
    private String source;

    /** 渠道 */
    private String channel;

    /** 用户ID（可选，用于个性化定价） */
    private String userId;
}
