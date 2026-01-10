package org.example.application.service.goods.cmd;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 创建 SKU 命令
 */
@Data
public class CreateSkuCmd {
    private String spuId;
    private String goodsName;
    private String specInfo;
    private BigDecimal originalPrice;
    private Integer stock;
    private String skuImage;
}
