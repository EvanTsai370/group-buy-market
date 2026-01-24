package org.example.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文件上传配置
 *
 * 从 application.yml 中读取文件上传相关配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "file.upload")
public class FileUploadConfig {

    /**
     * 文件存储路径（本地磁盘路径）
     * 例如：/Users/bin/upload 或 /var/www/upload
     */
    private String path;

    /**
     * 访问URL前缀
     * 例如：http://localhost:8080/files
     */
    private String urlPrefix;

    /**
     * 允许的文件类型
     */
    private String[] allowedTypes = {"jpg", "jpeg", "png", "gif", "webp"};

    /**
     * 最大文件大小（字节）
     * 默认 5MB
     */
    private long maxSize = 5 * 1024 * 1024;
}
