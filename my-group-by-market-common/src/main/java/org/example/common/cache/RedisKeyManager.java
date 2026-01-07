package org.example.common.cache;

/**
 * Redis Key 管理器
 *
 * <p>
 * 职责：
 * <ul>
 * <li>集中管理所有Redis Key的生成逻辑</li>
 * <li>避免魔法字符串分散在代码各处</li>
 * <li>确保Key格式的一致性</li>
 * <li>便于统一修改和维护</li>
 * </ul>
 *
 * <p>
 * 设计原则：
 * <ul>
 * <li>使用静态方法，无需实例化</li>
 * <li>Key格式统一：{业务模块}:{业务对象}:{ID}:{字段}</li>
 * <li>支持嵌套调用，提高代码可读性</li>
 * </ul>
 *
 * @author 开发团队
 * @since 2026-01-07
 */
public class RedisKeyManager {

    // ==================== 拼团库存相关 ====================

    /**
     * 拼团库存Key前缀
     */
    private static final String TEAM_STOCK_PREFIX = "team_stock";

    /**
     * 可用库存后缀
     */
    private static final String AVAILABLE_SUFFIX = "available";

    /**
     * 已锁定量后缀
     */
    private static final String LOCKED_SUFFIX = "locked";

    /**
     * 生成拼团库存的基础Key
     *
     * <p>
     * 格式：team_stock:{orderId}
     *
     * @param orderId 拼团订单ID
     * @return 拼团库存基础Key
     */
    public static String teamStockKey(String orderId) {
        return TEAM_STOCK_PREFIX + ":" + orderId;
    }

    /**
     * 生成拼团可用库存Key
     *
     * <p>
     * 格式：team_stock:{orderId}:available
     *
     * @param orderId 拼团订单ID
     * @return 可用库存Key
     */
    public static String teamStockAvailableKey(String orderId) {
        return teamStockKey(orderId) + ":" + AVAILABLE_SUFFIX;
    }

    /**
     * 生成拼团已锁定量Key
     *
     * <p>
     * 格式：team_stock:{orderId}:locked
     *
     * @param orderId 拼团订单ID
     * @return 已锁定量Key
     */
    public static String teamStockLockedKey(String orderId) {
        return teamStockKey(orderId) + ":" + LOCKED_SUFFIX;
    }

    // ==================== 人群标签相关 ====================

    /**
     * 人群标签Key前缀
     */
    private static final String CROWD_TAG_PREFIX = "crowd_tag";

    /**
     * 生成人群标签用户集合Key
     *
     * <p>
     * 格式：crowd_tag:{tagId}:users
     *
     * @param tagId 标签ID
     * @return 人群标签用户集合Key
     */
    public static String crowdTagUsersKey(String tagId) {
        return CROWD_TAG_PREFIX + ":" + tagId + ":users";
    }

    // ==================== 分布式锁相关 ====================

    /**
     * 分布式锁Key前缀
     */
    private static final String LOCK_PREFIX = "lock";

    /**
     * 生成分布式锁Key
     *
     * <p>
     * 格式：lock:{业务类型}:{业务ID}
     *
     * @param businessType 业务类型（如：order, account）
     * @param businessId   业务ID
     * @return 分布式锁Key
     */
    public static String lockKey(String businessType, String businessId) {
        return LOCK_PREFIX + ":" + businessType + ":" + businessId;
    }

    // ==================== 工具方法 ====================

    /**
     * 从完整的库存Key中提取orderId
     *
     * <p>
     * 用于回滚场景，从teamStockKey反向解析orderId
     *
     * @param teamStockKey 完整的库存Key（如：team_stock:ORDER123）
     * @return orderId
     */
    public static String extractOrderIdFromTeamStockKey(String teamStockKey) {
        if (teamStockKey == null || !teamStockKey.startsWith(TEAM_STOCK_PREFIX + ":")) {
            throw new IllegalArgumentException("Invalid team stock key: " + teamStockKey);
        }
        return teamStockKey.substring((TEAM_STOCK_PREFIX + ":").length());
    }

    /**
     * 私有构造函数，防止实例化
     */
    private RedisKeyManager() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
