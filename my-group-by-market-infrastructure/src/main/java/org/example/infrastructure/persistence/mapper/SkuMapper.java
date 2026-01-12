package org.example.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.infrastructure.persistence.po.SkuPO;

import java.util.List;

/**
 * SKU Mapper
 */
@Mapper
public interface SkuMapper extends BaseMapper<SkuPO> {

    @Select("SELECT * FROM sku WHERE sku_id = #{skuId}")
    SkuPO selectBySkuId(@Param("skuId") String skuId);

    @Select("SELECT * FROM sku WHERE spu_id = #{spuId}")
    List<SkuPO> selectBySpuId(@Param("spuId") String spuId);

    @Select("SELECT * FROM sku WHERE status = #{status}")
    List<SkuPO> selectByStatus(@Param("status") String status);

    @Select("SELECT * FROM sku WHERE status = 'ON_SALE'")
    List<SkuPO> selectAllOnSale();

    /**
     * 原子冻结库存
     */
    @Update("UPDATE sku SET frozen_stock = frozen_stock + #{quantity}, update_time = NOW() " +
            "WHERE sku_id = #{skuId} AND (stock - frozen_stock) >= #{quantity}")
    int freezeStock(@Param("skuId") String skuId, @Param("quantity") int quantity);

    /**
     * 原子释放库存
     */
    @Update("UPDATE sku SET frozen_stock = GREATEST(0, frozen_stock - #{quantity}), update_time = NOW() " +
            "WHERE sku_id = #{skuId}")
    int unfreezeStock(@Param("skuId") String skuId, @Param("quantity") int quantity);

    /**
     * 原子扣减库存
     */
    @Update("UPDATE sku SET stock = stock - #{quantity}, frozen_stock = frozen_stock - #{quantity}, update_time = NOW() "
            +
            "WHERE sku_id = #{skuId} AND frozen_stock >= #{quantity}")
    int deductStock(@Param("skuId") String skuId, @Param("quantity") int quantity);
}