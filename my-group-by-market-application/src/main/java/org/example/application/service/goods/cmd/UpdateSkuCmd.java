package org.example.application.service.goods.cmd;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 更新 SKU 命令
 */
@Data
public class UpdateSkuCmd {
    private String goodsId;
    private String goodsName;
    private String specInfo;
    private BigDecimal originalPrice;
    private String skuImage;
}
