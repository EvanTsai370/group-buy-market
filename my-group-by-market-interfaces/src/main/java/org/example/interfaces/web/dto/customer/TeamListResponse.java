package org.example.interfaces.web.dto.customer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 拼团队伍响应（SPU维度）
 *
 * 注意：本系统采用SPU拼团模式，不同规格(SKU)的用户可以在同一队伍中一起拼团
 *
 */
@Data
@Schema(description = "拼团队伍信息")
public class TeamListResponse {

    @Schema(description = "拼团订单ID")
    private String orderId;

    @Schema(description = "商品SPU ID")
    private String spuId;

    @Schema(description = "商品SPU名称")
    private String spuName;

    @Schema(description = "当前参与人数")
    private Integer currentCount;

    @Schema(description = "目标人数")
    private Integer targetCount;

    @Schema(description = "剩余时间（秒）")
    private Long remainingSeconds;

    @Schema(description = "团长用户ID")
    private String leaderUserId;

    @Schema(description = "团长昵称")
    private String leaderNickname;

    @Schema(description = "团长头像")
    private String leaderAvatar;
}
