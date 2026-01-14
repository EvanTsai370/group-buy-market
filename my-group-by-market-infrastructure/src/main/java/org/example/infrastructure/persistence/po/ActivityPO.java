// ============ 文件: ActivityPO.java ============
package org.example.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 活动持久化对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("activity")
public class ActivityPO {

    /** 活动ID（业务主键） */
    @TableId(type = IdType.INPUT)
    private String activityId;

    /** 活动名称 */
    private String activityName;

    /** 活动描述 */
    private String activityDesc;

    /** 默认折扣ID */
    private String discountId;

    /** 人群标签ID */
    private String tagId;

    /** 人群标签作用域 */
    private String tagScope;

    /** 成团方式（0=虚拟，1=真实） */
    private Integer groupType;

    /** 目标人数 */
    private Integer target;

    /** 拼单有效时长（秒） */
    private Integer validTime;

    /** 用户参团次数限制 */
    private Integer participationLimit;

    /** 活动开始时间 */
    private LocalDateTime startTime;

    /** 活动结束时间 */
    private LocalDateTime endTime;

    /** 活动状态 */
    private String status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}