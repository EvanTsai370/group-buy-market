package org.example.domain.model.trade.repository;

import org.example.domain.model.trade.TradeOrder;

import java.util.List;
import java.util.Optional;

/**
 * 交易订单仓储接口
 *
 * <p>职责：
 * <ul>
 *   <li>交易订单的持久化操作</li>
 *   <li>查询交易订单信息</li>
 *   <li>不包含业务逻辑，只负责数据访问</li>
 * </ul>
 *
 * <p>设计原则：
 * <ul>
 *   <li>接口定义在Domain层（遵循DDD的依赖倒置原则）</li>
 *   <li>实现在Infrastructure层</li>
 *   <li>Repository负责PO和Domain对象的转换</li>
 * </ul>
 *
 * @author 开发团队
 * @since 2026-01-04
 */
public interface TradeOrderRepository {

    /**
     * 保存交易订单（新增）
     *
     * <p>注意：此方法用于创建新的交易订单（锁单）
     *
     * @param tradeOrder 交易订单
     */
    void save(TradeOrder tradeOrder);

    /**
     * 更新交易订单
     *
     * <p>注意：更新状态、支付时间、结算时间等字段
     *
     * @param tradeOrder 交易订单
     */
    void update(TradeOrder tradeOrder);

    /**
     * 根据交易订单ID查询
     *
     * @param tradeOrderId 交易订单ID
     * @return 交易订单（如果不存在则返回empty）
     */
    Optional<TradeOrder> findByTradeOrderId(String tradeOrderId);

    /**
     * 根据外部交易单号查询（幂等性校验）
     *
     * <p>用途：防止重复锁单
     *
     * @param outTradeNo 外部交易单号
     * @return 交易订单（如果不存在则返回empty）
     */
    Optional<TradeOrder> findByOutTradeNo(String outTradeNo);

    /**
     * 根据用户ID和活动ID查询交易订单列表
     *
     * <p>用途：查询用户在某个活动下的所有交易记录
     *
     * @param userId 用户ID
     * @param activityId 活动ID
     * @return 交易订单列表
     */
    List<TradeOrder> findByUserIdAndActivityId(String userId, String activityId);

    /**
     * 根据队伍ID查询交易订单列表
     *
     * <p>用途：查询某个拼团队伍的所有交易记录
     *
     * @param teamId 队伍ID
     * @return 交易订单列表
     */
    List<TradeOrder> findByTeamId(String teamId);

    /**
     * 根据订单ID查询交易订单列表
     *
     * <p>用途：查询某个拼团订单关联的所有交易记录
     *
     * @param orderId 拼团订单ID
     * @return 交易订单列表
     */
    List<TradeOrder> findByOrderId(String orderId);

    /**
     * 统计用户在某个活动下的参与次数
     *
     * <p>用途：校验用户是否超过参与次数限制
     *
     * @param userId 用户ID
     * @param activityId 活动ID
     * @return 参与次数
     */
    int countByUserIdAndActivityId(String userId, String activityId);

    /**
     * 占用组队库存（Redis库存扣减模式）
     *
     * <p>业务场景：
     * <ul>
     *   <li>用户加入已有拼团时,需要抢占库存位</li>
     *   <li>通过Redis DECR原子操作扣减库存,保证并发安全</li>
     *   <li>支持退款恢复机制（退款时增加可用库存）</li>
     * </ul>
     *
     * <p>实现逻辑（库存扣减模式）：
     * <ol>
     *   <li>初始化：available = target（可用库存 = 目标人数）</li>
     *   <li>占用时：DECR available，判断是否 &lt; 0</li>
     *   <li>如果 &lt; 0，回滚（INCR available）并返回失败</li>
     *   <li>成功则增加 locked 计数器（用于审计）</li>
     * </ol>
     *
     * <p>Redis Key设计：
     * <pre>
     * 可用库存: team_stock:{orderId}:available  - 剩余可用名额（可增可减）
     * 已锁定量: team_stock:{orderId}:locked     - 累计锁定次数（只增不减，用于审计）
     * </pre>
     *
     * <p>优势：
     * <ul>
     *   <li>语义清晰：available 直接代表剩余库存</li>
     *   <li>易于监控：运维可以直接查看 available 值</li>
     *   <li>简单可靠：不需要双计数器配合</li>
     * </ul>
     *
     * @param teamStockKey 组队库存key（例如：team_stock:order123）
     * @param target 目标人数
     * @param validTime 有效时间（分钟）
     * @return true=抢占成功，false=抢占失败（库存不足）
     */
    boolean occupyTeamStock(String teamStockKey, Integer target, Integer validTime);

    /**
     * 恢复组队库存（退款场景）
     *
     * <p>业务场景：
     * <ul>
     *   <li>用户退款时,需要释放已占用的库存</li>
     *   <li>通过增加可用库存,让其他用户可以继续加入</li>
     * </ul>
     *
     * <p>实现逻辑（库存扣减模式）：
     * <pre>
     * Redis INCR {teamStockKey}:available
     * </pre>
     *
     * @param teamStockKey 组队库存key（例如：team_stock:order123）
     * @param validTime 有效时间（分钟）
     */
    void recoveryTeamStock(String teamStockKey, Integer validTime);
}
