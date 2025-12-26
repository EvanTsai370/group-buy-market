package org.example.infrastructure.config;

import jakarta.validation.Validator;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@Configuration
public class ValidationConfig {

    @Bean
    public Validator validator(MessageSource messageSource) {
        LocalValidatorFactoryBean factory = new LocalValidatorFactoryBean();
        // 关键：将 Validation 的消息源指向 Spring 的 MessageSource (即 messages.properties)
        factory.setValidationMessageSource(messageSource);
        return factory;
    }
}