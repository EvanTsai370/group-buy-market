package org.example.domain.model.goods;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.example.common.exception.BizException;
import org.example.domain.model.goods.valueobject.GoodsStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * SKU 实体 (Stock Keeping Unit - 库存量单位)
 * 
 * <p>
 * SKU 代表商品的具体规格，例如"iPhone 15 Pro 256GB 黑色钛金属"
 * 每个 SKU 有独立的库存和价格
 * </p>
 * 
 * @author 开发团队
 * @since 2026-01-10
 */
@Slf4j
@Data
public class Sku {

    /** SKU ID（商品ID） */
    private String goodsId;

    /** 关联的 SPU ID */
    private String spuId;

    /** SKU 名称（含规格） */
    private String goodsName;

    /** 规格信息（JSON格式） */
    private String specInfo;

    /** 原价 */
    private BigDecimal originalPrice;

    /** 库存数量 */
    private Integer stock;

    /** 冻结库存（锁单预占） */
    private Integer frozenStock;

    /** SKU 图片 */
    private String skuImage;

    /** 状态 */
    private GoodsStatus status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /**
     * 创建 SKU（工厂方法）
     */
    public static Sku create(String goodsId, String spuId, String goodsName,
            BigDecimal originalPrice, Integer stock) {
        if (goodsId == null || goodsId.isEmpty()) {
            throw new BizException("SKU ID不能为空");
        }
        if (originalPrice == null || originalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("价格必须大于0");
        }
        if (stock == null || stock < 0) {
            throw new BizException("库存不能为负数");
        }

        Sku sku = new Sku();
        sku.goodsId = goodsId;
        sku.spuId = spuId;
        sku.goodsName = goodsName;
        sku.originalPrice = originalPrice;
        sku.stock = stock;
        sku.frozenStock = 0;
        sku.status = GoodsStatus.ON_SALE;
        sku.createTime = LocalDateTime.now();
        sku.updateTime = LocalDateTime.now();

        log.info("【SKU实体】创建SKU, goodsId: {}, goodsName: {}, stock: {}",
                goodsId, goodsName, stock);
        return sku;
    }

    /**
     * 获取可用库存（总库存 - 冻结库存）
     */
    public int getAvailableStock() {
        return Math.max(0, stock - frozenStock);
    }

    /**
     * 冻结库存（锁单时调用）
     */
    public void freezeStock(int quantity) {
        if (quantity <= 0) {
            throw new BizException("冻结数量必须大于0");
        }
        if (getAvailableStock() < quantity) {
            throw new BizException("库存不足，无法冻结");
        }

        this.frozenStock += quantity;
        this.updateTime = LocalDateTime.now();

        log.info("【SKU实体】库存已冻结, goodsId: {}, 冻结数量: {}, 冻结后: {}",
                goodsId, quantity, frozenStock);
    }

    /**
     * 释放冻结库存（超时/退单时调用）
     */
    public void unfreezeStock(int quantity) {
        if (quantity <= 0) {
            throw new BizException("释放数量必须大于0");
        }

        this.frozenStock = Math.max(0, this.frozenStock - quantity);
        this.updateTime = LocalDateTime.now();

        log.info("【SKU实体】库存已释放, goodsId: {}, 释放数量: {}, 释放后: {}",
                goodsId, quantity, frozenStock);
    }

    /**
     * 扣减库存（支付成功后调用）
     */
    public void deductStock(int quantity) {
        if (quantity <= 0) {
            throw new BizException("扣减数量必须大于0");
        }
        if (this.frozenStock < quantity) {
            throw new BizException("冻结库存不足，无法扣减");
        }

        this.stock -= quantity;
        this.frozenStock -= quantity;
        this.updateTime = LocalDateTime.now();

        log.info("【SKU实体】库存已扣减, goodsId: {}, 扣减数量: {}, 剩余库存: {}",
                goodsId, quantity, stock);
    }

    /**
     * 增加库存
     */
    public void addStock(int quantity) {
        if (quantity <= 0) {
            throw new BizException("增加数量必须大于0");
        }

        this.stock += quantity;
        this.updateTime = LocalDateTime.now();

        log.info("【SKU实体】库存已增加, goodsId: {}, 增加数量: {}, 当前库存: {}",
                goodsId, quantity, stock);
    }

    /**
     * 更新价格
     */
    public void updatePrice(BigDecimal newPrice) {
        if (newPrice == null || newPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("价格必须大于0");
        }

        this.originalPrice = newPrice;
        this.updateTime = LocalDateTime.now();

        log.info("【SKU实体】价格已更新, goodsId: {}, 新价格: {}", goodsId, newPrice);
    }

    /**
     * 上架
     */
    public void onSale() {
        this.status = GoodsStatus.ON_SALE;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 下架
     */
    public void offSale() {
        this.status = GoodsStatus.OFF_SALE;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 判断是否在售
     */
    public boolean isOnSale() {
        return this.status != null && this.status.isOnSale();
    }

    /**
     * 判断是否有可用库存
     */
    public boolean hasAvailableStock() {
        return getAvailableStock() > 0;
    }
}