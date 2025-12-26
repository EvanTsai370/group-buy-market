package org.example.interfaces.web.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageUtils {

    private final MessageSource messageSource;

    /**
     * 获取单个国际化消息
     * @param msgKey 消息 Key (例如: error.user.not.exist)
     * @return 翻译后的文本
     */
    public String get(String msgKey) {
        try {
            // LocaleContextHolder.getLocale() 会自动获取 HTTP Header 中的 Accept-Language
            return messageSource.getMessage(msgKey, null, LocaleContextHolder.getLocale());
        } catch (Exception e) {
            // 如果找不到 Key，就直接返回 Key 本身，防止报错
            return msgKey;
        }
    }

    /**
     * 获取带参数的国际化消息
     * @param msgKey 消息 Key (例如: error.param.invalid)
     * @param args 参数数组 (例如: ["用户名不能为空"])
     * @return 翻译后的文本 (例如: "Validation failed: 用户名不能为空")
     */
    public String get(String msgKey, Object... args) {
        try {
            return messageSource.getMessage(msgKey, args, LocaleContextHolder.getLocale());
        } catch (Exception e) {
            return msgKey;
        }
    }
}