// ============ 文件: domain/shared/DomainEvent.java ============
package org.example.domain.shared;

import java.time.LocalDateTime;

/**
 * 领域事件接口
 * 
 * 设计目的：
 * 1. Domain 层：所有领域事件都实现此接口，统一事件契约
 * 2. Infrastructure 层：事件发布器（EventPublisher）依赖此接口进行事件发布
 * 3. Application 层：事件监听器（EventListener）依赖此接口进行事件消费
 * 
 * 为什么放在 shared 包：
 * - 这是 Domain 层对外暴露的基础设施接口
 * - Infrastructure 层的 EventPublisher 需要依赖它
 * - 体现"依赖倒置"：外层依赖内层的接口，而不是具体实现
 */
public interface DomainEvent {

    /**
     * 事件发生时间
     * 用于事件溯源、审计、排序等场景
     */
    LocalDateTime occurredOn();

    /**
     * 事件类型
     * 用于事件路由、过滤、监控等场景
     * 
     * 建议格式：聚合名.事件名，如 "Order.Created"
     */
    String eventType();

    /**
     * 事件版本（可选）
     * 用于事件演化和兼容性处理
     */
    default int version() {
        return 1;
    }

    /**
     * 聚合根ID（可选）
     * 用于事件溯源时重建聚合根
     */
    default String aggregateId() {
        return null;
    }
}