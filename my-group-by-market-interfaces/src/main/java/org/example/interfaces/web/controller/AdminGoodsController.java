package org.example.interfaces.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.application.service.goods.GoodsService;
import org.example.application.service.goods.cmd.*;
import org.example.common.api.Result;
import org.example.domain.model.goods.Sku;
import org.example.domain.model.goods.Spu;
import org.example.interfaces.web.request.CreateSkuRequest;
import org.example.interfaces.web.request.CreateSpuRequest;
import org.example.interfaces.web.request.UpdateSkuRequest;
import org.example.interfaces.web.request.UpdateSpuRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品管理控制器（管理后台）
 * 
 * @author 开发团队
 * @since 2026-01-10
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/goods")
@RequiredArgsConstructor
@Tag(name = "商品管理", description = "管理后台商品管理接口")
// TODO: 使用mapstruct
public class AdminGoodsController {

    private final GoodsService goodsService;

    // ============== SPU 管理 ==============

    @PostMapping("/spu")
    @Operation(summary = "创建SPU", description = "创建商品SPU")
    public Result<Spu> createSpu(@Valid @RequestBody CreateSpuRequest request) {
        log.info("【AdminGoods】创建SPU, spuName: {}", request.getSpuName());

        CreateSpuCmd cmd = new CreateSpuCmd();
        cmd.setSpuName(request.getSpuName());
        cmd.setCategoryId(request.getCategoryId());
        cmd.setBrand(request.getBrand());
        cmd.setDescription(request.getDescription());
        cmd.setMainImage(request.getMainImage());
        cmd.setDetailImages(request.getDetailImages());

        Spu spu = goodsService.createSpu(cmd);
        return Result.success(spu);
    }

    @PutMapping("/spu/{spuId}")
    @Operation(summary = "更新SPU", description = "更新商品SPU信息")
    public Result<Spu> updateSpu(@PathVariable String spuId,
            @Valid @RequestBody UpdateSpuRequest request) {
        log.info("【AdminGoods】更新SPU, spuId: {}", spuId);

        UpdateSpuCmd cmd = new UpdateSpuCmd();
        cmd.setSpuId(spuId);
        cmd.setSpuName(request.getSpuName());
        cmd.setCategoryId(request.getCategoryId());
        cmd.setBrand(request.getBrand());
        cmd.setDescription(request.getDescription());
        cmd.setMainImage(request.getMainImage());
        cmd.setDetailImages(request.getDetailImages());

        Spu spu = goodsService.updateSpu(cmd);
        return Result.success(spu);
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
    public Result<Spu> getSpuDetail(@PathVariable String spuId) {
        Spu spu = goodsService.getSpuDetail(spuId);
        return Result.success(spu);
    }

    @GetMapping("/spu")
    @Operation(summary = "分页查询SPU", description = "分页查询商品SPU列表")
    public Result<List<Spu>> listSpus(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<Spu> spus = goodsService.listSpus(page, size);
        return Result.success(spus);
    }

    // ============== SKU 管理 ==============

    @PostMapping("/sku")
    @Operation(summary = "创建SKU", description = "创建商品SKU")
    public Result<Sku> createSku(@Valid @RequestBody CreateSkuRequest request) {
        log.info("【AdminGoods】创建SKU, goodsName: {}", request.getGoodsName());

        CreateSkuCmd cmd = new CreateSkuCmd();
        cmd.setSpuId(request.getSpuId());
        cmd.setGoodsName(request.getGoodsName());
        cmd.setSpecInfo(request.getSpecInfo());
        cmd.setOriginalPrice(request.getOriginalPrice());
        cmd.setStock(request.getStock());
        cmd.setSkuImage(request.getSkuImage());

        Sku sku = goodsService.createSku(cmd);
        return Result.success(sku);
    }

    @PutMapping("/sku/{goodsId}")
    @Operation(summary = "更新SKU", description = "更新商品SKU信息")
    public Result<Sku> updateSku(@PathVariable String goodsId,
            @Valid @RequestBody UpdateSkuRequest request) {
        log.info("【AdminGoods】更新SKU, goodsId: {}", goodsId);

        UpdateSkuCmd cmd = new UpdateSkuCmd();
        cmd.setGoodsId(goodsId);
        cmd.setGoodsName(request.getGoodsName());
        cmd.setSpecInfo(request.getSpecInfo());
        cmd.setOriginalPrice(request.getOriginalPrice());
        cmd.setSkuImage(request.getSkuImage());

        Sku sku = goodsService.updateSku(cmd);
        return Result.success(sku);
    }

    @PostMapping("/sku/{goodsId}/add-stock")
    @Operation(summary = "增加库存", description = "增加SKU库存")
    public Result<Void> addStock(@PathVariable String goodsId,
            @RequestParam int quantity) {
        log.info("【AdminGoods】增加库存, goodsId: {}, quantity: {}", goodsId, quantity);
        goodsService.addStock(goodsId, quantity);
        return Result.success();
    }

    @GetMapping("/sku/{goodsId}")
    @Operation(summary = "查询SKU详情", description = "查询商品SKU详情")
    public Result<Sku> getSkuDetail(@PathVariable String goodsId) {
        Sku sku = goodsService.getSkuDetail(goodsId);
        return Result.success(sku);
    }

    @GetMapping("/sku")
    @Operation(summary = "分页查询SKU", description = "分页查询商品SKU列表")
    public Result<List<Sku>> listSkus(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<Sku> skus = goodsService.listSkus(page, size);
        return Result.success(skus);
    }
}
