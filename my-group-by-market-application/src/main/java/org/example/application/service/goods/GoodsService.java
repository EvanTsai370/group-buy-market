package org.example.application.service.goods;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.application.assembler.GoodsResultAssembler;
import org.example.application.service.goods.cmd.*;
import org.example.application.service.goods.result.SkuResult;
import org.example.application.service.goods.result.SpuResult;
import org.example.common.exception.BizException;
import org.example.common.model.PageResult;
import org.example.domain.model.goods.Sku;
import org.example.domain.model.goods.Spu;
import org.example.domain.model.goods.repository.SkuRepository;
import org.example.domain.model.goods.repository.SpuRepository;
import org.example.domain.shared.IdGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 商品管理服务（管理后台使用）
 * 
 * @author 开发团队
 * @since 2026-01-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsService {

    private final SpuRepository spuRepository;
    private final SkuRepository skuRepository;
    private final IdGenerator idGenerator;
    private final GoodsResultAssembler goodsResultAssembler;

    /**
     * 创建 SPU
     */
    @Transactional
    public SpuResult createSpu(CreateSpuCmd cmd) {
        log.info("【GoodsService】创建SPU, spuName: {}", cmd.getSpuName());

        String spuId = "SPU-" + idGenerator.nextId();
        Spu spu = Spu.create(spuId, cmd.getSpuName(), cmd.getCategoryId(), cmd.getBrand());
        spu.setDescription(cmd.getDescription());
        spu.setMainImage(cmd.getMainImage());
        spu.setDetailImages(cmd.getDetailImages());

        spuRepository.save(spu);

        log.info("【GoodsService】SPU创建成功, spuId: {}", spuId);
        return goodsResultAssembler.toResult(spu);
    }

    /**
     * 创建 SKU
     */
    @Transactional
    public SkuResult createSku(CreateSkuCmd cmd) {
        log.info("【GoodsService】创建SKU, goodsName: {}, spuId: {}", cmd.getGoodsName(), cmd.getSpuId());

        // 验证 SPU 存在
        if (cmd.getSpuId() != null) {
            spuRepository.findBySpuId(cmd.getSpuId())
                    .orElseThrow(() -> new BizException("SPU不存在"));
        }

        String skuId = "SKU-" + idGenerator.nextId();
        Sku sku = Sku.create(skuId, cmd.getSpuId(), cmd.getGoodsName(),
                cmd.getOriginalPrice(), cmd.getStock());
        sku.setSpecInfo(cmd.getSpecInfo());
        sku.setSkuImage(cmd.getSkuImage());

        skuRepository.save(sku);

        log.info("【GoodsService】SKU创建成功, skuId: {}", skuId);
        return goodsResultAssembler.toResult(sku);
    }

    /**
     * 更新 SPU
     */
    @Transactional
    public SpuResult updateSpu(UpdateSpuCmd cmd) {
        log.info("【GoodsService】更新SPU, spuId: {}", cmd.getSpuId());

        Spu spu = spuRepository.findBySpuId(cmd.getSpuId())
                .orElseThrow(() -> new BizException("SPU不存在"));

        spu.updateInfo(cmd.getSpuName(), cmd.getDescription(),
                cmd.getMainImage(), cmd.getDetailImages());

        if (cmd.getCategoryId() != null) {
            spu.setCategoryId(cmd.getCategoryId());
        }
        if (cmd.getBrand() != null) {
            spu.setBrand(cmd.getBrand());
        }

        spuRepository.update(spu);

        log.info("【GoodsService】SPU更新成功, spuId: {}", cmd.getSpuId());
        return goodsResultAssembler.toResult(spu);
    }

    /**
     * 更新 SKU
     */
    @Transactional
    public SkuResult updateSku(UpdateSkuCmd cmd) {
        log.info("【GoodsService】更新SKU, skuId: {}", cmd.getSkuId());

        Sku sku = skuRepository.findBySkuId(cmd.getSkuId())
                .orElseThrow(() -> new BizException("SKU不存在"));

        if (cmd.getGoodsName() != null) {
            sku.setGoodsName(cmd.getGoodsName());
        }
        if (cmd.getOriginalPrice() != null) {
            sku.updatePrice(cmd.getOriginalPrice());
        }
        if (cmd.getSpecInfo() != null) {
            sku.setSpecInfo(cmd.getSpecInfo());
        }
        if (cmd.getSkuImage() != null) {
            sku.setSkuImage(cmd.getSkuImage());
        }

        skuRepository.update(sku);

        log.info("【GoodsService】SKU更新成功, skuId: {}", cmd.getSkuId());
        return goodsResultAssembler.toResult(sku);
    }

    /**
     * 上架 SPU
     */
    @Transactional
    public void onSaleSpu(String spuId) {
        Spu spu = spuRepository.findBySpuId(spuId)
                .orElseThrow(() -> new BizException("SPU不存在"));
        spu.onSale();
        spuRepository.update(spu);
        log.info("【GoodsService】SPU已上架, spuId: {}", spuId);
    }

    /**
     * 下架 SPU
     */
    @Transactional
    public void offSaleSpu(String spuId) {
        Spu spu = spuRepository.findBySpuId(spuId)
                .orElseThrow(() -> new BizException("SPU不存在"));
        spu.offSale();
        spuRepository.update(spu);
        log.info("【GoodsService】SPU已下架, spuId: {}", spuId);
    }

    /**
     * 增加库存
     */
    @Transactional
    public void addStock(String skuId, int quantity) {
        Sku sku = skuRepository.findBySkuId(skuId)
                .orElseThrow(() -> new BizException("SKU不存在"));
        sku.addStock(quantity);
        skuRepository.update(sku);
        log.info("【GoodsService】库存增加成功, skuId: {}, 增加: {}, 当前: {}",
                skuId, quantity, sku.getStock());
    }

    /**
     * 查询 SPU
     */
    public SpuResult getSpuDetail(String spuId) {
        Spu spu = spuRepository.findBySpuId(spuId)
                .orElseThrow(() -> new BizException("SPU不存在"));
        // 加载 SKU 列表
        List<Sku> skuList = skuRepository.findBySpuId(spuId);
        spu.setSkuList(skuList);
        return goodsResultAssembler.toResult(spu);
    }

    /**
     * 查询 SKU
     */
    public SkuResult getSkuDetail(String skuId) {
        Sku sku = skuRepository.findBySkuId(skuId)
                .orElseThrow(() -> new BizException("SKU不存在"));
        return goodsResultAssembler.toResult(sku);
    }

    /**
     * 查询所有在售 SPU
     */
    public List<SpuResult> listOnSaleSpus() {
        List<Spu> spus = spuRepository.findAllOnSale();
        return goodsResultAssembler.toSpuResultList(spus);
    }

    /**
     * 查询所有在售 SKU
     */
    public List<SkuResult> listOnSaleSkus() {
        List<Sku> skus = skuRepository.findAllOnSale();
        return goodsResultAssembler.toSkuResultList(skus);
    }

    /**
     * 分页查询 SPU
     */
    public PageResult<SpuResult> listSpus(int page, int size) {
        PageResult<Spu> pageResult = spuRepository.findAll(page, size);

        List<SpuResult> list = goodsResultAssembler.toSpuResultList(pageResult.getList());

        return PageResult.of(list, pageResult.getTotal(), pageResult.getPage(), pageResult.getSize());
    }

    /**
     * 分页查询 SKU
     */
    public List<SkuResult> listSkus(int page, int size) {
        List<Sku> skus = skuRepository.findAll(page, size);
        return goodsResultAssembler.toSkuResultList(skus);
    }
}
