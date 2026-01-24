package org.example.application.service.trade.cmd;

import lombok.Builder;
import lombok.Data;

/**
 * 退款命令
 *
 * <p>
 * 表达"执行退款"的用例输入
 *
 */
@Data
@Builder
public class RefundCmd {

    /**
     * 交易订单ID
     */
    private String tradeOrderId;

    /**
     * 退款原因
     */
    private String reason;

    /**
     * 操作人
     */
    private String operator;

    /**
     * 客户端IP
     */
    private String clientIp;
}
