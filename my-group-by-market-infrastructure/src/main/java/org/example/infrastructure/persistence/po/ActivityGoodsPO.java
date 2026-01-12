package org.example.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 活动商品关联持久化对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("activity_goods")
public class ActivityGoodsPO {

    /** 自增ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 活动ID */
    private String activityId;

    /** 商品ID */
    private String spuId;

    /** 来源 */
    private String source;

    /** 渠道 */
    private String channel;

    /** 折扣ID（可选，为空则使用活动默认折扣） */
    private String discountId;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
