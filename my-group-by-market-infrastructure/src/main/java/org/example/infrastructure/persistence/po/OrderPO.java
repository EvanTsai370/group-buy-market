// ============ 文件: OrderPO.java ============
package org.example.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单持久化对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("`order`") // order 是 MySQL 关键字，需要加反引号
public class OrderPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderId;
    private String activityId;
    private String spuId;
    private BigDecimal originalPrice;
    private BigDecimal deductionPrice;
    private Integer targetCount;
    private Integer completeCount;
    private Integer lockCount;
    private String teamId;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime deadlineTime;
    private LocalDateTime completedTime;
    private String leaderUserId;
    private BigDecimal payAmount;
    private String source;
    private String channel;
    private String notifyUrl;

    /** 乐观锁版本号 */
    @Version
    private Long version;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}