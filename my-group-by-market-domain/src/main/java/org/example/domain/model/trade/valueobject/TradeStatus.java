package org.example.domain.model.trade.valueobject;

import lombok.Getter;

/**
 * 交易状态枚举
 *
 * <p>状态流转说明：
 * <pre>
 * CREATE（已创建/锁单）
 *    ↓ 用户支付成功
 * PAID（已支付）
 *    ↓ 拼团成功
 * SETTLED（已结算）
 *
 * CREATE（已创建）
 *    ↓ 超时未支付 或 支付失败
 * TIMEOUT / REFUND（超时/退单）
 * </pre>
 *
 */
@Getter
public enum TradeStatus {

    /**
     * 已创建（锁单）
     * <p>用户下单时创建交易订单，锁定优惠名额
     */
    CREATE("CREATE", "已创建"),

    /**
     * 已支付
     * <p>用户支付成功，等待拼团结果
     */
    PAID("PAID", "已支付"),

    /**
     * 已结算
     * <p>拼团成功，订单结算完成
     */
    SETTLED("SETTLED", "已结算"),

    /**
     * 已超时
     * <p>用户下单后超时未支付，释放锁定的名额
     */
    TIMEOUT("TIMEOUT", "已超时"),

    /**
     * 已退单
     * <p>支付失败或用户主动取消，释放锁定的名额
     */
    REFUND("REFUND", "已退单");

    private final String code;
    private final String desc;

    TradeStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

  /**
     * 根据code获取枚举
     *
     * @param code 状态码
     * @return 交易状态枚举
     */
    public static TradeStatus fromCode(String code) {
        for (TradeStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的交易状态: " + code);
    }

    /**
     * 判断是否可以退单
     *
     * @return true=可以退单, false=不可以退单
     */
    public boolean canRefund() {
        return this == CREATE || this == PAID;
    }

    /**
     * 判断是否是终态（不可再变更）
     *
     * @return true=终态, false=非终态
     */
    public boolean isFinal() {
        return this == SETTLED || this == TIMEOUT || this == REFUND;
    }
}
