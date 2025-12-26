package org.example.domain.model.user.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 领域事件：用户已创建
 * 命名建议：名词 + 动词过去式 (User Created)
 */
@Getter
@ToString
@AllArgsConstructor
public class UserCreatedEvent {
    
    private Long userId;
    private String email;
    private LocalDateTime occurredOn; // 事件发生时间

    // 辅助构造器
    public UserCreatedEvent(Long userId, String email) {
        this.userId = userId;
        this.email = email;
        this.occurredOn = LocalDateTime.now();
    }
}