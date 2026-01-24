package org.example.interfaces.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.api.Result;
import org.example.common.exception.BizException;
import org.example.domain.service.storage.FileStorageService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传控制器
 *
 * 提供图片上传功能，支持商品主图、详情图等
 */
@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(name = "文件上传", description = "图片上传接口")
public class FileUploadController {

    private final FileStorageService fileStorageService;

    @PostMapping("/upload")
    @Operation(summary = "上传图片", description = "上传商品图片，返回图片URL")
    public Result<FileUploadResponse> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            // 校验文件
            if (file.isEmpty()) {
                throw new BizException("文件不能为空");
            }

            // 调用存储服务上传文件
            String fileUrl = fileStorageService.upload(
                file.getInputStream(),
                file.getOriginalFilename(),
                file.getContentType()
            );

            // 返回响应
            FileUploadResponse response = new FileUploadResponse();
            response.setFileUrl(fileUrl);
            response.setFileName(file.getOriginalFilename());
            response.setFileSize(file.getSize());

            log.info("【文件上传】上传成功, fileName: {}, fileUrl: {}",
                file.getOriginalFilename(), fileUrl);

            return Result.success(response);

        } catch (Exception e) {
            log.error("【文件上传】上传失败", e);
            throw new BizException("文件上传失败: " + e.getMessage());
        }
    }

    @DeleteMapping
    @Operation(summary = "删除图片", description = "删除已上传的图片")
    public Result<Void> deleteImage(@RequestParam("fileUrl") String fileUrl) {
        fileStorageService.delete(fileUrl);
        return Result.success();
    }

    /**
     * 文件上传响应
     */
    @Data
    public static class FileUploadResponse {
        private String fileUrl;
        private String fileName;
        private Long fileSize;
    }
}
