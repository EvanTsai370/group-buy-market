package org.example.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.infrastructure.persistence.po.OrderPO;

import java.util.List;

/**
 * 订单Mapper
 */
@Mapper
public interface OrderMapper extends BaseMapper<OrderPO> {

    /**
     * 原子化增加锁单量（解决高并发超卖问题）
     *
     * SQL 逻辑：
     * 条件更新：status = 'PENDING' AND lock_count < target_count AND deadline_time > NOW()
     *
     * @param orderId 订单ID
     * @return 影响行数（1=成功，0=失败）
     */
    int incrementLockCount(@Param("orderId") String orderId);

    /**
     * 原子化减少锁单量（释放锁定）
     *
     * SQL 逻辑：
     * 条件更新：lock_count > 0
     *
     * @param orderId 订单ID
     * @return 影响行数（1=成功，0=失败）
     */
    int decrementLockCount(@Param("orderId") String orderId);

    /**
     * 原子化增加完成人数（解决高并发误杀问题）
     *
     * SQL 逻辑：
     * 1. 条件更新：status = 'PENDING' AND complete_count < target_count AND deadline_time > NOW()
     * 2. 如果 complete_count + 1 = target_count，同时更新 status = 'SUCCESS'
     *
     * @param orderId 订单ID
     * @return 影响行数（1=成功，0=失败）
     */
    int incrementCompleteCount(@Param("orderId") String orderId);

    /**
     * 查询超时未成团的订单
     * 查询条件：status = 'PENDING' AND deadline_time < NOW()
     */
    List<OrderPO> selectTimeoutOrders();

    /**
     * 查询可虚拟成团的订单
     */
    List<OrderPO> selectVirtualCompletableOrders();
}