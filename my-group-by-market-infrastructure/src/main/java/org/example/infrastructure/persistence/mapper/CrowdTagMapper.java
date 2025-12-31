// ============ 文件: CrowdTagMapper.java ============
package org.example.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.infrastructure.persistence.po.CrowdTagPO;

/**
 * 人群标签Mapper
 */
@Mapper
public interface CrowdTagMapper extends BaseMapper<CrowdTagPO> {
}