package org.example.application.service.goods.cmd;

import lombok.Data;

/**
 * 创建 SPU 命令
 */
@Data
public class CreateSpuCmd {
    private String spuName;
    private String categoryId;
    private String brand;
    private String description;
    private String mainImage;
    private String detailImages;
}
