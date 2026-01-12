package org.example.domain.service.trial.loader;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.pattern.flow.DataLoader;
import org.example.domain.model.activity.TrialBalanceRequest;
import org.example.domain.model.activity.context.TrialBalanceContext;
import org.example.domain.model.goods.Sku;
import org.example.domain.model.goods.repository.SkuRepository;

/**
 * 商品信息数据加载器
 */
@Slf4j
@AllArgsConstructor
public class SkuDataLoader implements DataLoader<TrialBalanceRequest, TrialBalanceContext> {

    private final SkuRepository skuRepository;

    @Override
    public void loadData(TrialBalanceRequest request, TrialBalanceContext context) {
        log.info("【数据加载器】开始加载商品信息，skuId: {}", request.getSkuId());

        try {
            Sku sku = skuRepository.findBySkuId(request.getSkuId())
                .orElse(null);

            if (sku != null) {
                context.setSku(sku);
                log.info("【数据加载器】商品信息加载完成，skuId: {}, goodsName: {}",
                         sku.getSkuId(), sku.getGoodsName());
            } else {
                log.warn("【数据加载器】未找到商品信息，skuId: {}", request.getSkuId());
            }

        } catch (Exception e) {
            log.error("【数据加载器】商品信息加载失败，skuId: {}", request.getSkuId(), e);
            throw new RuntimeException("商品信息加载失败", e);
        }
    }
}
