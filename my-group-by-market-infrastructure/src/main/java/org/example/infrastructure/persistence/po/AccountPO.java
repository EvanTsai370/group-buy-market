// ============ 文件: AccountPO.java ============
package org.example.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 账户持久化对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("account")
public class AccountPO {

    @TableId(type = IdType.INPUT)
    private String accountId;
    private String userId;
    private String activityId;
    private Integer participationCount;

    /** 乐观锁版本号 */
    @Version
    private Long version;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}