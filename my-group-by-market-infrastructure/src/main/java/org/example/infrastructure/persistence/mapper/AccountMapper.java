// ============ 文件: AccountMapper.java ============
package org.example.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.infrastructure.persistence.po.AccountPO;

/**
 * 账户Mapper
 */
@Mapper
public interface AccountMapper extends BaseMapper<AccountPO> {

    /**
     * 根据用户和活动查询账户
     */
    AccountPO selectByUserAndActivity(@Param("userId") String userId,
                                      @Param("activityId") String activityId);
}