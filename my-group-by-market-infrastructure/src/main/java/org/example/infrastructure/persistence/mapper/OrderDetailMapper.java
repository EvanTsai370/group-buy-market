// ============ 文件: OrderDetailMapper.java ============
package org.example.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.infrastructure.persistence.po.OrderDetailPO;

import java.util.List;

/**
 * 订单明细Mapper
 */
@Mapper
public interface OrderDetailMapper extends BaseMapper<OrderDetailPO> {

    /**
     * 根据订单ID查询所有明细
     */
    List<OrderDetailPO> selectByOrderId(@Param("orderId") String orderId);
}