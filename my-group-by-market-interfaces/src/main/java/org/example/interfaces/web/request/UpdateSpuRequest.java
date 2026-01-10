package org.example.interfaces.web.request;

import lombok.Data;

/**
 * 更新 SPU 请求
 */
@Data
public class UpdateSpuRequest {
    private String spuName;
    private String categoryId;
    private String brand;
    private String description;
    private String mainImage;
    private String detailImages;
}
