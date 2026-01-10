package org.example.domain.model.goods.valueobject;

/**
 * 商品状态
 */
public enum GoodsStatus {

    ON_SALE("在售"),
    OFF_SALE("下架");

    private final String desc;

    GoodsStatus(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    public boolean isOnSale() {
        return this == ON_SALE;
    }
}
