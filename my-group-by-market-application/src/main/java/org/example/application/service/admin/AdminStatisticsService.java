package org.example.application.service.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.application.assembler.UserResultAssembler;
import org.example.application.service.admin.result.DashboardOverviewResult;
import org.example.application.service.admin.result.GoodsStatisticsResult;
import org.example.application.service.admin.result.SkuStatisticsInfo;
import org.example.application.service.admin.result.UserStatisticsResult;
import org.example.domain.model.goods.Sku;
import org.example.domain.model.goods.Spu;
import org.example.domain.model.goods.repository.SkuRepository;
import org.example.domain.model.goods.repository.SpuRepository;
import org.example.domain.model.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理后台统计服务
 * 
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminStatisticsService {

        private final UserRepository userRepository;
        private final SpuRepository spuRepository;
        private final SkuRepository skuRepository;
        private final org.example.domain.model.trade.repository.TradeOrderRepository tradeOrderRepository;
        private final org.example.domain.model.activity.repository.ActivityRepository activityRepository;
        private final UserResultAssembler userResultAssembler;
        private final org.example.application.assembler.TradeOrderResultAssembler tradeOrderResultAssembler;

        /**
         * 获取仪表盘概览数据
         */
        public DashboardOverviewResult getDashboardOverview() {
                log.info("【AdminStatistics】获取仪表盘概览");

                LocalDateTime now = LocalDateTime.now();
                LocalDateTime todayStart = LocalDateTime.of(now.toLocalDate(), LocalTime.MIN);
                LocalDateTime todayEnd = LocalDateTime.of(now.toLocalDate(), LocalTime.MAX);

                // 用户统计
                long totalUsers = userRepository.count();

                // 商品统计
                List<Spu> allSpus = spuRepository.findAll(1, 10000).getList();
                List<Spu> onSaleSpus = spuRepository.findAllOnSale();
                List<Sku> allSkus = skuRepository.findAll(1, 10000);
                List<Sku> onSaleSkus = skuRepository.findAllOnSale();

                // 库存预警（库存 < 10 的SKU）
                long lowStockCount = allSkus.stream()
                                .filter(sku -> sku.getAvailableStock() < 10)
                                .count();

                // 统计今日数据
                long todayOrders = tradeOrderRepository.countByCreateTimeBetween(todayStart, todayEnd);
                BigDecimal todayGMV = tradeOrderRepository.sumPayPriceByPayTimeBetween(todayStart, todayEnd);
                if (todayGMV == null) {
                        todayGMV = BigDecimal.ZERO;
                }
                long todayUsers = userRepository.countByCreateTimeBetween(todayStart, todayEnd);
                long activeActivities = activityRepository.countActive(now);

                // 获取最近订单
                List<org.example.domain.model.trade.TradeOrder> recentOrderDomains = tradeOrderRepository
                                .findLatest(10);
                List<org.example.application.service.admin.result.TradeOrderResult> recentOrders = recentOrderDomains
                                .stream()
                                .map(tradeOrderResultAssembler::toAdminResult)
                                .toList();

                return DashboardOverviewResult.builder()
                                .totalUsers(totalUsers)
                                .totalSpus(allSpus.size())
                                .onSaleSpus(onSaleSpus.size())
                                .totalSkus(allSkus.size())
                                .onSaleSkus(onSaleSkus.size())
                                .lowStockCount(lowStockCount)
                                .todayOrders(todayOrders)
                                .todayGMV(todayGMV)
                                .todayUsers(todayUsers)
                                .activeActivities(activeActivities)
                                .recentOrders(recentOrders)
                                .updateTime(LocalDateTime.now())
                                .build();
        }

        /**
         * 获取用户统计
         */
        public UserStatisticsResult getUserStatistics() {
                log.info("【AdminStatistics】获取用户统计");

                long totalUsers = userRepository.count();

                // 按角色统计
                Map<String, Long> roleDistribution = new HashMap<>();
                // TODO: 实现详细统计

                return UserStatisticsResult.builder()
                                .totalUsers(totalUsers)
                                .roleDistribution(roleDistribution)
                                .build();
        }

        /**
         * 获取商品统计
         */
        public GoodsStatisticsResult getGoodsStatistics() {
                log.info("【AdminStatistics】获取商品统计");

                List<Spu> allSpus = spuRepository.findAll(1, 10000).getList();
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
                List<Sku> lowStockSkuDomains = allSkus.stream()
                                .filter(sku -> sku.getAvailableStock() < 10)
                                .toList();

                // 转换为统计信息对象
                List<SkuStatisticsInfo> lowStockSkus = userResultAssembler.toSkuStatisticsInfoList(lowStockSkuDomains);

                return GoodsStatisticsResult.builder()
                                .totalSpus(allSpus.size())
                                .onSaleSpus(onSaleSpus.size())
                                .offSaleSpus(allSpus.size() - onSaleSpus.size())
                                .totalSkus(allSkus.size())
                                .totalStock(totalStock)
                                .frozenStock(frozenStock)
                                .lowStockSkus(lowStockSkus)
                                .build();
        }
}
