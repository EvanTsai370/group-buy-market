package org.example.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.infrastructure.persistence.po.TradeOrderPO;

import java.util.List;

/**
 * 交易订单Mapper
 *
 * <p>
 * SQL实现方式：
 * <ul>
 * <li>所有SQL语句都在TradeOrderMapper.xml中定义</li>
 * <li>保持项目风格一致性（其他Mapper如OrderMapper也使用XML）</li>
 * <li>便于添加详细的业务注释和性能说明</li>
 * </ul>
 *
 * @author 开发团队
 * @since 2026-01-04
 */
@Mapper
public interface TradeOrderMapper extends BaseMapper<TradeOrderPO> {

    /**
     * 根据外部交易单号查询
     *
     * <p>
     * 用于幂等性校验，防止同一笔订单重复提交
     *
     * @param outTradeNo 外部交易单号
     * @return 交易订单PO
     */
    TradeOrderPO selectByOutTradeNo(@Param("outTradeNo") String outTradeNo);

    /**
     * 根据用户ID和活动ID查询交易订单列表
     *
     * <p>
     * 用于查询用户在某个活动下的所有参与记录
     *
     * @param userId     用户ID
     * @param activityId 活动ID
     * @return 交易订单列表
     */
    List<TradeOrderPO> selectByUserIdAndActivityId(@Param("userId") String userId,
            @Param("activityId") String activityId);

    /**
     * 根据队伍ID查询交易订单列表
     *
     * <p>
     * 用于查询某个拼团队伍的所有成员订单
     *
     * @param teamId 队伍ID
     * @return 交易订单列表
     */
    List<TradeOrderPO> selectByTeamId(@Param("teamId") String teamId);

    /**
     * 根据订单ID查询交易订单列表
     *
     * <p>
     * 用于查询某个拼团订单下的所有交易订单
     *
     * @param orderId 拼团订单ID
     * @return 交易订单列表
     */
    List<TradeOrderPO> selectByOrderId(@Param("orderId") String orderId);

    /**
     * 统计用户在某个活动下的参与次数
     *
     * <p>
     * 用于限制用户参与次数，防止恶意刷单
     *
     * @param userId     用户ID
     * @param activityId 活动ID
     * @return 参与次数
     */
    int countByUserIdAndActivityId(@Param("userId") String userId,
            @Param("activityId") String activityId);

    /**
     * 根据用户ID分页查询交易订单列表
     *
     * <p>
     * 用于用户中心订单列表展示
     *
     * @param userId 用户ID
     * @param offset 偏移量
     * @param limit  数量
     * @return 交易订单列表
     */
    List<TradeOrderPO> selectByUserId(@Param("userId") String userId,
            @Param("offset") int offset,
            @Param("limit") int limit);
}
