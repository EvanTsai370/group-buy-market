package org.example.application.assembler;

import org.example.application.service.goods.result.SkuResult;
import org.example.application.service.goods.result.SpuResult;
import org.example.domain.model.goods.Sku;
import org.example.domain.model.goods.Spu;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 商品结果转换器（Application 层）
 *
 * <p>
 * 职责：领域模型 → 视图模型转换
 *
 */
@Mapper(componentModel = "spring")
public interface GoodsResultAssembler {

    /**
     * Spu → SpuResult 转换
     */
    @Mapping(target = "status", expression = "java(spu.getStatus() != null ? spu.getStatus().name() : null)")
    @Mapping(target = "minPrice", ignore = true)
    @Mapping(target = "skuCount", ignore = true)
    SpuResult toResult(Spu spu);

    /**
     * Spu 列表转换
     */
    List<SpuResult> toSpuResultList(List<Spu> spus);

    /**
     * Sku → SkuResult 转换
     */
    @Mapping(target = "price", source = "originalPrice")
    SkuResult toResult(Sku sku);

    /**
     * Sku 列表转换
     */
    List<SkuResult> toSkuResultList(List<Sku> skus);
}
