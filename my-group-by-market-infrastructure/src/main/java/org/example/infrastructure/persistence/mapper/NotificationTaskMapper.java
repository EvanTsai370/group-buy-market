package org.example.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.infrastructure.persistence.po.NotificationTaskPO;

import java.util.List;

/**
 * 通知任务Mapper
 *
 * @author 开发团队
 * @since 2026-01-06
 */
@Mapper
public interface NotificationTaskMapper extends BaseMapper<NotificationTaskPO> {

    /**
     * 根据交易订单ID查询
     *
     * @param tradeOrderId 交易订单ID
     * @return 通知任务列表
     */
    List<NotificationTaskPO> selectByTradeOrderId(@Param("tradeOrderId") String tradeOrderId);

    /**
     * 查询待处理任务
     *
     * @param limit 限制数量
     * @return 待处理任务列表
     */
    List<NotificationTaskPO> selectPendingTasks(@Param("limit") int limit);

    /**
     * 查询待处理任务（分页）
     *
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 待处理任务列表
     */
    List<NotificationTaskPO> selectPendingTasksWithPage(@Param("offset") int offset, @Param("limit") int limit);
}
