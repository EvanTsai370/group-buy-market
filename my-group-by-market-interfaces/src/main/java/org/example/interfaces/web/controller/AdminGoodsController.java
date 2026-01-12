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
import org.example.interfaces.web.assembler.AdminGoodsAssembler;
import org.example.interfaces.web.dto.admin.SkuResponse;
import org.example.interfaces.web.dto.admin.SpuResponse;
import org.example.interfaces.web.request.CreateSkuRequest;
import org.example.interfaces.web.request.CreateSpuRequest;
import org.example.interfaces.web.request.UpdateSkuRequest;
import org.example.interfaces.web.request.UpdateSpuRequest;
import org.springframework.security.access.prepost.PreAuthorize;
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
@PreAuthorize("hasRole('ADMIN')")
public class AdminGoodsController {

    private final GoodsService goodsService;
    private final AdminGoodsAssembler adminGoodsAssembler;

    // ============== SPU 管理 ==============

    @PostMapping("/spu")
    @Operation(summary = "创建SPU", description = "创建商品SPU")
    public Result<SpuResponse> createSpu(@Valid @RequestBody CreateSpuRequest request) {
        log.info("【AdminGoods】创建SPU, spuName: {}", request.getSpuName());
        CreateSpuCmd cmd = adminGoodsAssembler.toCommand(request);
        SpuResult result = goodsService.createSpu(cmd);
        return Result.success(adminGoodsAssembler.toResponse(result));
    }

    @PutMapping("/spu/{spuId}")
    @Operation(summary = "更新SPU", description = "更新商品SPU信息")
    public Result<SpuResponse> updateSpu(@PathVariable String spuId,
            @Valid @RequestBody UpdateSpuRequest request) {
        log.info("【AdminGoods】更新SPU, spuId: {}", spuId);
        UpdateSpuCmd cmd = adminGoodsAssembler.toCommand(request);
        cmd.setSpuId(spuId);
        SpuResult result = goodsService.updateSpu(cmd);
        return Result.success(adminGoodsAssembler.toResponse(result));
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
        return Result.success(adminGoodsAssembler.toResponse(result));
    }

    @GetMapping("/spu")
    @Operation(summary = "分页查询SPU", description = "分页查询商品SPU列表")
    public Result<List<SpuResponse>> listSpus(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<SpuResult> results = goodsService.listSpus(page, size);
        return Result.success(adminGoodsAssembler.toSpuResponseList(results));
    }

    // ============== SKU 管理 ==============

    @PostMapping("/sku")
    @Operation(summary = "创建SKU", description = "创建商品SKU")
    public Result<SkuResponse> createSku(@Valid @RequestBody CreateSkuRequest request) {
        log.info("【AdminGoods】创建SKU, goodsName: {}", request.getGoodsName());
        CreateSkuCmd cmd = adminGoodsAssembler.toCommand(request);
        SkuResult result = goodsService.createSku(cmd);
        return Result.success(adminGoodsAssembler.toResponse(result));
    }

    @PutMapping("/sku/{goodsId}")
    @Operation(summary = "更新SKU", description = "更新商品SKU信息")
    public Result<SkuResponse> updateSku(@PathVariable String goodsId,
            @Valid @RequestBody UpdateSkuRequest request) {
        log.info("【AdminGoods】更新SKU, goodsId: {}", goodsId);
        UpdateSkuCmd cmd = adminGoodsAssembler.toCommand(request);
        cmd.setGoodsId(goodsId);
        SkuResult result = goodsService.updateSku(cmd);
        return Result.success(adminGoodsAssembler.toResponse(result));
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
    public Result<SkuResponse> getSkuDetail(@PathVariable String goodsId) {
        SkuResult result = goodsService.getSkuDetail(goodsId);
        return Result.success(adminGoodsAssembler.toResponse(result));
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
