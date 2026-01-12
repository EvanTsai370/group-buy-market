package org.example.domain.model.trade.filter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.domain.model.activity.Activity;

/**
 * 交易规则过滤上下文对象
 *
 * <p>
 * 封装过滤链执行过程中需要的共享数据
 *
 * @author 开发团队
 * @since 2026-01-04
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeFilterContext {

    /** 活动信息（懒加载） */
    private Activity activity;

    /** 用户在该活动下的已参与次数（懒加载） */
    private Integer userParticipationCount;

    /** 名额恢复key（用于失败回滚时恢复Redis名额） */
    private String recoveryTeamSlotKey;

    /** 库存恢复商品ID（用于失败回滚时释放库存） */
    private String recoverySkuId;
}
