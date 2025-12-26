package org.example.infrastructure.shared;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.example.domain.shared.IdGenerator;
import org.springframework.stereotype.Component;

/**
 * 基于 MyBatis-Plus 内置雪花算法的实现
 */
@Component
public class SnowflakeIdGenerator implements IdGenerator {

    @Override
    public Long nextId() {
        // 使用 MyBatis-Plus 提供的成熟雪花算法工具
        return IdWorker.getId();
    }
}