package org.example.domain.gateway;

/**
 * 库存网关接口（对接外部库存服务）
 * 
 * <p>
 * 定义在 Domain 层，由 Infrastructure 层实现
 * 用于对接外部电商系统的库存服务
 * </p>
 * 
 * @author 开发团队
 * @since 2026-01-10
 */
// TODO: 使用使用 MQ + 事件驱动架构
public interface InventoryGateway {

    /**
     * 冻结库存（锁单时调用）
     * 
     * @param goodsId  商品ID
     * @param orderId  订单ID
     * @param quantity 数量
     * @return 是否成功
     */
    boolean freezeStock(String goodsId, String orderId, int quantity);

    /**
     * 扣减库存（支付成功后调用）
     * 
     * @param goodsId  商品ID
     * @param orderId  订单ID
     * @param quantity 数量
     * @return 是否成功
     */
    boolean deductStock(String goodsId, String orderId, int quantity);

    /**
     * 释放库存（退单/超时时调用）
     * 
     * @param goodsId  商品ID
     * @param orderId  订单ID
     * @param quantity 数量
     * @return 是否成功
     */
    boolean releaseStock(String goodsId, String orderId, int quantity);

    /**
     * 查询可用库存
     * 
     * @param goodsId 商品ID
     * @return 可用库存数量
     */
    int queryAvailableStock(String goodsId);

    /**
     * 扣减库存（简化版，按SKU ID）
     * 
     * @param skuId    SKU ID
     * @param quantity 数量
     * @return 是否成功
     */
    default boolean deductStock(Long skuId, int quantity) {
        return deductStock(String.valueOf(skuId), null, quantity);
    }
}
