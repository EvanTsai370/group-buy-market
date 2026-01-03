package org.example.domain.model.activity.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.domain.model.activity.Activity;
import org.example.domain.model.activity.Discount;
import org.example.domain.model.goods.Sku;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 拼团试算上下文（强类型）
 * 用于在流程节点间传递数据
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TrialBalanceContext {

    // ============ 核心业务数据 ============

    /** 拼团活动配置 */
    private Activity activity;

    /** 折扣配置 */
    private Discount discount;

    /** 商品信息 */
    private Sku sku;

    /** 折扣金额 */
    private BigDecimal deductionAmount;

    /** 实付金额 */
    private BigDecimal payAmount;

    /** 活动可见性（是否在人群标签范围内-可见） */
    private boolean visible;

    /** 活动可参与性（是否在人群标签范围内-可参与） */
    private boolean participable;

    // ============ 流程追踪数据 ============

    /** 已执行的节点列表（用于调试） */
    private List<String> executedNodes;

    /** 链路追踪ID */
    private String traceId;

    /**
     * 添加已执行节点
     */
    public void addExecutedNode(String nodeName) {
        if (executedNodes == null) {
            executedNodes = new ArrayList<>();
        }
        executedNodes.add(nodeName);
    }

    /**
     * 判断是否有活动配置
     */
    public boolean hasActivity() {
        return activity != null;
    }

    /**
     * 判断是否有折扣配置
     */
    public boolean hasDiscount() {
        return discount != null;
    }

    /**
     * 判断是否有商品信息
     */
    public boolean hasSku() {
        return sku != null;
    }

    /**
     * 判断是否已计算折扣
     */
    public boolean hasCalculatedDiscount() {
        return deductionAmount != null && payAmount != null;
    }
}
