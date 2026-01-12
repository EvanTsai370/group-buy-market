package org.example.application.service.customer.result;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品列表项结果
 * 
 * @author 开发团队
 * @since 2026-01-11
 */
@Data
public class GoodsListResult {

    /** 商品ID */
    private String skuId;

    /** 商品名称 */
    private String goodsName;

    /** 原价 */
    private BigDecimal originalPrice;

    /** 拼团价（如有活动） */
    private BigDecimal groupPrice;

    /** 主图 */
    private String mainImage;

    /** 是否有拼团活动 */
    private Boolean hasActivity;

    /** 活动ID（如有活动） */
    private String activityId;

    /** 可用库存 */
    private Integer availableStock;
}
