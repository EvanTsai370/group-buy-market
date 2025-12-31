package org.example.domain.model.goods;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * SKU（商品）
 * 这里简化为值对象，实际项目中可能是独立的聚合
 */
@Data
public class Sku {

    /** 商品ID */
    private String goodsId;

    /** 商品名称 */
    private String goodsName;

    /** 原价 */
    private BigDecimal originalPrice;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}