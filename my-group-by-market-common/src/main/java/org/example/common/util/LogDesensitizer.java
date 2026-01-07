package org.example.common.util;

import org.slf4j.Logger;

import java.math.BigDecimal;

/**
 * 日志脱敏工具类
 *
 * <p>
 * 用于脱敏日志中的敏感信息，防止信息泄露
 *
 * <p>
 * 使用场景：
 * <ul>
 * <li>价格信息（payPrice、originalPrice、deductionPrice）</li>
 * <li>其他敏感业务数据</li>
 * </ul>
 *
 * @author 开发团队
 * @since 2026-01-07
 */
public class LogDesensitizer {

    /** 价格脱敏占位符 */
    private static final String PRICE_MASK = "**.**";

    /**
     * 脱敏价格信息
     *
     * <p>
     * 脱敏策略：
     * <ul>
     * <li>DEBUG 级别：返回原始价格（便于开发调试）</li>
     * <li>INFO 及以上级别：返回 "**.**"（保护生产环境敏感信息）</li>
     * </ul>
     *
     * @param price  价格
     * @param logger 日志对象（用于判断日志级别）
     * @return 脱敏后的价格字符串
     */
    public static String maskPrice(BigDecimal price, Logger logger) {
        if (price == null) {
            return "null";
        }

        // DEBUG 级别显示完整价格，便于开发调试
        if (logger.isDebugEnabled()) {
            return price.toString();
        }

        // 其他级别（INFO、WARN、ERROR）显示脱敏后的价格
        return PRICE_MASK;
    }

    /**
     * 脱敏价格信息（默认脱敏）
     *
     * <p>
     * 不依赖日志级别，直接返回脱敏后的价格
     *
     * @param price 价格
     * @return 脱敏后的价格字符串
     */
    public static String maskPrice(BigDecimal price) {
        if (price == null) {
            return "null";
        }
        return PRICE_MASK;
    }
}
