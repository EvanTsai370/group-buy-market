package org.example.interfaces.web.dto.customer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 拼团成员信息DTO
 *
 * 表示拼团订单中的单个成员信息
 * 注意：本系统采用SPU拼团模式，不同SKU的用户可以在同一队伍中一起拼团
 *
 */
@Data
@Schema(description = "拼团成员信息")
public class OrderMemberDTO {

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "购买的SKU ID")
    private String skuId;

    @Schema(description = "购买的SKU名称")
    private String skuName;

    @Schema(description = "交易状态（CREATE-已锁单, PAID-已支付, SETTLED-已结算）")
    private String status;

    @Schema(description = "加入时间")
    private LocalDateTime joinTime;

    @Schema(description = "是否团长")
    private Boolean isLeader;
}
