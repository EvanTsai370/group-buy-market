// ============ 文件: OrderDetailPO.java ============
package org.example.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单明细持久化对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("order_detail")
public class OrderDetailPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String detailId;
    private String orderId;
    private String activityId;
    private String userId;
    private String userType;
    private String goodsId;
    private BigDecimal payAmount;
    private String outTradeNo;
    private String source;
    private String channel;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}