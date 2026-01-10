package org.example.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * SPU 持久化对象
 */
@Data
@TableName("spu")
public class SpuPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String spuId;
    private String spuName;
    private String categoryId;
    private String brand;
    private String description;
    private String mainImage;
    private String detailImages;
    private String status;
    private Integer sortOrder;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
