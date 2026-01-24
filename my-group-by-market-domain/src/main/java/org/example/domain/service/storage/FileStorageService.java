package org.example.domain.service.storage;

import java.io.InputStream;

/**
 * 文件存储服务接口（Domain 层定义）
 *
 * 采用依赖倒置原则：
 * - Domain 层定义接口（框架无关）
 * - Infrastructure 层实现接口
 *
 * 这样可以支持多种存储方式：
 * - LocalFileStorageService（本地存储）
 * - OssFileStorageService（阿里云 OSS）
 * - CosFileStorageService（腾讯云 COS）
 */
public interface FileStorageService {

    /**
     * 上传文件
     *
     * @param inputStream 文件输入流
     * @param originalFilename 原始文件名
     * @param contentType 文件类型（如 image/jpeg）
     * @return 文件的访问 URL
     */
    String upload(InputStream inputStream, String originalFilename, String contentType);

    /**
     * 删除文件
     *
     * @param fileUrl 文件 URL
     */
    void delete(String fileUrl);
}
