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
 */
public class RedisKeyManager {

    // ==================== 拼团名额相关 ====================

    /**
     * 拼团名额Key前缀
     */
    private static final String TEAM_SLOT_PREFIX = "team_slot";

    /**
     * 可用名额后缀
     */
    private static final String AVAILABLE_SUFFIX = "available";

    /**
     * 已锁定量后缀
     */
    private static final String LOCKED_SUFFIX = "locked";

    /**
     * 生成拼团名额的基础Key
     *
     * <p>
     * 格式：team_slot:{orderId}
     *
     * @param orderId 拼团订单ID
     * @return 拼团名额基础Key
     */
    public static String teamSlotKey(String orderId) {
        return TEAM_SLOT_PREFIX + ":" + orderId;
    }

    /**
     * 生成拼团可用名额Key
     *
     * <p>
     * 格式：team_slot:{orderId}:available
     *
     * @param orderId 拼团订单ID
     * @return 可用名额Key
     */
    public static String teamSlotAvailableKey(String orderId) {
        return teamSlotKey(orderId) + ":" + AVAILABLE_SUFFIX;
    }

    /**
     * 生成拼团已锁定量Key
     *
     * <p>
     * 格式：team_slot:{orderId}:locked
     *
     * @param orderId 拼团订单ID
     * @return 已锁定量Key
     */
    public static String teamSlotLockedKey(String orderId) {
        return teamSlotKey(orderId) + ":" + LOCKED_SUFFIX;
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
     * 从完整的名额Key中提取orderId
     *
     * <p>
     * 用于回滚场景，从teamSlotKey反向解析orderId
     *
     * @param teamSlotKey 完整的名额Key（如：team_slot:ORDER123）
     * @return orderId
     */
    public static String extractOrderIdFromTeamSlotKey(String teamSlotKey) {
        if (teamSlotKey == null || !teamSlotKey.startsWith(TEAM_SLOT_PREFIX + ":")) {
            throw new IllegalArgumentException("Invalid team slot key: " + teamSlotKey);
        }
        return teamSlotKey.substring((TEAM_SLOT_PREFIX + ":").length());
    }

    /**
     * 私有构造函数，防止实例化
     */
    private RedisKeyManager() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
