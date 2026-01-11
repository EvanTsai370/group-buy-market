package org.example.interfaces.web.dto.customer;

import lombok.Data;

import java.math.BigDecimal;

/**
 * C端商品列表响应
 * 
 * @author 开发团队
 * @since 2026-01-11
 */
@Data
public class CustomerGoodsListResponse {

    /** 商品ID */
    private String goodsId;

    /** 商品名称 */
    private String goodsName;

    /** 原价 */
    private BigDecimal originalPrice;

    /** 拼团价 */
    private BigDecimal groupPrice;

    /** 主图 */
    private String mainImage;

    /** 是否有拼团活动 */
    private Boolean hasActivity;

    /** 活动ID */
    private String activityId;

    /** 可用库存 */
    private Integer availableStock;
}
