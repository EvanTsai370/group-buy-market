// ============ 文件: SkuMapper.java ============
package org.example.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.infrastructure.persistence.po.SkuPO;

/**
 * SKU Mapper
 */
@Mapper
public interface SkuMapper extends BaseMapper<SkuPO> {
}