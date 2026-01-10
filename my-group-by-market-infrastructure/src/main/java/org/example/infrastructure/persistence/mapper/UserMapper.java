package org.example.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.infrastructure.persistence.po.UserPO;

/**
 * 用户 Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<UserPO> {

    @Select("SELECT * FROM user WHERE user_id = #{userId}")
    UserPO selectByUserId(@Param("userId") String userId);

    @Select("SELECT * FROM user WHERE username = #{username}")
    UserPO selectByUsername(@Param("username") String username);

    @Select("SELECT * FROM user WHERE phone = #{phone}")
    UserPO selectByPhone(@Param("phone") String phone);

    @Select("SELECT * FROM user WHERE email = #{email}")
    UserPO selectByEmail(@Param("email") String email);

    @Select("SELECT COUNT(1) > 0 FROM user WHERE username = #{username}")
    boolean existsByUsername(@Param("username") String username);

    @Select("SELECT COUNT(1) > 0 FROM user WHERE phone = #{phone}")
    boolean existsByPhone(@Param("phone") String phone);

    @Select("SELECT COUNT(1) > 0 FROM user WHERE email = #{email}")
    boolean existsByEmail(@Param("email") String email);
}
