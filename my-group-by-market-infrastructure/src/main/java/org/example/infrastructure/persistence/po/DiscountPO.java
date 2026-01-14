// ============ 文件: DiscountPO.java ============
package org.example.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 折扣持久化对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("discount")
public class DiscountPO {

    @TableId(type = IdType.INPUT)
    private String discountId;
    private String discountName;
    private String discountDesc;
    private BigDecimal discountAmount;
    private String discountType;
    private String marketPlan;
    private String marketExpr;
    private String tagId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}