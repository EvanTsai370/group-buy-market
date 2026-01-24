package org.example.infrastructure.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 静态资源配置
 *
 * 功能：
 * 1. 配置文件访问路径
 * 2. 将 /files/** 映射到本地文件系统
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class StaticResourceConfig implements WebMvcConfigurer {

    private final FileUploadConfig fileUploadConfig;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置文件访问路径
        // 例如：访问 http://localhost:8080/files/2026/01/22/xxx.jpg
        // 映射到 file:///tmp/my-group-buy-market/upload/2026/01/22/xxx.jpg
        String resourceLocation = "file:" + fileUploadConfig.getPath() + "/";

        registry.addResourceHandler("/files/**")
                .addResourceLocations(resourceLocation);

        log.info("【静态资源配置】文件访问路径: /files/** -> {}", resourceLocation);
    }
}
