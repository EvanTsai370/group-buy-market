package org.example.domain.model.payment.repository;

import org.example.domain.model.payment.PaymentCallbackRecord;

import java.util.Optional;

/**
 * 支付回调记录仓储接口
 *
 * <p>
 * 职责：
 * <ul>
 * <li>支付回调记录的持久化</li>
 * <li>幂等性检查</li>
 * </ul>
 *
 * @author 开发团队
 * @since 2026-01-07
 */
public interface PaymentCallbackRecordRepository {

    /**
     * 保存支付回调记录
     *
     * @param record 支付回调记录
     */
    void save(PaymentCallbackRecord record);

    /**
     * 检查回调ID是否已存在（幂等性检查）
     *
     * @param callbackId 回调ID
     * @return true=已存在，false=不存在
     */
    boolean existsByCallbackId(String callbackId);

    /**
     * 根据回调ID查询记录
     *
     * @param callbackId 回调ID
     * @return 支付回调记录
     */
    Optional<PaymentCallbackRecord> findByCallbackId(String callbackId);
}
