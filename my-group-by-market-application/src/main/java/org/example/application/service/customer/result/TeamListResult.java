package org.example.application.service.customer.result;

import lombok.Data;

/**
 * 拼团队伍列表项结果
 * 
 * @author 开发团队
 * @since 2026-01-11
 */
@Data
public class TeamListResult {

    /** 拼团订单ID */
    private String orderId;

    /** 当前参与人数 */
    private Integer currentCount;

    /** 目标人数 */
    private Integer targetCount;

    /** 剩余时间（秒） */
    private Long remainingSeconds;

    /** 团长用户ID */
    private String leaderUserId;

    /** 团长昵称 */
    private String leaderNickname;

    /** 团长头像 */
    private String leaderAvatar;
}
