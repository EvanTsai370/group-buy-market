package org.example.infrastructure.config.dynamic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 类型转换器
 * 支持常见Java类型的字符串转换
 */
public class TypeConverter {

    private static final Logger log = LoggerFactory.getLogger(TypeConverter.class);

    /**
     * 类型转换器映射表
     */
    private static final Map<Class<?>, Function<String, ?>> CONVERTERS = new HashMap<>();

    static {
        // 基本类型及包装类
        CONVERTERS.put(String.class, value -> value);
        CONVERTERS.put(Integer.class, Integer::parseInt);
        CONVERTERS.put(int.class, Integer::parseInt);
        CONVERTERS.put(Long.class, Long::parseLong);
        CONVERTERS.put(long.class, Long::parseLong);
        CONVERTERS.put(Double.class, Double::parseDouble);
        CONVERTERS.put(double.class, Double::parseDouble);
        CONVERTERS.put(Float.class, Float::parseFloat);
        CONVERTERS.put(float.class, Float::parseFloat);
        CONVERTERS.put(Boolean.class, TypeConverter::parseBoolean);
        CONVERTERS.put(boolean.class, TypeConverter::parseBoolean);
        CONVERTERS.put(Short.class, Short::parseShort);
        CONVERTERS.put(short.class, Short::parseShort);
        CONVERTERS.put(Byte.class, Byte::parseByte);
        CONVERTERS.put(byte.class, Byte::parseByte);

        // 高精度数值
        CONVERTERS.put(BigDecimal.class, BigDecimal::new);

        // 日期时间类型
        CONVERTERS.put(LocalDate.class, value -> LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE));
        CONVERTERS.put(LocalDateTime.class, value -> LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }

    /**
     * 将字符串值转换为目标类型
     *
     * @param value      原始字符串值
     * @param targetType 目标类型
     * @return 转换后的值
     * @throws ConfigConversionException 转换失败时抛出
     */
    public static Object convert(String value, Class<?> targetType) {
        if (value == null) {
            return null;
        }

        // 空字符串处理
        if (value.trim().isEmpty()) {
            if (targetType == String.class) {
                return value;
            }
            return null;
        }

        Function<String, ?> converter = CONVERTERS.get(targetType);
        if (converter == null) {
            log.warn("不支持的类型转换: {} -> {}, 返回原始字符串", value, targetType.getName());
            return value;
        }

        try {
            return converter.apply(value.trim());
        } catch (Exception e) {
            String errorMsg = String.format("配置值类型转换失败: value=%s, targetType=%s", value, targetType.getName());
            log.error(errorMsg, e);
            throw new ConfigConversionException(errorMsg, e);
        }
    }

    /**
     * 增强的布尔值解析
     * 支持: true/false, 1/0, yes/no, on/off, enable/disable, open/close
     */
    private static Boolean parseBoolean(String value) {
        String normalized = value.trim().toLowerCase();

        // true values
        if ("true".equals(normalized) || "1".equals(normalized) ||
            "yes".equals(normalized) || "on".equals(normalized) ||
            "enable".equals(normalized) || "open".equals(normalized)) {
            return true;
        }

        // false values
        if ("false".equals(normalized) || "0".equals(normalized) ||
            "no".equals(normalized) || "off".equals(normalized) ||
            "disable".equals(normalized) || "close".equals(normalized)) {
            return false;
        }

        throw new IllegalArgumentException("无法解析布尔值: " + value);
    }

    /**
     * 检查是否支持该类型转换
     */
    public static boolean isSupported(Class<?> targetType) {
        return CONVERTERS.containsKey(targetType);
    }

    /**
     * 注册自定义类型转换器
     */
    public static <T> void registerConverter(Class<T> targetType, Function<String, T> converter) {
        CONVERTERS.put(targetType, converter);
        log.info("注册自定义类型转换器: {}", targetType.getName());
    }

    /**
     * 配置转换异常
     */
    public static class ConfigConversionException extends RuntimeException {
        public ConfigConversionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
