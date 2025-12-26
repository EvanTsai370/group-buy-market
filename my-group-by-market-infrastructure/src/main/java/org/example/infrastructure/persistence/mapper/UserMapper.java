package org.example.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.example.infrastructure.persistence.po.UserPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus Mapper 接口
 * 职责：直接与数据库交互，执行 SQL
 * * 1. 继承 BaseMapper<UserPO>：自动获得 CRUD 能力 (insert, selectById, update等)
 * 2. 泛型指定为 UserPO：表示这个 Mapper 专门操作 t_user 表对应的 PO 对象
 */
@Mapper
public interface UserMapper extends BaseMapper<UserPO> {
    
    // 如果 BaseMapper 提供的通用方法不够用
    // 你可以在这里定义自定义 SQL 方法
    // 例如：根据邮箱查找用户
    // UserPO selectByEmail(@Param("email") String email);
}