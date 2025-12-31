// ============ 文件: ActivityMapper.java ============
package org.example.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.infrastructure.persistence.po.ActivityPO;

/**
 * 活动Mapper
 */
@Mapper
public interface ActivityMapper extends BaseMapper<ActivityPO> {

    /**
     * 根据来源和渠道查询活动
     */
    ActivityPO selectBySourceAndChannel(@Param("source") String source, 
                                        @Param("channel") String channel);
}