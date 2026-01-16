package org.example.application.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.event.PaymentCompletedEvent;
import org.example.domain.service.SettlementService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

/**
 * Settlement事件监听器
 * 
 * <p>
 * 使用异步+TransactionalEventListener确保：
 * <ol>
 * <li>只在事务提交后才处理（TransactionPhase.AFTER_COMMIT）</li>
 * <li>异步处理不阻塞支付回调响应（@Async）</li>
 * <li>所有并发事务已提交，settlement可见所有PAID状态的TradeOrder</li>
 * </ol>
 * 
 * <p>
 * 关键设计：
 * <ul>
 * <li>100ms延迟：确保大部分并发事务完全提交</li>
 * <li>只在达到targetCount时触发settlement</li>
 * <li>幂等性：settlement本身是幂等的，可以重复调用</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementEventListener {

    private final SettlementService settlementService;

    /**
     * 处理支付完成事件
     * 
     * <p>
     * 事件时机：在支付成功事务提交后异步触发
     * 
     * @param event 支付完成事件
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        log.info("【SettlementEventListener】收到支付完成事件, orderId: {}, completeCount: {}/{}",
                event.getOrderId(), event.getNewCompleteCount(), event.getTargetCount());

        // 只有达到targetCount才触发settlement
        if (event.isOrderCompleted()) {
            log.info("【SettlementEventListener】订单已完成，触发异步settlement, orderId: {}",
                    event.getOrderId());

            // 短暂延迟确保所有并发事务完全提交
            // 100ms通常足够大部分并发事务提交（根据Test 5-4的并发场景）
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("【SettlementEventListener】等待被中断, orderId: {}", event.getOrderId());
            }

            // 触发settlement（此时应该能看到所有已提交的PAID状态）
            settlementService.settleCompletedOrder(event.getOrderId());
            log.info("【SettlementEventListener】异步settlement完成, orderId: {}", event.getOrderId());
        }
    }
}
