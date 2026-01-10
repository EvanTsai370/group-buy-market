package org.example.interfaces.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import java.math.BigDecimal;

/**
 * 创建 SKU 请求
 */
@Data
public class CreateSkuRequest {
    private String spuId;

    @NotBlank(message = "商品名称不能为空")
    private String goodsName;

    private String specInfo;

    @NotNull(message = "价格不能为空")
    @Positive(message = "价格必须大于0")
    private BigDecimal originalPrice;

    @NotNull(message = "库存不能为空")
    @PositiveOrZero(message = "库存不能为负数")
    private Integer stock;

    private String skuImage;
}
