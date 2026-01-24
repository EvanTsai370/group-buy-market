package org.example.application.service.customer.result;

import lombok.Data;

/**
 * 拼团队伍列表项结果
 *
 * 用于展示SPU维度的拼团队伍信息。
 * 注意：本系统采用SPU拼团模式，不同规格(SKU)的用户可以在同一队伍中一起拼团。
 *
 */
@Data
public class TeamListResult {

    /** 拼团订单ID */
    private String orderId;

    /** 商品SPU ID */
    private String spuId;

    /** 商品SPU名称 */
    private String spuName;

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
