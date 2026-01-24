package org.example.domain.model.trade.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 拼团成功事件
 * 
 * <p>
 * 当拼团完成时发布此事件，用于触发后续处理：
 * 1. 库存扣减
 * 2. 订单通知
 * 3. 营销活动结算
 * </p>
 * 
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupCompleteEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 事件ID */
    private String eventId;

    /** 活动ID */
    private String activityId;

    /** 团ID */
    private String teamId;

    /** 团长用户ID */
    private String leaderUserId;

    /** 拼团成功时间 */
    private LocalDateTime completeTime;

    /** 参团信息列表 */
    private List<ParticipantInfo> participants;

    /**
     * 参团成员信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantInfo implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /** 订单ID */
        private String orderId;

        /** 用户ID */
        private String userId;

        /** SKU ID */
        private Long skuId;

        /** 数量 */
        private Integer quantity;

        /** 支付金额 */
        private BigDecimal payAmount;

        /** 参团时间 */
        private LocalDateTime joinTime;
    }

    /**
     * 创建拼团成功事件
     */
    public static GroupCompleteEvent create(String activityId, String teamId, String leaderUserId) {
        GroupCompleteEvent event = new GroupCompleteEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setActivityId(activityId);
        event.setTeamId(teamId);
        event.setLeaderUserId(leaderUserId);
        event.setCompleteTime(LocalDateTime.now());
        return event;
    }
}
