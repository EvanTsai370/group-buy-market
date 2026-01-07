package org.example.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode implements IErrorCode {

    SUCCESS("00000", "操作成功"),

    // --- A类: 用户端错误 ---
    USER_ERROR("A0001", "用户端错误"),
    PARAM_ERROR("A0400", "请求参数非法"),
    UNAUTHORIZED("A0401", "用户未登录"),
    USER_NOT_EXIST("A0201", "用户不存在"),

    // --- B类: 业务执行错误 ---
    SYSTEM_ERROR("B0001", "系统执行出错"),
    BUSINESS_ERROR("B0002", "业务逻辑错误"),

    // --- B1类: 订单相关错误 ---
    ORDER_NOT_FOUND("B1001", "拼团订单不存在"),
    ORDER_ALREADY_FULL("B1002", "拼团已满"),
    ORDER_CLOSED("B1003", "拼团已结束"),
    ORDER_EXPIRED("B1004", "拼团已过期"),
    ORDER_JOIN_FAILED("B1005", "加入拼团失败"),
    ORDER_USER_ALREADY_JOINED("B1006", "您已参与该拼团"),

    // --- B2类: 账户相关错误 ---
    ACCOUNT_NOT_FOUND("B2001", "用户账户不存在"),
    ACCOUNT_INSUFFICIENT("B2002", "参团次数不足"),
    ACCOUNT_PARTICIPATION_LIMIT_REACHED("B2003", "您的参与次数已达上限（%d/%d）"),

    // --- B3类: 活动相关错误 ---
    ACTIVITY_NOT_FOUND("B3001", "活动不存在"),
    ACTIVITY_NOT_ACTIVE("B3002", "活动未开启，当前状态: %s"),
    ACTIVITY_NOT_STARTED("B3003", "活动未开始，开始时间: %s"),
    ACTIVITY_EXPIRED("B3004", "活动已结束，结束时间: %s"),
    ACTIVITY_CLOSED("B3005", "活动已关闭"),

    // --- C类: 第三方调用错误 ---
    THIRD_PARTY_ERROR("C0001", "第三方服务调用出错");

    private final String code;
    private final String msg;
}