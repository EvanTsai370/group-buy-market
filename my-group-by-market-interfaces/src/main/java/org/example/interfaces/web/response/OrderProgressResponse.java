package org.example.interfaces.web.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.example.interfaces.web.dto.customer.OrderMemberDTO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 拼团进度响应
 *
 * 用于展示拼团订单的实时进度信息
 *
 * @author 开发团队
 * @since 2026-01-13
 */
@Data
@Schema(description = "拼团进度响应")
public class OrderProgressResponse {

    @Schema(description = "拼团订单ID")
    private String orderId;

    @Schema(description = "队伍ID（用户友好的短ID）")
    private String teamId;

    @Schema(description = "活动ID")
    private String activityId;

    @Schema(description = "活动名称")
    private String activityName;

    @Schema(description = "商品SPU ID")
    private String spuId;

    @Schema(description = "商品SPU名称")
    private String spuName;

    @Schema(description = "拼团状态（PENDING-进行中, SUCCESS-已成团, FAILED-失败）")
    private String status;

    @Schema(description = "目标人数")
    private Integer targetCount;

    @Schema(description = "已完成人数（已支付）")
    private Integer completeCount;

    @Schema(description = "已锁定人数（含未支付）")
    private Integer lockCount;

    @Schema(description = "还差多少人成团")
    private Integer remainingCount;

    @Schema(description = "进度百分比（0-100）")
    private Integer progress;

    @Schema(description = "团长用户ID")
    private String leaderUserId;

    @Schema(description = "团长昵称")
    private String leaderNickname;

    @Schema(description = "团长头像")
    private String leaderAvatar;

    @Schema(description = "截止时间")
    private LocalDateTime deadline;

    @Schema(description = "剩余秒数")
    private Long remainingSeconds;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "成团时间（SUCCESS时有值）")
    private LocalDateTime completedTime;

    @Schema(description = "成员列表")
    private List<OrderMemberDTO> members;
}
