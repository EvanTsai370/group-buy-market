package org.example.interfaces.web.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建 SPU 请求
 */
@Data
public class CreateSpuRequest {
    @NotBlank(message = "商品名称不能为空")
    private String spuName;
    private String categoryId;
    private String brand;
    private String description;
    private String mainImage;
    private String detailImages;
}
