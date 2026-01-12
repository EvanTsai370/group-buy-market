package org.example.interfaces.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.application.service.customer.CustomerGoodsService;
import org.example.application.service.customer.query.PriceTrialQuery;
import org.example.application.service.customer.result.*;
import org.example.common.api.Result;
import org.example.interfaces.web.assembler.CustomerGoodsAssembler;
import org.example.interfaces.web.dto.customer.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * C端商品控制器
 * 
 * 提供给前端用户的商品相关接口
 * 
 * @author 开发团队
 * @since 2026-01-11
 */
@Slf4j
@RestController
@RequestMapping("/api/goods")
@RequiredArgsConstructor
@Tag(name = "商品（C端）", description = "面向用户的商品接口")
public class CustomerGoodsController {

    private final CustomerGoodsService customerGoodsService;
    private final CustomerGoodsAssembler customerGoodsAssembler;

    /**
     * 商品列表（首页）
     */
    @GetMapping("/list")
    @Operation(summary = "商品列表", description = "查询在售商品列表（首页展示）")
    public Result<List<CustomerGoodsListResponse>> listGoods() {
        log.info("【CustomerGoodsController】查询商品列表");

        List<GoodsListResult> results = customerGoodsService.listOnSaleGoods();
        List<CustomerGoodsListResponse> responses = customerGoodsAssembler.toListResponse(results);

        return Result.success(responses);
    }

    /**
     * 商品详情
     */
    @GetMapping("/{skuId}/detail")
    @Operation(summary = "商品详情", description = "查询商品详情（含活动信息）")
    public Result<CustomerGoodsDetailResponse> getGoodsDetail(@PathVariable String skuId) {
        log.info("【CustomerGoodsController】查询商品详情, skuId: {}", skuId);

        GoodsDetailResult result = customerGoodsService.getGoodsDetail(skuId);
        CustomerGoodsDetailResponse response = customerGoodsAssembler.toResponse(result);

        return Result.success(response);
    }

    /**
     * 价格试算
     */
    @GetMapping("/{skuId}/trial")
    @Operation(summary = "价格试算", description = "根据商品和渠道计算拼团价格")
    public Result<PriceTrialResponse> trialPrice(
            @PathVariable String skuId,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String userId) {

        log.info("【CustomerGoodsController】价格试算, skuId: {}, source: {}, channel: {}",
                skuId, source, channel);

        PriceTrialQuery query = new PriceTrialQuery();
        query.setSkuId(skuId);
        query.setSource(source);
        query.setChannel(channel);
        query.setUserId(userId);

        PriceTrialResult result = customerGoodsService.trialPrice(query);
        PriceTrialResponse response = customerGoodsAssembler.toResponse(result);

        return Result.success(response);
    }

    /**
     * 拼团队伍列表
     */
    @GetMapping("/{skuId}/teams")
    @Operation(summary = "拼团队伍", description = "查询商品的进行中拼团队伍")
    public Result<List<TeamListResponse>> listTeams(@PathVariable String skuId) {
        log.info("【CustomerGoodsController】查询拼团队伍, skuId: {}", skuId);

        List<TeamListResult> results = customerGoodsService.listGoodsTeams(skuId);
        List<TeamListResponse> responses = customerGoodsAssembler.toTeamListResponse(results);

        return Result.success(responses);
    }

    /**
     * SPU 列表（新版首页）
     */
    @GetMapping("/spu/list")
    @Operation(summary = "SPU列表", description = "查询在售SPU列表（支持关联活动展示）")
    public Result<List<SpuListResponse>> listSpus() {
        log.info("【CustomerGoodsController】查询SPU列表");

        List<SpuListResult> results = customerGoodsService.listSpuOnSale();
        List<SpuListResponse> responses = customerGoodsAssembler.toSpuListResponse(results);

        return Result.success(responses);
    }

    /**
     * SPU 详情
     */
    @GetMapping("/spu/{spuId}")
    @Operation(summary = "SPU详情", description = "查询SPU详情（含SKU列表和活动信息）")
    public Result<SpuDetailResponse> getSpuDetail(@PathVariable String spuId) {
        log.info("【CustomerGoodsController】查询SPU详情, spuId: {}", spuId);

        SpuDetailResult result = customerGoodsService.getSpuDetail(spuId);
        SpuDetailResponse response = customerGoodsAssembler.toResponse(result);

        return Result.success(response);
    }
}
