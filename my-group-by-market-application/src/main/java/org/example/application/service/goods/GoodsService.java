package org.example.application.service.goods;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.application.service.goods.cmd.*;
import org.example.common.exception.BizException;
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

    /**
     * 创建 SPU
     */
    @Transactional
    public Spu createSpu(CreateSpuCmd cmd) {
        log.info("【GoodsService】创建SPU, spuName: {}", cmd.getSpuName());

        String spuId = "SPU-" + idGenerator.nextId();
        Spu spu = Spu.create(spuId, cmd.getSpuName(), cmd.getCategoryId(), cmd.getBrand());
        spu.setDescription(cmd.getDescription());
        spu.setMainImage(cmd.getMainImage());
        spu.setDetailImages(cmd.getDetailImages());

        spuRepository.save(spu);

        log.info("【GoodsService】SPU创建成功, spuId: {}", spuId);
        return spu;
    }

    /**
     * 创建 SKU
     */
    @Transactional
    public Sku createSku(CreateSkuCmd cmd) {
        log.info("【GoodsService】创建SKU, goodsName: {}, spuId: {}", cmd.getGoodsName(), cmd.getSpuId());

        // 验证 SPU 存在
        if (cmd.getSpuId() != null) {
            spuRepository.findBySpuId(cmd.getSpuId())
                    .orElseThrow(() -> new BizException("SPU不存在"));
        }

        String goodsId = "SKU-" + idGenerator.nextId();
        Sku sku = Sku.create(goodsId, cmd.getSpuId(), cmd.getGoodsName(),
                cmd.getOriginalPrice(), cmd.getStock());
        sku.setSpecInfo(cmd.getSpecInfo());
        sku.setSkuImage(cmd.getSkuImage());

        skuRepository.save(sku);

        log.info("【GoodsService】SKU创建成功, goodsId: {}", goodsId);
        return sku;
    }

    /**
     * 更新 SPU
     */
    @Transactional
    public Spu updateSpu(UpdateSpuCmd cmd) {
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
        return spu;
    }

    /**
     * 更新 SKU
     */
    @Transactional
    public Sku updateSku(UpdateSkuCmd cmd) {
        log.info("【GoodsService】更新SKU, goodsId: {}", cmd.getGoodsId());

        Sku sku = skuRepository.findByGoodsId(cmd.getGoodsId())
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

        log.info("【GoodsService】SKU更新成功, goodsId: {}", cmd.getGoodsId());
        return sku;
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
    public void addStock(String goodsId, int quantity) {
        Sku sku = skuRepository.findByGoodsId(goodsId)
                .orElseThrow(() -> new BizException("SKU不存在"));
        sku.addStock(quantity);
        skuRepository.update(sku);
        log.info("【GoodsService】库存增加成功, goodsId: {}, 增加: {}, 当前: {}",
                goodsId, quantity, sku.getStock());
    }

    /**
     * 查询 SPU
     */
    public Spu getSpuDetail(String spuId) {
        Spu spu = spuRepository.findBySpuId(spuId)
                .orElseThrow(() -> new BizException("SPU不存在"));
        // 加载 SKU 列表
        List<Sku> skuList = skuRepository.findBySpuId(spuId);
        spu.setSkuList(skuList);
        return spu;
    }

    /**
     * 查询 SKU
     */
    public Sku getSkuDetail(String goodsId) {
        return skuRepository.findByGoodsId(goodsId)
                .orElseThrow(() -> new BizException("SKU不存在"));
    }

    /**
     * 查询所有在售 SPU
     */
    public List<Spu> listOnSaleSpus() {
        return spuRepository.findAllOnSale();
    }

    /**
     * 查询所有在售 SKU
     */
    public List<Sku> listOnSaleSkus() {
        return skuRepository.findAllOnSale();
    }

    /**
     * 分页查询 SPU
     */
    public List<Spu> listSpus(int page, int size) {
        return spuRepository.findAll(page, size);
    }

    /**
     * 分页查询 SKU
     */
    public List<Sku> listSkus(int page, int size) {
        return skuRepository.findAll(page, size);
    }
}
