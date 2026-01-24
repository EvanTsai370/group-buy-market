package org.example.domain.model.trade.filter;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.example.common.cache.RedisKeyManager;
import org.example.common.exception.BizException;
import org.example.common.pattern.chain.model2.IChainHandler;
import org.example.domain.model.trade.repository.TradeOrderRepository;

/**
 * 组队名额占用规则处理器
 *
 * <p>
 * 职责：
 * <ul>
 * <li>在高并发场景下,防止拼团组队超卖</li>
 * <li>通过Redis原子操作实现分布式名额控制</li>
 * <li>支持退款恢复机制（增加可用名额）</li>
 * </ul>
 *
 * <p>
 * 业务逻辑：
 * <ol>
 * <li>如果orderId为空(首次开团),不做名额限制,直接放行</li>
 * <li>如果orderId不为空(加入已有团),通过Redis名额扣减抢占名额</li>
 * <li>使用Redis DECR原子操作扣减可用名额</li>
 * <li>如果扣减后 &lt; 0，回滚并返回失败</li>
 * </ol>
 *
 * <p>
 * 技术要点（名额扣减模式）：
 * <ul>
 * <li>Redis DECR：原子扣减名额,保证并发安全</li>
 * <li>语义清晰：available 直接代表剩余名额</li>
 * <li>易于监控：运维可以直接查看剩余名额</li>
 * <li>恢复机制：退款时 INCR available,释放名额</li>
 * <li>延时过期：validTime + 60分钟,便于排查问题</li>
 * </ul>
 *
 * <p>
 * Redis Key设计：
 * 
 * <pre>
 * 可用名额: team_slot:{orderId}:available  - 剩余可用名额
 * 已锁定量: team_slot:{orderId}:locked     - 累计锁定次数（审计用）
 * </pre>
 *
 * <p>
 * 示例场景：
 * 
 * <pre>
 * 5人团，初始状态：
 *   available = 5, locked = 0
 *
 * 用户A加入：
 *   DECR available → 4
 *   INCR locked → 1
 *   结果：成功，剩余4个名额
 *
 * ...3个用户加入后：
 *   available = 1, locked = 4
 *
 * 用户E加入：
 *   DECR available → 0
 *   INCR locked → 5
 *   结果：成功，团满
 *
 * 用户F尝试加入：
 *   DECR available → -1
 *   检测到 &lt; 0，回滚（INCR available → 0）
 *   结果：失败
 *
 * 用户E退款：
 *   INCR available → 1
 *   结果：available = 1, locked = 5（审计显示总共5人锁定过）
 *
 * 用户G加入：
 *   DECR available → 0
 *   INCR locked → 6
 *   结果：成功（使用了退款释放的名额）
 * </pre>
 *
 */
@Slf4j
public class TeamSlotOccupyHandler
        implements IChainHandler<TradeFilterRequest, TradeFilterContext, TradeFilterResponse> {

    private final TradeOrderRepository tradeOrderRepository;

    public TeamSlotOccupyHandler(TradeOrderRepository tradeOrderRepository) {
        this.tradeOrderRepository = tradeOrderRepository;
    }

    @Override
    public TradeFilterResponse handle(TradeFilterRequest request, TradeFilterContext context) throws Exception {
        log.info("【交易规则过滤-组队名额校验】userId: {}, activityId: {}, orderId: {}",
                request.getUserId(), request.getActivityId(), request.getOrderId());

        // 1. orderId为空,则为首次开团,不做拼团组队目标量名额限制
        String orderId = request.getOrderId();
        if (StringUtils.isBlank(orderId)) {
            log.info("【交易规则过滤-组队名额校验】首次开团,不做名额限制, userId: {}", request.getUserId());
            return TradeFilterResponse.allow();
        }

        // 2. 从上下文获取活动信息
        if (context.getActivity() == null) {
            throw new BizException("活动信息未加载");
        }

        Integer target = context.getActivity().getTarget();
        Integer validTime = context.getActivity().getValidTime();

        // 3. 生成Redis Key
        // 注意：这里使用orderId作为teamId的替代,因为在我们的设计中orderId就是拼团订单ID
        String teamSlotKey = generateTeamSlotKey(orderId);

        // 4. 抢占名额:通过Redis名额扣减,来降低对数据库的操作压力
        boolean success = tradeOrderRepository.occupyTeamSlot(
                teamSlotKey,
                target,
                validTime);

        if (!success) {
            log.warn("【交易规则过滤-组队名额校验】抢占失败, userId: {}, activityId: {}, orderId: {}, teamSlotKey: {}",
                    request.getUserId(), request.getActivityId(), orderId, teamSlotKey);
            return TradeFilterResponse.reject("拼团已满,请选择其他拼团或发起新团");
        }

        // 5. 抢占成功,将名额key放入上下文,用于后续失败回滚
        context.setRecoveryTeamSlotKey(teamSlotKey);

        log.info("【交易规则过滤-组队名额校验】抢占成功, userId: {}, orderId: {}, teamSlotKey: {}",
                request.getUserId(), orderId, teamSlotKey);

        return TradeFilterResponse.allow();
    }

    /**
     * 生成组队名额key
     *
     * @param orderId 拼团订单ID
     * @return Redis key
     */
    private String generateTeamSlotKey(String orderId) {
        return RedisKeyManager.teamSlotKey(orderId);
    }
}
