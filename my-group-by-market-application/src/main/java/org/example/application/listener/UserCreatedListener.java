package org.example.application.listener;

import lombok.extern.slf4j.Slf4j;
import org.example.domain.model.user.event.UserCreatedEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class UserCreatedListener {

    /**
     * @Async("commonExecutor"): 告诉 Spring 不要用主线程跑，
     * 而是去 "commonExecutor" 线程池里拿一个线程来跑。
     */
    @Async("commonExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserCreated(UserCreatedEvent event) {
        log.info("==> 开始异步发送邮件, 当前线程: {}", Thread.currentThread().getName());
        
        try {
            // 模拟耗时操作 (例如调用第三方邮件服务)
            Thread.sleep(2000); 
            log.info("邮件发送内容: 欢迎用户 {}", event.getUserId());
        } catch (InterruptedException e) {
            log.error("邮件发送中断", e);
        }
        
        log.info("<== 邮件发送完成");
    }
}