package org.example.domain.model.goods;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.example.common.exception.BizException;
import org.example.domain.model.goods.valueobject.GoodsStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * SPU 聚合根 (Standard Product Unit - 标准化产品单元)
 * 
 * <p>
 * SPU 代表一类商品，例如"iPhone 15 Pro"
 * 一个 SPU 可以有多个 SKU（不同规格，如颜色、尺寸）
 * </p>
 * 
 */
@Slf4j
@Data
public class Spu {

    /** SPU ID */
    private String spuId;

    /** 商品名称 */
    private String spuName;

    /** 分类ID */
    private String categoryId;

    /** 品牌 */
    private String brand;

    /** 商品描述 */
    private String description;

    /** 主图URL */
    private String mainImage;

    /** 详情图列表（JSON数组） */
    private String detailImages;

    /** 状态 */
    private GoodsStatus status;

    /** 排序权重 */
    private Integer sortOrder;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 关联的 SKU 列表（延迟加载） */
    private List<Sku> skuList;

    /**
     * 创建 SPU（工厂方法）
     */
    public static Spu create(String spuId, String spuName, String categoryId, String brand) {
        if (spuId == null || spuId.isEmpty()) {
            throw new BizException("SPU ID不能为空");
        }
        if (spuName == null || spuName.isEmpty()) {
            throw new BizException("商品名称不能为空");
        }

        Spu spu = new Spu();
        spu.spuId = spuId;
        spu.spuName = spuName;
        spu.categoryId = categoryId;
        spu.brand = brand;
        spu.status = GoodsStatus.OFF_SALE; // 默认下架
        spu.sortOrder = 0;
        spu.skuList = new ArrayList<>();
        spu.createTime = LocalDateTime.now();
        spu.updateTime = LocalDateTime.now();

        log.info("【SPU聚合】创建商品, spuId: {}, spuName: {}", spuId, spuName);
        return spu;
    }

    /**
     * 上架商品
     */
    public void onSale() {
        if (this.status == GoodsStatus.ON_SALE) {
            return; // 幂等
        }

        this.status = GoodsStatus.ON_SALE;
        this.updateTime = LocalDateTime.now();

        log.info("【SPU聚合】商品已上架, spuId: {}", spuId);
    }

    /**
     * 下架商品
     */
    public void offSale() {
        if (this.status == GoodsStatus.OFF_SALE) {
            return; // 幂等
        }

        this.status = GoodsStatus.OFF_SALE;
        this.updateTime = LocalDateTime.now();

        log.info("【SPU聚合】商品已下架, spuId: {}", spuId);
    }

    /**
     * 更新商品信息
     */
    public void updateInfo(String spuName, String description, String mainImage, String detailImages) {
        if (spuName != null && !spuName.isEmpty()) {
            this.spuName = spuName;
        }
        this.description = description;
        this.mainImage = mainImage;
        this.detailImages = detailImages;
        this.updateTime = LocalDateTime.now();

        log.info("【SPU聚合】商品信息已更新, spuId: {}", spuId);
    }

    /**
     * 判断是否在售
     */
    public boolean isOnSale() {
        return this.status != null && this.status.isOnSale();
    }
}
