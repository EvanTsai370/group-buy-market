package org.example.interfaces.web.assembler;

import org.example.application.service.goods.cmd.*;
import org.example.application.service.goods.result.SkuResult;
import org.example.application.service.goods.result.SpuResult;
import org.example.interfaces.web.dto.admin.SkuResponse;
import org.example.interfaces.web.dto.admin.SpuResponse;
import org.example.interfaces.web.request.CreateSkuRequest;
import org.example.interfaces.web.request.CreateSpuRequest;
import org.example.interfaces.web.request.UpdateSkuRequest;
import org.example.interfaces.web.request.UpdateSpuRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 商品管理转换器（Interfaces 层）
 *
 * <p>
 * 职责：Request → Cmd 和 Result → Response 转换
 *
 * @author 开发团队
 * @since 2026-01-10
 */
@Mapper(componentModel = "spring")
public interface AdminGoodsAssembler {

    // ============== Request → Cmd ==============

    /**
     * CreateSpuRequest → CreateSpuCmd
     */
    CreateSpuCmd toCommand(CreateSpuRequest request);

    /**
     * UpdateSpuRequest → UpdateSpuCmd（需要外部设置 spuId）
     */
    UpdateSpuCmd toCommand(UpdateSpuRequest request);

    /**
     * CreateSkuRequest → CreateSkuCmd
     */
    CreateSkuCmd toCommand(CreateSkuRequest request);

    /**
     * UpdateSkuRequest → UpdateSkuCmd（需要外部设置 goodsId）
     */
    UpdateSkuCmd toCommand(UpdateSkuRequest request);

    // ============== Result → Response ==============

    /**
     * SpuResult → SpuResponse
     */
    SpuResponse toResponse(SpuResult result);

    /**
     * SpuResult 列表转换
     */
    List<SpuResponse> toSpuResponseList(List<SpuResult> results);

    /**
     * SkuResult → SkuResponse
     */
    SkuResponse toResponse(SkuResult result);

    /**
     * SkuResult 列表转换
     */
    List<SkuResponse> toSkuResponseList(List<SkuResult> results);
}
