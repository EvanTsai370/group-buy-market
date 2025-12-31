// ============ 文件: DiscountMapper.java ============
package org.example.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.infrastructure.persistence.po.DiscountPO;

/**
 * 折扣Mapper
 */
@Mapper
public interface DiscountMapper extends BaseMapper<DiscountPO> {
}