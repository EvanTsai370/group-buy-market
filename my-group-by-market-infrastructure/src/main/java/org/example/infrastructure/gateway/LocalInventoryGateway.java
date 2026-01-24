package org.example.infrastructure.gateway;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.gateway.InventoryGateway;
import org.example.domain.model.goods.Sku;
import org.example.domain.model.goods.repository.SkuRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 库存网关本地实现
 * 
 * <p>
 * 使用本地 SKU 仓储实现库存操作
 * 生产环境可替换为外部库存服务实现
 * </p>
 * 
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocalInventoryGateway implements InventoryGateway {

    private final SkuRepository skuRepository;

    @Override
    public boolean freezeStock(String skuId, String orderId, int quantity) {
        log.info("【InventoryGateway】冻结库存, skuId: {}, orderId: {}, quantity: {}",
                skuId, orderId, quantity);

        int result = skuRepository.freezeStock(skuId, quantity);
        boolean success = result >= 0;
        log.info("【InventoryGateway】冻结库存结果: {}", success);
        return success;
    }

    @Override
    public boolean deductStock(String skuId, String orderId, int quantity) {
        log.info("【InventoryGateway】扣减库存, skuId: {}, orderId: {}, quantity: {}",
                skuId, orderId, quantity);

        int result = skuRepository.deductStock(skuId, quantity);
        boolean success = result >= 0;
        log.info("【InventoryGateway】扣减库存结果: {}", success);
        return success;
    }

    @Override
    public boolean releaseStock(String skuId, String orderId, int quantity) {
        log.info("【InventoryGateway】释放库存, skuId: {}, orderId: {}, quantity: {}",
                skuId, orderId, quantity);

        int result = skuRepository.unfreezeStock(skuId, quantity);
        boolean success = result >= 0;
        log.info("【InventoryGateway】释放库存结果: {}", success);
        return success;
    }

    @Override
    public int queryAvailableStock(String skuId) {
        log.info("【InventoryGateway】查询可用库存, skuId: {}", skuId);

        Optional<Sku> skuOpt = skuRepository.findBySkuId(skuId);

        if (skuOpt.isPresent()) {
            int availableStock = skuOpt.get().getAvailableStock();
            log.info("【InventoryGateway】可用库存: {}", availableStock);
            return availableStock;
        } else {
            log.warn("【InventoryGateway】SKU 不存在: {}", skuId);
            return 0;
        }
    }
}
