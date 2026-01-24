package org.example.interfaces.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.application.service.goods.GoodsService;
import org.example.application.service.goods.cmd.*;
import org.example.application.service.goods.result.SkuResult;
import org.example.application.service.goods.result.SpuResult;
import org.example.common.api.Result;
import org.example.common.exception.BizException;
import org.example.common.model.PageResult;
import org.example.domain.service.storage.FileStorageService;
import org.example.interfaces.web.assembler.AdminGoodsAssembler;
import org.example.interfaces.web.dto.admin.SkuResponse;
import org.example.interfaces.web.dto.admin.SpuResponse;
import org.example.interfaces.web.request.CreateSkuRequest;
import org.example.interfaces.web.request.CreateSpuRequest;
import org.example.interfaces.web.request.UpdateSkuRequest;
import org.example.interfaces.web.request.UpdateSpuRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * 商品管理控制器（管理后台）
 * 
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/goods")
@RequiredArgsConstructor
@Tag(name = "商品管理", description = "管理后台商品管理接口")
@PreAuthorize("hasRole('ADMIN')")
public class AdminGoodsController {

    private final GoodsService goodsService;
    private final AdminGoodsAssembler adminGoodsAssembler;
    private final FileStorageService fileStorageService;

    // ============== 图片上传 ==============

    @PostMapping("/upload/image")
    @Operation(summary = "上传商品图片", description = "上传商品主图或详情图，返回图片URL")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                throw new BizException("文件不能为空");
            }

            String fileUrl = fileStorageService.upload(
                file.getInputStream(),
                file.getOriginalFilename(),
                file.getContentType()
            );

            log.info("【商品图片上传】上传成功, fileName: {}, fileUrl: {}",
                file.getOriginalFilename(), fileUrl);

            return Result.success(fileUrl);

        } catch (Exception e) {
            log.error("【商品图片上传】上传失败", e);
            throw new BizException("图片上传失败: " + e.getMessage());
        }
    }

    @PostMapping("/upload/images")
    @Operation(summary = "批量上传商品图片", description = "批量上传详情图，返回图片URL列表")
    public Result<List<String>> uploadImages(@RequestParam("files") MultipartFile[] files) {
        try {
            if (files == null || files.length == 0) {
                throw new BizException("文件不能为空");
            }

            List<String> fileUrls = new ArrayList<>();
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String fileUrl = fileStorageService.upload(
                        file.getInputStream(),
                        file.getOriginalFilename(),
                        file.getContentType()
                    );
                    fileUrls.add(fileUrl);
                }
            }

            log.info("【商品图片批量上传】上传成功, 数量: {}", fileUrls.size());
            return Result.success(fileUrls);

        } catch (Exception e) {
            log.error("【商品图片批量上传】上传失败", e);
            throw new BizException("图片批量上传失败: " + e.getMessage());
        }
    }

    // ============== SPU 管理 ==============

    @PostMapping("/spu")
    @Operation(
        summary = "创建SPU",
        description = """
            创建商品SPU

            使用步骤：
            1. 先调用 POST /api/admin/goods/upload/image 上传主图，获取 mainImage URL
            2. 再调用 POST /api/admin/goods/upload/images 批量上传详情图，获取 detailImages URL 列表
            3. 将获取的图片 URL 填入本接口的 mainImage 和 detailImages 字段

            示例：
            {
              "spuName": "iPhone 15 Pro",
              "categoryId": "CAT001",
              "brand": "Apple",
              "description": "最新款 iPhone",
              "mainImage": "http://localhost:8080/files/2026/01/22/uuid.jpg",
              "detailImages": "[\\"http://localhost:8080/files/2026/01/22/uuid2.jpg\\"]"
            }
            """
    )
    public Result<SpuResponse> createSpu(@Valid @RequestBody CreateSpuRequest request) {
        log.info("【AdminGoods】创建SPU, spuName: {}", request.getSpuName());
        CreateSpuCmd cmd = adminGoodsAssembler.toCommand(request);
        SpuResult result = goodsService.createSpu(cmd);
        return Result.success(adminGoodsAssembler.toSpuResponse(result));
    }

    @PutMapping("/spu/{spuId}")
    @Operation(summary = "更新SPU", description = "更新商品SPU信息")
    public Result<SpuResponse> updateSpu(@PathVariable String spuId,
            @Valid @RequestBody UpdateSpuRequest request) {
        log.info("【AdminGoods】更新SPU, spuId: {}", spuId);
        UpdateSpuCmd cmd = adminGoodsAssembler.toCommand(request);
        cmd.setSpuId(spuId);
        SpuResult result = goodsService.updateSpu(cmd);
        return Result.success(adminGoodsAssembler.toSpuResponse(result));
    }

    @PostMapping("/spu/{spuId}/on-sale")
    @Operation(summary = "上架SPU", description = "上架商品")
    public Result<Void> onSaleSpu(@PathVariable String spuId) {
        log.info("【AdminGoods】上架SPU, spuId: {}", spuId);
        goodsService.onSaleSpu(spuId);
        return Result.success();
    }

    @PostMapping("/spu/{spuId}/off-sale")
    @Operation(summary = "下架SPU", description = "下架商品")
    public Result<Void> offSaleSpu(@PathVariable String spuId) {
        log.info("【AdminGoods】下架SPU, spuId: {}", spuId);
        goodsService.offSaleSpu(spuId);
        return Result.success();
    }

    @GetMapping("/spu/{spuId}")
    @Operation(summary = "查询SPU详情", description = "查询商品SPU详情（包含SKU列表）")
    public Result<SpuResponse> getSpuDetail(@PathVariable String spuId) {
        SpuResult result = goodsService.getSpuDetail(spuId);
        return Result.success(adminGoodsAssembler.toSpuResponse(result));
    }

    @GetMapping("/spu")
    @Operation(summary = "分页查询SPU", description = "分页查询商品SPU列表")
    public Result<PageResult<SpuResponse>> listSpus(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        PageResult<SpuResult> result = goodsService.listSpus(page, size);
        return Result.success(adminGoodsAssembler.toSpuResponsePage(result));
    }

    // ============== SKU 管理 ==============

    @PostMapping("/sku")
    @Operation(
        summary = "创建SKU",
        description = """
            创建商品SKU

            使用步骤：
            1. 先调用 POST /api/admin/goods/upload/image 上传 SKU 图片，获取 skuImage URL
            2. 将获取的图片 URL 填入本接口的 skuImage 字段

            示例：
            {
              "spuId": "SPU001",
              "goodsName": "iPhone 15 Pro 256GB 黑色",
              "specInfo": "{\\"颜色\\":\\"黑色\\",\\"容量\\":\\"256GB\\"}",
              "originalPrice": 7999.00,
              "stock": 100,
              "skuImage": "http://localhost:8080/files/2026/01/22/uuid.jpg"
            }
            """
    )
    public Result<SkuResponse> createSku(@Valid @RequestBody CreateSkuRequest request) {
        log.info("【AdminGoods】创建SKU, goodsName: {}", request.getGoodsName());
        CreateSkuCmd cmd = adminGoodsAssembler.toCommand(request);
        SkuResult result = goodsService.createSku(cmd);
        return Result.success(adminGoodsAssembler.toSkuResponse(result));
    }

    @PutMapping("/sku/{skuId}")
    @Operation(summary = "更新SKU", description = "更新商品SKU信息")
    public Result<SkuResponse> updateSku(@PathVariable String skuId,
            @Valid @RequestBody UpdateSkuRequest request) {
        log.info("【AdminGoods】更新SKU, skuId: {}", skuId);
        UpdateSkuCmd cmd = adminGoodsAssembler.toCommand(request);
        cmd.setSkuId(skuId);
        SkuResult result = goodsService.updateSku(cmd);
        return Result.success(adminGoodsAssembler.toSkuResponse(result));
    }

    @PostMapping("/sku/{skuId}/add-stock")
    @Operation(summary = "增加库存", description = "增加SKU库存")
    public Result<Void> addStock(@PathVariable String skuId,
            @RequestParam int quantity) {
        log.info("【AdminGoods】增加库存, skuId: {}, quantity: {}", skuId, quantity);
        goodsService.addStock(skuId, quantity);
        return Result.success();
    }

    @GetMapping("/sku/{skuId}")
    @Operation(summary = "查询SKU详情", description = "查询商品SKU详情")
    public Result<SkuResponse> getSkuDetail(@PathVariable String skuId) {
        SkuResult result = goodsService.getSkuDetail(skuId);
        return Result.success(adminGoodsAssembler.toSkuResponse(result));
    }

    @GetMapping("/sku")
    @Operation(summary = "分页查询SKU", description = "分页查询商品SKU列表")
    public Result<List<SkuResponse>> listSkus(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<SkuResult> results = goodsService.listSkus(page, size);
        return Result.success(adminGoodsAssembler.toSkuResponseList(results));
    }
}
