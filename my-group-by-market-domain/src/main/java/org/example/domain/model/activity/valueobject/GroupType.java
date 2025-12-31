package org.example.domain.model.activity.valueobject;

import lombok.Getter;

/**
 * 成团方式（值对象）
 * 
 * 业务含义：
 * - VIRTUAL（虚拟成团）：活动到期后自动成团，无论是否达到目标人数
 * - REAL（真实成团）：必须达到目标人数才能成团，否则失败退款
 * 
 * 设计理由：
 * 1. 封装业务规则：不同的成团方式有不同的业务逻辑
 * 2. 类型安全：避免使用魔法数字（0/1），提高代码可读性
 * 3. 易于扩展：未来如果有新的成团方式，只需添加枚举值
 */
@Getter
public enum GroupType {

    /**
     * 虚拟成团
     * - 运营策略：优先保证成交率，降低用户流失
     * - 业务规则：活动到期后自动成团，无论是否达到目标人数
     * - 适用场景：新品推广、库存清仓、用户增长活动
     */
    VIRTUAL(0, "虚拟成团", "活动到期自动成团"),

    /**
     * 真实成团
     * - 运营策略：保证拼团的真实性和用户体验
     * - 业务规则：必须达到目标人数才能成团，否则失败退款
     * - 适用场景：正常的拼团营销活动
     */
    REAL(1, "真实成团", "必须达到目标人数");

    /**
     * 数据库存储的code值
     */
    private final Integer code;

    /**
     * 显示名称
     */
    private final String name;

    /**
     * 详细描述
     */
    private final String description;

    /**
     * 构造函数
     */
    GroupType(Integer code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    /**
     * 根据code值获取枚举
     * 用于从数据库读取时的类型转换
     *
     * @param code 数据库中的code值（0或1）
     * @return 对应的枚举
     * @throws IllegalArgumentException 如果code值无效
     */
    public static GroupType fromCode(Integer code) {
        if (code == null) {
            throw new IllegalArgumentException("成团方式code不能为空");
        }

        for (GroupType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }

        throw new IllegalArgumentException("未知的成团方式: " + code);
    }

    /**
     * 是否为虚拟成团
     */
    public boolean isVirtual() {
        return this == VIRTUAL;
    }

    /**
     * 是否为真实成团
     */
    public boolean isReal() {
        return this == REAL;
    }

    /**
     * 业务规则：判断是否需要严格检查目标人数
     * 
     * @return true=需要严格检查，false=不需要
     */
    public boolean requiresStrictTargetCheck() {
        return this == REAL;
    }

    /**
     * 业务规则：到期时是否自动成团
     * 
     * @return true=自动成团，false=检查人数后决定
     */
    public boolean shouldAutoCompleteOnExpiry() {
        return this == VIRTUAL;
    }
}