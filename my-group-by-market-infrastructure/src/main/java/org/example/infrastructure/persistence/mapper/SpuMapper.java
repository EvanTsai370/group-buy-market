package org.example.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.infrastructure.persistence.po.SpuPO;

import java.util.List;

/**
 * SPU Mapper
 */
@Mapper
public interface SpuMapper extends BaseMapper<SpuPO> {

    @Select("SELECT * FROM spu WHERE spu_id = #{spuId}")
    SpuPO selectBySpuId(@Param("spuId") String spuId);

    @Select("SELECT * FROM spu WHERE category_id = #{categoryId}")
    List<SpuPO> selectByCategoryId(@Param("categoryId") String categoryId);

    @Select("SELECT * FROM spu WHERE status = #{status}")
    List<SpuPO> selectByStatus(@Param("status") String status);

    @Select("SELECT * FROM spu WHERE status = 'ON_SALE' ORDER BY sort_order DESC")
    List<SpuPO> selectAllOnSale();
}
