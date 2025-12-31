package org.example.domain.model.order.repository;

import org.example.domain.model.order.Order;
import org.example.domain.model.order.OrderDetail;
import org.example.domain.model.order.valueobject.OrderStatus;

import java.util.List;
import java.util.Optional;

/**
 * Order 仓储接口
 * 定义在 Domain 层，实现在 Infrastructure 层
 */
public interface OrderRepository {

    /**
     * 保存拼团订单
     *
     * @param order 订单聚合
     */
    void save(Order order);

    /**
     * 根据ID查找订单
     *
     * @param orderId 订单ID
     * @return 订单聚合
     */
    Optional<Order> findById(String orderId);

    /**
     * 原子化增加完成人数（解决高并发误杀问题）
     *
     * 使用条件更新 SQL：
     * UPDATE `order` SET complete_count = complete_count + 1, status = ...
     * WHERE order_id = ? AND status = 'PENDING'
     *   AND complete_count < target_count AND deadline_time > NOW()
     *
     * 设计说明：
     * - 通过数据库层面的条件更新保证并发安全
     * - 避免乐观锁在高并发下的误杀问题
     * - 如果更新成功且达到目标人数，同时更新状态为 SUCCESS
     *
     * @param orderId 订单ID
     * @return 更新结果：成功返回更新后的完成人数，失败返回 -1
     */
    int tryIncrementCompleteCount(String orderId);

    /**
     * 保存订单明细（独立保存，不走聚合根）
     *
     * 配合 tryIncrementCompleteCount 使用，在原子更新成功后保存明细
     *
     * @param orderId 订单ID
     * @param detail 订单明细
     */
    void saveDetail(String orderId, OrderDetail detail);

    /**
     * 根据活动ID查找进行中的拼团
     *
     * @param activityId 活动ID
     * @return 订单列表
     */
    List<Order> findPendingOrdersByActivity(String activityId);

    /**
     * 查找超时未成团的拼单
     *
     * @return 订单列表
     */
    List<Order> findTimeoutOrders();

    /**
     * 查找可以虚拟成团的拼单
     *
     * @return 订单列表
     */
    List<Order> findVirtualCompletableOrders();

    /**
     * 更新订单状态
     *
     * @param orderId 订单ID
     * @param status 新状态
     */
    void updateStatus(String orderId, OrderStatus status);

    /**
     * 生成下一个订单ID
     *
     * @return 订单ID
     */
    String nextId();
}