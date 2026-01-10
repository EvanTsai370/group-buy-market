package org.example.application.service.goods.cmd;

import lombok.Data;

/**
 * 更新 SPU 命令
 */
@Data
public class UpdateSpuCmd {
    private String spuId;
    private String spuName;
    private String categoryId;
    private String brand;
    private String description;
    private String mainImage;
    private String detailImages;
}
