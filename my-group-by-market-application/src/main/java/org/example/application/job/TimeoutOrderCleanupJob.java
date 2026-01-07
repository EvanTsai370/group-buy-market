package org.example.application.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.model.order.Order;
import org.example.domain.model.order.repository.OrderRepository;
import org.example.domain.service.RefundService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 超时订单清理定时任务
 *
 * <p>职责：
 * <ul>
 *   <li>定时扫描超时未成团的拼团订单</li>
 *   <li>批量退款给已支付的用户</li>
 *   <li>释放锁定的库存名额</li>
 *   <li>更新订单状态为 FAILED</li>
 * </ul>
 *
 * <p>业务场景：
 * <ul>
 *   <li>拼团订单超过截止时间（deadlineTime）仍未达到目标人数</li>
 *   <li>需要自动退款，避免用户资金占用</li>
 *   <li>释放库存名额，让其他用户可以参与新的拼团</li>
 * </ul>
 *
 * <p>调度策略：
 * <ul>
 *   <li>每5分钟执行一次（可配置）</li>
 *   <li>初始延迟30秒，避免启动时资源竞争</li>
 *   <li>批量处理，每次最多处理100个订单</li>
 * </ul>
 *
 * <p>注意事项：
 * <ul>
 *   <li>幂等性：RefundService 内部会检查订单状态，防止重复退款</li>
 *   <li>事务处理：每个订单的退款是独立事务，失败不影响其他订单</li>
 *   <li>日志记录：详细记录扫描数量、成功数、失败数</li>
 * </ul>
 *
 * @author 开发团队
 * @since 2026-01-06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TimeoutOrderCleanupJob {

    private final OrderRepository orderRepository;
    private final RefundService refundService;

    /**
     * 定时扫描并清理超时订单
     *
     * <p>执行周期：每5分钟一次（300000ms）
     * <p>初始延迟：30秒（30000ms），避免启动时资源竞争
     * <p>批量大小：每次最多100个订单
     *
     * <p>执行逻辑：
     * <ol>
     *   <li>查询超时订单（status=PENDING && deadlineTime < NOW）</li>
     *   <li>提取订单ID列表</li>
     *   <li>调用 RefundService 批量退款</li>
     *   <li>记录执行结果（扫描数量、成功数、失败数）</li>
     * </ol>
     */
    @Scheduled(fixedRate = 300000, initialDelay = 30000)
    public void cleanupTimeoutOrders() {
        log.info("【超时订单清理】开始执行定时任务");

        try {
            // 1. 查询超时订单（Repository 层已实现过滤逻辑）
            List<Order> timeoutOrders = orderRepository.findTimeoutOrders();

            if (timeoutOrders.isEmpty()) {
                log.info("【超时订单清理】未发现超时订单，本次任务结束");
                return;
            }

            // 2. 提取订单ID列表
            List<String> orderIds = timeoutOrders.stream()
                    .map(Order::getOrderId)
                    .collect(Collectors.toList());

            log.info("【超时订单清理】发现超时订单, count={}, orderIds={}",
                    timeoutOrders.size(), orderIds);

            // 3. 批量退款（RefundService 内部会处理每个订单的独立事务）
            refundService.batchRefundTimeoutOrders(orderIds);

            log.info("【超时订单清理】批量退款完成, totalCount={}", orderIds.size());

        } catch (Exception e) {
            log.error("【超时订单清理】任务执行失败", e);
        }

        log.info("【超时订单清理】定时任务执行结束");
    }
}
