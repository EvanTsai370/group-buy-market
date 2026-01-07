package org.example.application.service.trade;

import org.example.application.assembler.TradeOrderAssembler;
import org.example.application.service.trade.cmd.LockOrderCmd;
import org.example.application.service.trade.vo.TradeOrderVO;
import org.example.domain.model.account.Account;
import org.example.domain.model.account.repository.AccountRepository;
import org.example.domain.model.activity.Activity;
import org.example.domain.model.activity.Discount;
import org.example.domain.model.activity.repository.ActivityRepository;
import org.example.domain.model.activity.valueobject.ActivityStatus;
import org.example.domain.model.activity.valueobject.GroupType;
import org.example.domain.model.activity.valueobject.TagScope;
import org.example.domain.model.goods.Sku;
import org.example.domain.model.goods.repository.SkuRepository;
import org.example.domain.model.order.Order;
import org.example.domain.model.order.repository.OrderRepository;
import org.example.domain.model.trade.TradeOrder;
import org.example.domain.model.trade.repository.TradeOrderRepository;
import org.example.domain.service.LockOrderService;
import org.example.domain.service.RefundService;
import org.example.domain.service.SettlementService;
import org.example.domain.service.discount.DiscountCalculator;
import org.example.domain.shared.IdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TradeOrderService 幂等性和并发测试
 * 
 * <p>
 * 测试目标：
 * <ul>
 * <li>验证幂等性：相同 outTradeNo 返回相同结果</li>
 * <li>验证 createOrderIfNeeded 中的 outTradeNo 检查逻辑</li>
 * </ul>
 *
 * @author 开发团队
 * @since 2026-01-06
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TradeOrderService 幂等性测试")
class TradeOrderServiceIdempotencyTest {

        @Mock
        private ActivityRepository activityRepository;
        @Mock
        private SkuRepository skuRepository;
        @Mock
        private OrderRepository orderRepository;
        @Mock
        private TradeOrderRepository tradeOrderRepository;
        @Mock
        private AccountRepository accountRepository;
        @Mock
        private IdGenerator idGenerator;
        @Mock
        private DiscountCalculator discountCalculator;
        @Mock
        private LockOrderService lockOrderService;
        @Mock
        private SettlementService settlementService;
        @Mock
        private RefundService refundService;
        @Mock
        private TradeOrderAssembler tradeOrderAssembler;

        private Map<String, DiscountCalculator> discountCalculatorMap;
        private TradeOrderService tradeOrderService;

        @BeforeEach
        void setUp() {
                // 使用真实的 HashMap
                discountCalculatorMap = new HashMap<>();
                discountCalculatorMap.put("ZJ", discountCalculator);

                tradeOrderService = new TradeOrderService(
                                activityRepository,
                                skuRepository,
                                orderRepository,
                                tradeOrderRepository,
                                accountRepository,
                                idGenerator,
                                discountCalculatorMap,
                                lockOrderService,
                                settlementService,
                                refundService,
                                tradeOrderAssembler);
        }

        @Test
        @DisplayName("幂等性测试：第一层检查 - lockOrder开头的outTradeNo检查")
        void testIdempotency_FirstLayerCheck() {
                // 准备测试数据
                String outTradeNo = "TEST_OUT_TRADE_NO_" + System.currentTimeMillis();
                String userId = "test_user_001";
                String activityId = "ACT001";
                String goodsId = "GOODS001";
                String orderId = "ORDER001";
                String tradeOrderId = "TRD001";

                LockOrderCmd cmd = createTestLockOrderCmd(userId, activityId, goodsId, null, outTradeNo);

                // Mock TradeOrder（已存在）
                TradeOrder existingTradeOrder = new TradeOrder();
                existingTradeOrder.setTradeOrderId(tradeOrderId);
                existingTradeOrder.setOrderId(orderId);
                existingTradeOrder.setOutTradeNo(outTradeNo);

                // Mock 第一次调用就返回已存在的订单（第一层检查）
                when(tradeOrderRepository.findByOutTradeNo(outTradeNo))
                                .thenReturn(Optional.of(existingTradeOrder));

                TradeOrderVO vo = new TradeOrderVO();
                vo.setTradeOrderId(tradeOrderId);
                vo.setOrderId(orderId);
                when(tradeOrderAssembler.toVO(existingTradeOrder)).thenReturn(vo);

                // 执行测试
                TradeOrderVO result = tradeOrderService.lockOrder(cmd);

                // 验证返回已有订单
                assertThat(result.getTradeOrderId()).isEqualTo(tradeOrderId);
                assertThat(result.getOrderId()).isEqualTo(orderId);

                // 验证没有创建新的 Order（第一层检查生效，直接返回）
                verify(orderRepository, never()).nextId();
                verify(orderRepository, never()).save(any(Order.class));

                // 验证没有调用锁单服务
                verify(lockOrderService, never()).lockOrder(
                                anyString(), anyString(), anyString(), anyString(), anyString(),
                                anyString(), any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class),
                                anyString(), anyString(), anyString(), any());

                // 验证没有加载活动、商品等（第一层检查生效，提前返回）
                verify(activityRepository, never()).findById(anyString());
                verify(skuRepository, never()).findByGoodsId(anyString());
        }

        @Test
        @DisplayName("幂等性测试：第二层检查 - createOrderIfNeeded中的outTradeNo检查")
        void testIdempotency_SecondLayerCheck() {
                // 准备测试数据
                String outTradeNo = "TEST_OUT_TRADE_NO_" + System.currentTimeMillis();
                String userId = "test_user_002";
                String activityId = "ACT002";
                String goodsId = "GOODS002";
                String existingOrderId = "EXISTING_ORDER_002";
                String tradeOrderId = "TRD002";

                LockOrderCmd cmd = createTestLockOrderCmd(userId, activityId, goodsId, null, outTradeNo);

                // Mock 活动数据
                Activity activity = Activity.create(
                                activityId,
                                "测试活动",
                                "DIS002",
                                null,
                                TagScope.OPEN,
                                GroupType.VIRTUAL,
                                3,
                                1200,
                                5,
                                LocalDateTime.now().minusDays(1),
                                LocalDateTime.now().plusDays(7));
                activity.setStatus(ActivityStatus.ACTIVE);
                when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));

                // Mock 商品数据
                Sku sku = new Sku();
                sku.setGoodsId(goodsId);
                sku.setGoodsName("测试商品");
                sku.setOriginalPrice(new BigDecimal("100.00"));
                when(skuRepository.findByGoodsId(goodsId)).thenReturn(Optional.of(sku));

                // Mock 折扣数据
                Discount discount = new Discount();
                discount.setDiscountId("DIS002");
                discount.setMarketPlan("ZJ");
                when(activityRepository.queryDiscountById("DIS002")).thenReturn(discount);
                when(discountCalculator.calculate(eq(userId), any(BigDecimal.class), eq(discount)))
                                .thenReturn(new BigDecimal("80.00"));

                // Mock Account
                Account account = Account.create("ACC002", userId, activityId, null);
                when(accountRepository.findByUserAndActivity(userId, activityId))
                                .thenReturn(Optional.of(account));

                // 关键 Mock：模拟第二层检查
                // 第一次调用 findByOutTradeNo（lockOrder 开头）：不存在
                // 第二次调用 findByOutTradeNo（createOrderIfNeeded 中）：已存在
                TradeOrder existingTradeOrder = new TradeOrder();
                existingTradeOrder.setTradeOrderId(tradeOrderId);
                existingTradeOrder.setOrderId(existingOrderId);
                existingTradeOrder.setOutTradeNo(outTradeNo);

                when(tradeOrderRepository.findByOutTradeNo(outTradeNo))
                                .thenReturn(Optional.empty()) // 第一次：不存在
                                .thenReturn(Optional.of(existingTradeOrder)); // 第二次：已存在（第二层检查）

                when(lockOrderService.lockOrder(
                                anyString(), eq(existingOrderId), eq(activityId), eq(userId), eq(goodsId),
                                anyString(), any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class),
                                eq(outTradeNo), anyString(), anyString(), any())).thenReturn(existingTradeOrder);

                TradeOrderVO vo = new TradeOrderVO();
                vo.setTradeOrderId(tradeOrderId);
                vo.setOrderId(existingOrderId);
                when(tradeOrderAssembler.toVO(existingTradeOrder)).thenReturn(vo);

                // 执行测试
                TradeOrderVO result = tradeOrderService.lockOrder(cmd);

                // 验证返回已有订单
                assertThat(result.getTradeOrderId()).isEqualTo(tradeOrderId);
                assertThat(result.getOrderId()).isEqualTo(existingOrderId);

                // 验证没有创建新的 Order（第二层检查生效）
                verify(orderRepository, never()).nextId();
                verify(orderRepository, never()).save(any(Order.class));

                // 验证调用了两次 findByOutTradeNo（第一层 + 第二层）
                verify(tradeOrderRepository, times(2)).findByOutTradeNo(outTradeNo);

                // 验证调用了锁单服务（使用已有的 orderId）
                verify(lockOrderService, times(1)).lockOrder(
                                anyString(), eq(existingOrderId), eq(activityId), eq(userId), eq(goodsId),
                                anyString(), any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class),
                                eq(outTradeNo), anyString(), anyString(), any());
        }

        @Test
        @DisplayName("正常流程测试：orderId为空时创建新Order")
        void testNormalFlow_CreateNewOrder() {
                // 准备测试数据
                String outTradeNo = "TEST_OUT_TRADE_NO_" + System.currentTimeMillis();
                String userId = "test_user_003";
                String activityId = "ACT003";
                String goodsId = "GOODS003";
                String newOrderId = "NEW_ORDER_003";
                String tradeOrderId = "TRD003";

                LockOrderCmd cmd = createTestLockOrderCmd(userId, activityId, goodsId, null, outTradeNo);

                // Mock 活动数据
                Activity activity = Activity.create(
                                activityId,
                                "测试活动",
                                "DIS003",
                                null,
                                TagScope.OPEN,
                                GroupType.VIRTUAL,
                                3,
                                1200,
                                5,
                                LocalDateTime.now().minusDays(1),
                                LocalDateTime.now().plusDays(7));
                activity.setStatus(ActivityStatus.ACTIVE);
                when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));

                // Mock 商品数据
                Sku sku = new Sku();
                sku.setGoodsId(goodsId);
                sku.setGoodsName("测试商品");
                sku.setOriginalPrice(new BigDecimal("100.00"));
                when(skuRepository.findByGoodsId(goodsId)).thenReturn(Optional.of(sku));

                // Mock 折扣数据
                Discount discount = new Discount();
                discount.setDiscountId("DIS003");
                discount.setMarketPlan("ZJ");
                when(activityRepository.queryDiscountById("DIS003")).thenReturn(discount);
                when(discountCalculator.calculate(eq(userId), any(BigDecimal.class), eq(discount)))
                                .thenReturn(new BigDecimal("80.00"));

                // Mock Account
                Account account = Account.create("ACC003", userId, activityId, null);
                when(accountRepository.findByUserAndActivity(userId, activityId))
                                .thenReturn(Optional.of(account));

                // Mock Order 创建
                when(orderRepository.nextId()).thenReturn(newOrderId);
                when(idGenerator.nextId()).thenReturn(12345678L);

                // Mock TradeOrder（不存在）
                when(tradeOrderRepository.findByOutTradeNo(outTradeNo))
                                .thenReturn(Optional.empty());

                TradeOrder newTradeOrder = new TradeOrder();
                newTradeOrder.setTradeOrderId(tradeOrderId);
                newTradeOrder.setOrderId(newOrderId);
                newTradeOrder.setOutTradeNo(outTradeNo);

                when(lockOrderService.lockOrder(
                                anyString(), eq(newOrderId), eq(activityId), eq(userId), eq(goodsId),
                                anyString(), any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class),
                                eq(outTradeNo), anyString(), anyString(), any())).thenReturn(newTradeOrder);

                TradeOrderVO vo = new TradeOrderVO();
                vo.setTradeOrderId(tradeOrderId);
                vo.setOrderId(newOrderId);
                when(tradeOrderAssembler.toVO(newTradeOrder)).thenReturn(vo);

                // 执行测试
                TradeOrderVO result = tradeOrderService.lockOrder(cmd);

                // 验证返回新订单
                assertThat(result.getTradeOrderId()).isEqualTo(tradeOrderId);
                assertThat(result.getOrderId()).isEqualTo(newOrderId);

                // 验证创建了新的 Order
                verify(orderRepository, times(1)).nextId();
                verify(orderRepository, times(1)).save(any(Order.class));

                // 验证调用了锁单服务
                verify(lockOrderService, times(1)).lockOrder(
                                anyString(), eq(newOrderId), eq(activityId), eq(userId), eq(goodsId),
                                anyString(), any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class),
                                eq(outTradeNo), anyString(), anyString(), any());
        }

        @Test
        @DisplayName("正常流程测试：orderId不为空时直接使用")
        void testNormalFlow_UseExistingOrderId() {
                // 准备测试数据
                String outTradeNo = "TEST_OUT_TRADE_NO_" + System.currentTimeMillis();
                String userId = "test_user_004";
                String activityId = "ACT004";
                String goodsId = "GOODS004";
                String existingOrderId = "EXISTING_ORDER_004";
                String tradeOrderId = "TRD004";

                // orderId 不为空（用户参团，不是团长）
                LockOrderCmd cmd = createTestLockOrderCmd(userId, activityId, goodsId, existingOrderId, outTradeNo);

                // Mock 活动数据
                Activity activity = Activity.create(
                                activityId,
                                "测试活动",
                                "DIS004",
                                null,
                                TagScope.OPEN,
                                GroupType.VIRTUAL,
                                3,
                                1200,
                                5,
                                LocalDateTime.now().minusDays(1),
                                LocalDateTime.now().plusDays(7));
                activity.setStatus(ActivityStatus.ACTIVE);
                when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));

                // Mock 商品数据
                Sku sku = new Sku();
                sku.setGoodsId(goodsId);
                sku.setGoodsName("测试商品");
                sku.setOriginalPrice(new BigDecimal("100.00"));
                when(skuRepository.findByGoodsId(goodsId)).thenReturn(Optional.of(sku));

                // Mock 折扣数据
                Discount discount = new Discount();
                discount.setDiscountId("DIS004");
                discount.setMarketPlan("ZJ");
                when(activityRepository.queryDiscountById("DIS004")).thenReturn(discount);
                when(discountCalculator.calculate(eq(userId), any(BigDecimal.class), eq(discount)))
                                .thenReturn(new BigDecimal("80.00"));

                // Mock Account
                Account account = Account.create("ACC004", userId, activityId, null);
                when(accountRepository.findByUserAndActivity(userId, activityId))
                                .thenReturn(Optional.of(account));

                // Mock Order（参团场景，Order 必须存在）
                Order existingOrder = mock(Order.class);
                when(existingOrder.getOrderId()).thenReturn(existingOrderId);
                when(existingOrder.getTargetCount()).thenReturn(3);
                when(existingOrder.getLockCount()).thenReturn(1); // 还有空位
                when(orderRepository.findById(existingOrderId)).thenReturn(Optional.of(existingOrder));

                // Mock TradeOrder（不存在）
                when(tradeOrderRepository.findByOutTradeNo(outTradeNo))
                                .thenReturn(Optional.empty());

                TradeOrder newTradeOrder = new TradeOrder();
                newTradeOrder.setTradeOrderId(tradeOrderId);
                newTradeOrder.setOrderId(existingOrderId);
                newTradeOrder.setOutTradeNo(outTradeNo);

                when(lockOrderService.lockOrder(
                                anyString(), eq(existingOrderId), eq(activityId), eq(userId), eq(goodsId),
                                anyString(), any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class),
                                eq(outTradeNo), anyString(), anyString(), any())).thenReturn(newTradeOrder);

                TradeOrderVO vo = new TradeOrderVO();
                vo.setTradeOrderId(tradeOrderId);
                vo.setOrderId(existingOrderId);
                when(tradeOrderAssembler.toVO(newTradeOrder)).thenReturn(vo);

                // 执行测试
                TradeOrderVO result = tradeOrderService.lockOrder(cmd);

                // 验证返回订单
                assertThat(result.getTradeOrderId()).isEqualTo(tradeOrderId);
                assertThat(result.getOrderId()).isEqualTo(existingOrderId);

                // 验证没有创建新的 Order（orderId 不为空，直接使用）
                verify(orderRepository, never()).nextId();
                verify(orderRepository, never()).save(any(Order.class));

                // 验证调用了锁单服务（使用已有的 orderId）
                verify(lockOrderService, times(1)).lockOrder(
                                anyString(), eq(existingOrderId), eq(activityId), eq(userId), eq(goodsId),
                                anyString(), any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class),
                                eq(outTradeNo), anyString(), anyString(), any());
        }

        /**
         * 创建测试用的 LockOrderCmd
         */
        private LockOrderCmd createTestLockOrderCmd(String userId, String activityId,
                        String goodsId, String orderId, String outTradeNo) {
                LockOrderCmd cmd = new LockOrderCmd();
                cmd.setUserId(userId);
                cmd.setActivityId(activityId);
                cmd.setGoodsId(goodsId);
                cmd.setOrderId(orderId);
                cmd.setOutTradeNo(outTradeNo);
                cmd.setOriginalPrice(new BigDecimal("100.00"));
                cmd.setDeductionPrice(new BigDecimal("20.00"));
                cmd.setPayPrice(new BigDecimal("80.00"));
                cmd.setSource("s01");
                cmd.setChannel("c01");
                return cmd;
        }
}
