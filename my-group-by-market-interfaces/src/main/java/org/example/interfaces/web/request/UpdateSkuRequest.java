package org.example.interfaces.web.request;

import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

/**
 * 更新 SKU 请求
 */
@Data
public class UpdateSkuRequest {
    private String goodsName;
    private String specInfo;

    @Positive(message = "价格必须大于0")
    private BigDecimal originalPrice;

    private String skuImage;
}
