package org.example.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对应数据库表 `t_user`
 */
@Data
@TableName("t_user")
public class UserPO {
    private Long id;
    private String username;
    private String pwd;   // 假设数据库字段叫 pwd，演示字段名不一致的转换
    private String email;
    private String city;  // 数据库里通常是扁平存储
    private String street;
    @TableField(fill = FieldFill.INSERT) // 插入时填充
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE) // 插入和更新时都填充
    private LocalDateTime updateTime;
}