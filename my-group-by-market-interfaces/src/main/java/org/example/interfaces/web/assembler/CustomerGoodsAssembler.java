package org.example.interfaces.web.assembler;

import org.example.application.service.customer.result.*;
import org.example.interfaces.web.dto.customer.*;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * C端商品 Assembler（MapStruct）
 * 负责 Application Result → Interfaces Response 转换
 * 
 * @author 开发团队
 * @since 2026-01-11
 */
@Mapper(componentModel = "spring")
public interface CustomerGoodsAssembler {

    /**
     * 商品列表结果 → 响应
     */
    CustomerGoodsListResponse toResponse(GoodsListResult result);

    /**
     * 商品列表结果列表 → 响应列表
     */
    List<CustomerGoodsListResponse> toListResponse(List<GoodsListResult> results);

    /**
     * 商品详情结果 → 响应
     */
    CustomerGoodsDetailResponse toResponse(GoodsDetailResult result);

    /**
     * 价格试算结果 → 响应
     */
    PriceTrialResponse toResponse(PriceTrialResult result);

    /**
     * 队伍列表结果 → 响应
     */
    TeamListResponse toResponse(TeamListResult result);

    /**
     * 队伍列表结果列表 → 响应列表
     */
    List<TeamListResponse> toTeamListResponse(List<TeamListResult> results);

    /**
     * SPU列表结果 → 响应
     */
    SpuListResponse toResponse(SpuListResult result);

    /**
     * SPU列表结果列表 → 响应列表
     */
    List<SpuListResponse> toSpuListResponse(List<SpuListResult> results);

    /**
     * SPU详情结果 → 响应
     */
    SpuDetailResponse toResponse(SpuDetailResult result);
}
