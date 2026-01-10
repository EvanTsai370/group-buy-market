package org.example.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;
import org.example.infrastructure.persistence.po.UserOAuthPO;

import java.util.List;

/**
 * OAuth绑定 Mapper
 */
@Mapper
public interface UserOAuthMapper extends BaseMapper<UserOAuthPO> {

    @Select("SELECT * FROM user_oauth WHERE provider = #{provider} AND provider_user_id = #{providerUserId}")
    UserOAuthPO selectByProviderAndProviderUserId(
            @Param("provider") String provider,
            @Param("providerUserId") String providerUserId);

    @Select("SELECT * FROM user_oauth WHERE user_id = #{userId}")
    List<UserOAuthPO> selectByUserId(@Param("userId") String userId);

    @Select("SELECT * FROM user_oauth WHERE user_id = #{userId} AND provider = #{provider}")
    UserOAuthPO selectByUserIdAndProvider(
            @Param("userId") String userId,
            @Param("provider") String provider);

    @Select("SELECT COUNT(1) > 0 FROM user_oauth WHERE provider = #{provider} AND provider_user_id = #{providerUserId}")
    boolean existsByProviderAndProviderUserId(
            @Param("provider") String provider,
            @Param("providerUserId") String providerUserId);

    @Delete("DELETE FROM user_oauth WHERE user_id = #{userId} AND provider = #{provider}")
    void deleteByUserIdAndProvider(
            @Param("userId") String userId,
            @Param("provider") String provider);
}
