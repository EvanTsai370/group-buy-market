package org.example.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * SKU持久化对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sku")
public class SkuPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String skuId;
    private String spuId;
    private String goodsName;
    private String specInfo;
    private BigDecimal originalPrice;
    private Integer stock;
    private Integer frozenStock;
    private String skuImage;
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}