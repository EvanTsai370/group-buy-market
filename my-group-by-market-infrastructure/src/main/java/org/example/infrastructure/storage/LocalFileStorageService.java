package org.example.infrastructure.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.exception.BizException;
import org.example.domain.service.storage.FileStorageService;
import org.example.infrastructure.config.FileUploadConfig;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.UUID;

/**
 * 本地文件存储服务实现
 *
 * 功能：
 * 1. 将文件保存到本地磁盘
 * 2. 按日期分目录存储（如：2026/01/22/xxx.jpg）
 * 3. 生成唯一文件名（UUID）
 * 4. 返回可访问的 URL
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocalFileStorageService implements FileStorageService {

    private final FileUploadConfig config;

    @Override
    public String upload(InputStream inputStream, String originalFilename, String contentType) {
        try {
            // 1. 校验文件类型
            String fileExtension = getFileExtension(originalFilename);
            validateFileType(fileExtension);

            // 2. 生成存储路径（按日期分目录）
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String fileName = UUID.randomUUID().toString() + "." + fileExtension;
            String relativePath = datePath + "/" + fileName;

            // 3. 创建完整的文件路径
            Path uploadPath = Paths.get(config.getPath(), datePath);
            Files.createDirectories(uploadPath);

            Path filePath = uploadPath.resolve(fileName);

            // 4. 保存文件
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);

            // 5. 返回访问 URL（使用相对路径，便于前端代理）
            String fileUrl = "/files/" + relativePath;
            log.info("【文件上传】文件上传成功, fileUrl: {}", fileUrl);
            return fileUrl;

        } catch (IOException e) {
            log.error("【文件上传】文件上传失败", e);
            throw new BizException("文件上传失败");
        }
    }

    @Override
    public void delete(String fileUrl) {
        try {
            // 从 URL 中提取相对路径（处理相对路径格式 /files/yyyy/MM/dd/xxx.jpg）
            String relativePath = fileUrl.replace("/files/", "");
            Path filePath = Paths.get(config.getPath(), relativePath);

            // 删除文件
            Files.deleteIfExists(filePath);
            log.info("【文件删除】文件删除成功, fileUrl: {}", fileUrl);

        } catch (IOException e) {
            log.error("【文件删除】文件删除失败, fileUrl: {}", fileUrl, e);
            throw new BizException("文件删除失败");
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new BizException("文件名无效");
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * 校验文件类型
     */
    private void validateFileType(String fileExtension) {
        boolean isAllowed = Arrays.asList(config.getAllowedTypes()).contains(fileExtension);
        if (!isAllowed) {
            throw new BizException("不支持的文件类型: " + fileExtension);
        }
    }
}
