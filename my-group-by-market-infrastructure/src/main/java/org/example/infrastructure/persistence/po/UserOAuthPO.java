package org.example.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * OAuth绑定持久化对象
 */
@Data
@TableName("user_oauth")
public class UserOAuthPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String userId;
    private String provider;
    private String providerUserId;
    private String unionId;
    private String accessToken;
    private String refreshToken;
    private LocalDateTime tokenExpireTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
