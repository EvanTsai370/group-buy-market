package org.example.application.service.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.model.goods.Sku;
import org.example.domain.model.goods.Spu;
import org.example.domain.model.goods.repository.SkuRepository;
import org.example.domain.model.goods.repository.SpuRepository;
import org.example.domain.model.user.User;
import org.example.domain.model.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理后台统计服务
 * 
 * @author 开发团队
 * @since 2026-01-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminStatisticsService {

    private final UserRepository userRepository;
    private final SpuRepository spuRepository;
    private final SkuRepository skuRepository;

    /**
     * 获取仪表盘概览数据
     */
    public DashboardOverview getDashboardOverview() {
        log.info("【AdminStatistics】获取仪表盘概览");

        // 用户统计
        long totalUsers = userRepository.count();

        // 商品统计
        List<Spu> allSpus = spuRepository.findAll(1, 10000);
        List<Spu> onSaleSpus = spuRepository.findAllOnSale();
        List<Sku> allSkus = skuRepository.findAll(1, 10000);
        List<Sku> onSaleSkus = skuRepository.findAllOnSale();

        // 库存预警（库存 < 10 的SKU）
        long lowStockCount = allSkus.stream()
                .filter(sku -> sku.getAvailableStock() < 10)
                .count();

        return new DashboardOverview(
                totalUsers,
                allSpus.size(),
                onSaleSpus.size(),
                allSkus.size(),
                onSaleSkus.size(),
                lowStockCount,
                LocalDateTime.now());
    }

    /**
     * 获取用户统计
     */
    public UserStatistics getUserStatistics() {
        log.info("【AdminStatistics】获取用户统计");

        long totalUsers = userRepository.count();

        // 按角色统计
        Map<String, Long> roleDistribution = new HashMap<>();
        // TODO: 实现详细统计

        return new UserStatistics(totalUsers, roleDistribution);
    }

    /**
     * 获取商品统计
     */
    public GoodsStatistics getGoodsStatistics() {
        log.info("【AdminStatistics】获取商品统计");

        List<Spu> allSpus = spuRepository.findAll(1, 10000);
        List<Spu> onSaleSpus = spuRepository.findAllOnSale();
        List<Sku> allSkus = skuRepository.findAll(1, 10000);

        // 计算总库存
        int totalStock = allSkus.stream()
                .mapToInt(Sku::getStock)
                .sum();

        // 计算冻结库存
        int frozenStock = allSkus.stream()
                .mapToInt(Sku::getFrozenStock)
                .sum();

        // 低库存SKU列表
        List<Sku> lowStockSkus = allSkus.stream()
                .filter(sku -> sku.getAvailableStock() < 10)
                .toList();

        return new GoodsStatistics(
                allSpus.size(),
                onSaleSpus.size(),
                allSpus.size() - onSaleSpus.size(),
                allSkus.size(),
                totalStock,
                frozenStock,
                lowStockSkus);
    }

    /**
     * 仪表盘概览
     */
    public record DashboardOverview(
            long totalUsers,
            int totalSpus,
            int onSaleSpus,
            int totalSkus,
            int onSaleSkus,
            long lowStockCount,
            LocalDateTime updateTime) {
    }

    /**
     * 用户统计
     */
    public record UserStatistics(
            long totalUsers,
            Map<String, Long> roleDistribution) {
    }

    /**
     * 商品统计
     */
    public record GoodsStatistics(
            int totalSpus,
            int onSaleSpus,
            int offSaleSpus,
            int totalSkus,
            int totalStock,
            int frozenStock,
            List<Sku> lowStockSkus) {
    }
}
