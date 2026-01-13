package org.example.application.result;

import lombok.Data;
import org.example.domain.model.order.valueobject.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 拼团进度查询结果
 *
 * 用于展示拼团订单的实时进度信息,包括:
 * - 基础信息(订单ID、活动、商品)
 * - 进度信息(目标人数、完成人数、锁定人数、剩余时间)
 * - 成员列表(所有参与用户的详情)
 *
 * @author 开发团队
 * @since 2026-01-13
 */
@Data
public class OrderProgressResult {

    /** 拼团订单ID */
    private String orderId;

    /** 队伍ID(用户友好的短ID,用于分享链接) */
    private String teamId;

    /** 活动ID */
    private String activityId;

    /** 活动名称 */
    private String activityName;

    /** 商品SPU ID */
    private String spuId;

    /** 商品SPU名称 */
    private String spuName;

    /** 拼团状态 */
    private OrderStatus status;

    /** 目标人数 */
    private Integer targetCount;

    /** 已完成人数(已支付) */
    private Integer completeCount;

    /** 已锁定人数(含未支付) */
    private Integer lockCount;

    /** 还差多少人成团(计算字段) */
    private Integer remainingCount;

    /** 进度百分比 0-100 (计算字段) */
    private Integer progress;

    /** 团长用户ID */
    private String leaderUserId;

    /** 团长昵称 */
    private String leaderNickname;

    /** 团长头像 */
    private String leaderAvatar;

    /** 截止时间 */
    private LocalDateTime deadline;

    /** 剩余秒数(计算字段) */
    private Long remainingSeconds;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 成团时间(SUCCESS时有值) */
    private LocalDateTime completedTime;

    /** 成员列表 */
    private List<OrderMemberResult> members;
}
