# 交易订单功能实现文档

## 📋 文档信息

- **功能名称**: 交易订单管理（Trade Order Management）
- **完成日期**: 2026-01-04
- **开发团队**: 开发团队
- **架构模式**: DDD（领域驱动设计）

---

## 🎯 业务背景

在拼团营销系统中，交易订单（TradeOrder）是用户参与拼团活动的核心实体。每个用户参与拼团都会创建一个交易订单，记录用户的支付信息、优惠金额、订单状态等关键数据。

### 业务场景

1. **用户参与拼团**：用户选择商品和活动后，锁定优惠名额，创建交易订单
2. **支付流程**：用户完成支付后，更新订单状态为"已支付"
3. **拼团成功**：达到目标人数后，订单结算，状态变更为"已结算"
4. **拼团失败**：超时未成团或用户取消，触发退单流程

---

## 📊 核心业务流程

### 1. 锁单流程（Lock Order）

```
用户请求 → 规则过滤链 → 价格计算 → 创建/加入拼团 → 创建交易订单 → 返回结果
```

**关键步骤**：
1. 执行交易规则过滤链（活动可用性、用户参与限制、人群标签）
2. 加载活动和商品信息，计算优惠价格
3. 如果是新建拼团，创建Order聚合；如果是加入拼团，加载已有Order
4. 调用锁单领域服务，创建TradeOrder聚合
5. 原子增加Order的lockCount（防止超卖）
6. 返回交易订单信息给前端

### 2. 支付成功流程（Payment Success）

```
支付回调 → 标记已支付 → 增加完成人数 → 检查是否成团 → 触发结算
```

**关键步骤**：
1. 接收支付系统回调，获取交易订单ID
2. 标记TradeOrder为PAID状态
3. 原子增加Order的completeCount
4. 检查是否达到目标人数
5. 如果成团，触发结算流程

### 3. 结算流程（Settlement）

```
拼团成功 → 批量查询交易订单 → 标记已结算 → 触发通知
```

**关键步骤**：
1. 查询该Order下所有PAID状态的TradeOrder
2. 批量标记为SETTLED状态
3. 记录结算时间
4. 触发外部通知（如果配置）

### 4. 退单流程（Refund）

```
退单请求 → 校验状态 → 标记退单 → 释放锁定名额 → 触发退款通知
```

**关键步骤**：
1. 加载TradeOrder，校验是否可退单
2. 标记为REFUND状态
3. 原子减少Order的lockCount（释放名额）
4. 触发退款通知给支付系统

---

## 🏗️ 系统架构设计

### DDD分层架构

```
┌─────────────────────────────────────────────────────────┐
│  Interface Layer (接口层)                                │
│  - 处理HTTP请求和响应                                    │
│  - 参数验证和转换                                        │
└─────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────┐
│  Application Layer (应用层)                              │
│  - 业务流程编排                                          │
│  - 事务管理                                              │
│  - 领域服务协调                                          │
└─────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────┐
│  Domain Layer (领域层)                                   │
│  - 聚合根和实体                                          │
│  - 领域服务                                              │
│  - 业务规则封装                                          │
└─────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────┐
│  Infrastructure Layer (基础设施层)                       │
│  - 数据持久化                                            │
│  - 外部接口                                              │
│  - 技术支撑                                              │
└─────────────────────────────────────────────────────────┘
```

---

## 💻 代码实现清单

### 1. Interface Layer（接口层）

#### 1.1 Controller

**类**: `TradeOrderController`
- **路径**: `my-group-by-market-interfaces/src/main/java/org/example/interfaces/web/controller/TradeOrderController.java`
- **职责**: 提供交易订单相关的REST API接口

**方法列表**:
```java
// 锁单接口
public Result<TradeOrderVO> lockOrder(@RequestBody LockOrderCmd cmd)

// 支付成功回调接口
public Result<Void> handlePaymentSuccess(@PathVariable String tradeOrderId)

// 退单接口
public Result<Void> refundOrder(@PathVariable String tradeOrderId)

// 查询交易订单接口
public Result<TradeOrderVO> queryTradeOrder(@PathVariable String tradeOrderId)
```

---

### 2. Application Layer（应用层）

#### 2.1 Application Service

**类**: `TradeOrderService`
- **路径**: `my-group-by-market-application/src/main/java/org/example/application/service/trade/TradeOrderService.java`
- **职责**: 编排交易业务流程，协调多个聚合和领域服务

**方法列表**:
```java
// 锁单
public TradeOrderVO lockOrder(LockOrderCmd cmd)

// 支付成功回调
public void handlePaymentSuccess(String tradeOrderId)

// 退单
public void refundTradeOrder(String tradeOrderId)

// 查询交易订单
public TradeOrderVO queryTradeOrder(String tradeOrderId)
```

#### 2.2 Command Objects

**类**: `LockOrderCmd`
- **路径**: `my-group-by-market-application/src/main/java/org/example/application/service/trade/cmd/LockOrderCmd.java`
- **职责**: 锁单命令对象，封装锁单请求参数

**字段列表**:
```java
private String orderId;          // 订单ID（加入已有拼团时传入）
private String activityId;       // 活动ID
private String userId;           // 用户ID
private String goodsId;          // 商品ID
private String outTradeNo;       // 外部交易单号（幂等性保证）
private String source;           // 来源
private String channel;          // 渠道
private String notifyType;       // 通知类型（HTTP/MQ）
private String notifyUrl;        // HTTP回调地址
private String notifyMq;         // MQ主题
```

#### 2.3 View Objects

**类**: `TradeOrderVO`
- **路径**: `my-group-by-market-application/src/main/java/org/example/application/service/trade/vo/TradeOrderVO.java`
- **职责**: 交易订单视图对象，用于API响应

**字段列表**:
```java
private String tradeOrderId;       // 交易订单ID
private String teamId;             // 拼团队伍ID
private String orderId;            // 拼团订单ID
private String activityId;         // 活动ID
private String userId;             // 用户ID
private String goodsId;            // 商品ID
private String goodsName;          // 商品名称
private BigDecimal originalPrice;  // 原始价格
private BigDecimal deductionPrice; // 减免金额
private BigDecimal payPrice;       // 实付金额
private String status;             // 交易状态
private String outTradeNo;         // 外部交易单号
private LocalDateTime payTime;     // 支付时间
private LocalDateTime settlementTime; // 结算时间
private String source;             // 来源
private String channel;            // 渠道
private LocalDateTime createTime;  // 创建时间
private LocalDateTime updateTime;  // 更新时间
```

#### 2.4 Assembler

**类**: `TradeOrderAssembler`
- **路径**: `my-group-by-market-application/src/main/java/org/example/application/assembler/TradeOrderAssembler.java`
- **职责**: 使用MapStruct实现Domain对象与VO对象的转换

**方法列表**:
```java
// Domain → VO转换
TradeOrderVO toVO(TradeOrder tradeOrder)

// Domain列表 → VO列表转换
List<TradeOrderVO> toVOList(List<TradeOrder> tradeOrders)
```

---

### 3. Domain Layer（领域层）

#### 3.1 Aggregate Root

**类**: `TradeOrder`
- **路径**: `my-group-by-market-domain/src/main/java/org/example/domain/model/trade/TradeOrder.java`
- **职责**: 交易订单聚合根，封装交易订单的业务逻辑

**方法列表**:
```java
// 工厂方法：创建交易订单
public static TradeOrder create(...)

// 标记为已支付
public void markAsPaid(LocalDateTime payTime)

// 标记为已结算
public void markAsSettled(LocalDateTime settlementTime)

// 标记为已退单
public void markAsRefund()

// 判断是否可以结算
public boolean canSettle()

// 判断是否可以退单
public boolean canRefund()
```

**字段列表**:
```java
private String tradeOrderId;       // 交易订单ID
private String teamId;             // 队伍ID
private String orderId;            // 拼团订单ID
private String activityId;         // 活动ID
private String userId;             // 用户ID
private String goodsId;            // 商品ID
private String goodsName;          // 商品名称
private BigDecimal originalPrice;  // 原始价格
private BigDecimal deductionPrice; // 减免金额
private BigDecimal payPrice;       // 实付金额
private TradeStatus status;        // 交易状态
private String outTradeNo;         // 外部交易单号
private LocalDateTime payTime;     // 支付时间
private LocalDateTime settlementTime; // 结算时间
private String source;             // 来源
private String channel;            // 渠道
private NotifyConfig notifyConfig; // 通知配置
private LocalDateTime createTime;  // 创建时间
private LocalDateTime updateTime;  // 更新时间
```

#### 3.2 Value Objects

**类**: `TradeStatus`
- **路径**: `my-group-by-market-domain/src/main/java/org/example/domain/model/trade/valueobject/TradeStatus.java`
- **职责**: 交易状态枚举

**枚举值**:
```java
CREATE("CREATE", "已创建")      // 锁单后的初始状态
PAID("PAID", "已支付")         // 用户支付成功
SETTLED("SETTLED", "已结算")   // 拼团成功，订单结算
TIMEOUT("TIMEOUT", "已超时")   // 超时未支付
REFUND("REFUND", "已退单")     // 退款
```

**方法列表**:
```java
// 根据code获取枚举
public static TradeStatus fromCode(String code)

// 判断是否可以退单
public boolean canRefund()

// 判断是否是终态
public boolean isFinal()
```

**类**: `NotifyConfig`
- **路径**: `my-group-by-market-domain/src/main/java/org/example/domain/model/trade/valueobject/NotifyConfig.java`
- **职责**: 通知配置值对象

**字段列表**:
```java
private NotifyType notifyType;  // 通知类型（HTTP/MQ）
private String notifyUrl;       // HTTP回调地址
private String notifyMq;        // MQ主题
```

**类**: `NotifyType`
- **路径**: `my-group-by-market-domain/src/main/java/org/example/domain/model/trade/valueobject/NotifyType.java`
- **职责**: 通知类型枚举

**枚举值**:
```java
HTTP("HTTP", "HTTP回调")
MQ("MQ", "消息队列")
```

#### 3.3 Domain Services

**类**: `LockOrderService`
- **路径**: `my-group-by-market-domain/src/main/java/org/example/domain/service/LockOrderService.java`
- **职责**: 锁单领域服务，协调Order和TradeOrder聚合的锁单操作

**方法列表**:
```java
// 锁单
public TradeOrder lockOrder(
    String tradeOrderId,
    String orderId,
    String activityId,
    String userId,
    String goodsId,
    String goodsName,
    BigDecimal originalPrice,
    BigDecimal deductionPrice,
    BigDecimal payPrice,
    String outTradeNo,
    String source,
    String channel,
    NotifyConfig notifyConfig
)
```

**类**: `SettlementService`
- **路径**: `my-group-by-market-domain/src/main/java/org/example/domain/service/SettlementService.java`
- **职责**: 结算领域服务，处理支付成功和拼团成功的结算逻辑

**方法列表**:
```java
// 处理支付成功
public void handlePaymentSuccess(String tradeOrderId, LocalDateTime payTime)

// 结算已完成的拼团订单
public void settleCompletedOrder(String orderId)

// 批量结算超时订单
public void batchSettleOrders(List<String> orderIds)
```

**类**: `RefundService`
- **路径**: `my-group-by-market-domain/src/main/java/org/example/domain/service/RefundService.java`
- **职责**: 退单领域服务，处理订单退款逻辑

**方法列表**:
```java
// 退单
public void refundTradeOrder(String tradeOrderId)

// 批量退单（拼团失败场景）
public void batchRefundByOrder(String orderId)
```

#### 3.4 Repository Interfaces

**类**: `TradeOrderRepository`
- **路径**: `my-group-by-market-domain/src/main/java/org/example/domain/model/trade/repository/TradeOrderRepository.java`
- **职责**: 交易订单仓储接口，定义数据访问规范

**方法列表**:
```java
// 保存交易订单
void save(TradeOrder tradeOrder)

// 更新交易订单
void update(TradeOrder tradeOrder)

// 根据交易订单ID查询
Optional<TradeOrder> findByTradeOrderId(String tradeOrderId)

// 根据外部交易单号查询
Optional<TradeOrder> findByOutTradeNo(String outTradeNo)

// 根据订单ID查询交易订单列表
List<TradeOrder> findByOrderId(String orderId)

// 根据用户ID和活动ID查询
List<TradeOrder> findByUserIdAndActivityId(String userId, String activityId)

// 统计用户在某个活动下的参与次数
int countByUserIdAndActivityId(String userId, String activityId)
```

#### 3.5 Trade Filter Chain（交易规则过滤链）

**类**: `TradeFilterFactory`
- **路径**: `my-group-by-market-domain/src/main/java/org/example/domain/model/trade/filter/TradeFilterFactory.java`
- **职责**: 创建交易规则过滤链

**方法列表**:
```java
// 创建过滤链
public ChainExecutor<TradeFilterRequest, TradeFilterContext, TradeFilterResponse> createFilterChain()
```

**过滤器列表**:

1. **`ActivityAvailabilityHandler`** - 活动可用性过滤器
   - 校验活动是否存在
   - 校验活动状态是否为ACTIVE
   - 加载Activity并放入上下文

2. **`UserParticipationLimitHandler`** - 用户参与限制过滤器
   - 校验用户参与次数是否超限

3. **`TeamStockOccupyHandler`** -  组队库存占用规则处理器
   - 在高并发场景下,防止拼团组队超卖

**类**: `TradeFilterRequest`
- **路径**: `my-group-by-market-domain/src/main/java/org/example/domain/model/trade/filter/TradeFilterRequest.java`
- **职责**: 过滤链请求对象

**字段列表**:
```java
private String userId;
private String activityId;
private String goodsId;
private String orderId;
```

**类**: `TradeFilterContext`
- **路径**: `my-group-by-market-domain/src/main/java/org/example/domain/model/trade/filter/TradeFilterContext.java`
- **职责**: 过滤链上下文，存储中间状态

**字段列表**:
```java
private Activity activity;  // 活动信息
```

**类**: `TradeFilterResponse`
- **路径**: `my-group-by-market-domain/src/main/java/org/example/domain/model/trade/filter/TradeFilterResponse.java`
- **职责**: 过滤链响应对象

**字段列表**:
```java
private boolean allowed;  // 是否允许交易
private String reason;    // 拒绝原因
```

---

### 4. Infrastructure Layer（基础设施层）

#### 4.1 Persistence Objects

**类**: `TradeOrderPO`
- **路径**: `my-group-by-market-infrastructure/src/main/java/org/example/infrastructure/persistence/po/TradeOrderPO.java`
- **职责**: 交易订单持久化对象，映射数据库表结构

**字段列表**:
```java
private Long id;                   // 主键ID
private String tradeOrderId;       // 交易订单ID
private String teamId;             // 队伍ID
private String orderId;            // 拼团订单ID
private String activityId;         // 活动ID
private String userId;             // 用户ID
private String goodsId;            // 商品ID
private String goodsName;          // 商品名称
private BigDecimal originalPrice;  // 原始价格
private BigDecimal deductionPrice; // 减免金额
private BigDecimal payPrice;       // 实付金额
private String status;             // 交易状态
private String outTradeNo;         // 外部交易单号
private LocalDateTime payTime;     // 支付时间
private LocalDateTime settlementTime; // 结算时间
private String source;             // 来源
private String channel;            // 渠道
private String notifyType;         // 通知类型
private String notifyUrl;          // 通知URL
private String notifyMq;           // 通知MQ
private LocalDateTime createTime;  // 创建时间
private LocalDateTime updateTime;  // 更新时间
```

#### 4.2 Mappers

**类**: `TradeOrderMapper`
- **路径**: `my-group-by-market-infrastructure/src/main/java/org/example/infrastructure/persistence/mapper/TradeOrderMapper.java`
- **职责**: MyBatis-Plus Mapper接口，定义数据库操作

**方法列表**:
```java
// 根据外部交易单号查询
TradeOrderPO selectByOutTradeNo(@Param("outTradeNo") String outTradeNo)

// 根据用户ID和活动ID查询
List<TradeOrderPO> selectByUserIdAndActivityId(
    @Param("userId") String userId,
    @Param("activityId") String activityId
)

// 根据队伍ID查询
List<TradeOrderPO> selectByTeamId(@Param("teamId") String teamId)

// 根据订单ID查询
List<TradeOrderPO> selectByOrderId(@Param("orderId") String orderId)

// 统计用户参与次数
int countByUserIdAndActivityId(
    @Param("userId") String userId,
    @Param("activityId") String activityId
)
```

**XML配置**: `TradeOrderMapper.xml`
- **路径**: `my-group-by-market-start/src/main/resources/mybatis/mapper/TradeOrderMapper.xml`
- **职责**: 定义SQL语句，包含详细的业务注释和性能说明

#### 4.3 Converters

**类**: `TradeOrderConverter`
- **路径**: `my-group-by-market-infrastructure/src/main/java/org/example/infrastructure/persistence/converter/TradeOrderConverter.java`
- **职责**: 使用MapStruct实现PO与Domain对象的转换

**方法列表**:
```java
// Domain → PO
TradeOrderPO toPO(TradeOrder tradeOrder)

// PO → Domain
TradeOrder toDomain(TradeOrderPO po)

// PO列表 → Domain列表
List<TradeOrder> toDomainList(List<TradeOrderPO> poList)
```

#### 4.4 Repository Implementations

**类**: `TradeOrderRepositoryImpl`
- **路径**: `my-group-by-market-infrastructure/src/main/java/org/example/infrastructure/persistence/repository/TradeOrderRepositoryImpl.java`
- **职责**: 实现TradeOrderRepository接口，处理数据持久化

**方法列表**:
```java
// 保存
public void save(TradeOrder tradeOrder)

// 更新
public void update(TradeOrder tradeOrder)

// 根据交易订单ID查询
public Optional<TradeOrder> findByTradeOrderId(String tradeOrderId)

// 根据外部交易单号查询
public Optional<TradeOrder> findByOutTradeNo(String outTradeNo)

// 根据订单ID查询列表
public List<TradeOrder> findByOrderId(String orderId)

// 根据用户ID和活动ID查询
public List<TradeOrder> findByUserIdAndActivityId(String userId, String activityId)

// 统计参与次数
public int countByUserIdAndActivityId(String userId, String activityId)
```

#### 4.5 Configuration

**类**: `DomainServiceConfiguration`
- **路径**: `my-group-by-market-infrastructure/src/main/java/org/example/infrastructure/config/DomainServiceConfiguration.java`
- **职责**: 将Domain层的领域服务注册为Spring Bean

**Bean定义**:
```java
// 锁单领域服务
@Bean
public LockOrderService lockOrderService(...)

// 结算领域服务
@Bean
public SettlementService settlementService(...)

// 退单领域服务
@Bean
public RefundService refundService(...)

// 折扣计算器
@Bean
public DiscountCalculator discountCalculator(...)
```

---

## 🗄️ 数据库设计

### trade_order 表结构

| 字段名 | 类型 | 说明 | 索引 |
|-------|------|------|------|
| id | BIGINT | 主键ID | PRIMARY |
| trade_order_id | VARCHAR(32) | 交易订单ID | UNIQUE |
| team_id | VARCHAR(32) | 队伍ID | INDEX |
| order_id | VARCHAR(32) | 拼团订单ID | INDEX |
| activity_id | VARCHAR(32) | 活动ID | INDEX |
| user_id | VARCHAR(32) | 用户ID | INDEX |
| goods_id | VARCHAR(32) | 商品ID | INDEX |
| goods_name | VARCHAR(128) | 商品名称 | - |
| original_price | DECIMAL(10,2) | 原始价格 | - |
| deduction_price | DECIMAL(10,2) | 减免金额 | - |
| pay_price | DECIMAL(10,2) | 实付金额 | - |
| status | VARCHAR(16) | 交易状态 | INDEX |
| out_trade_no | VARCHAR(64) | 外部交易单号 | UNIQUE |
| pay_time | DATETIME | 支付时间 | - |
| settlement_time | DATETIME | 结算时间 | - |
| source | VARCHAR(32) | 来源 | - |
| channel | VARCHAR(32) | 渠道 | - |
| notify_type | VARCHAR(16) | 通知类型 | - |
| notify_url | VARCHAR(256) | 通知URL | - |
| notify_mq | VARCHAR(128) | 通知MQ | - |
| create_time | DATETIME | 创建时间 | - |
| update_time | DATETIME | 更新时间 | - |

### 核心索引说明

1. **idx_out_trade_no（唯一索引）**: 用于幂等性校验，防止重复提交
2. **idx_user_activity（组合索引）**: 用于查询用户参与次数
3. **idx_order_id**: 用于查询拼团下的所有交易订单
4. **idx_team_id**: 用于查询队伍成员
5. **idx_status**: 用于批量查询特定状态的订单

---

## 🔐 安全性设计

### 1. 幂等性保证

**机制**: 使用外部交易单号（outTradeNo）作为唯一标识
```java
// 锁单前检查是否已存在
Optional<TradeOrder> existing = tradeOrderRepository.findByOutTradeNo(outTradeNo);
if (existing.isPresent()) {
    return existing.get();  // 返回已有订单，保证幂等
}
```

### 2. 并发控制

**机制**: 使用数据库行锁和原子操作
```java
// Order的lockCount原子增加，防止超卖
int affectedRows = orderRepository.tryIncrementLockCount(orderId);
if (affectedRows == 0) {
    throw new BizException("拼团已满或已超时");
}
```

### 3. 状态机保护

**规则**: 严格控制状态流转，防止非法状态变更
```java
// 只有CREATE和PAID状态才能退单
public boolean canRefund() {
    return this == CREATE || this == PAID;
}

// 只有PAID状态才能结算
public boolean canSettle() {
    return this == PAID;
}
```

---

## 🎨 设计模式应用

### 1. DDD战术模式

- **聚合根**: TradeOrder
- **值对象**: TradeStatus, NotifyConfig, NotifyType
- **领域服务**: LockOrderService, SettlementService, RefundService
- **仓储模式**: TradeOrderRepository

### 2. 责任链模式

**应用场景**: 交易规则过滤

```
TradeFilterChain:
  ActivityAvailabilityFilter
    → UserParticipationLimitFilter
      → CrowdTagFilter
```

### 3. 工厂模式

**应用场景**: 聚合对象创建

```java
public static TradeOrder create(...) {
    // 业务规则验证
    // 初始化状态
    // 返回聚合实例
}
```

### 4. 策略模式

**应用场景**: 折扣计算

```java
DiscountCalculator
  ├── DirectDiscountCalculator      // 直接减免
  ├── PercentageDiscountCalculator  // 百分比折扣
  ├── FixedPriceDiscountCalculator  // 固定价格
  └── FullReductionDiscountCalculator // 满减
```

### 5. 状态模式

**应用场景**: 交易状态管理

```
TradeStatus枚举 + canRefund()/canSettle() 方法
```

---

## 📈 性能优化

### 1. 数据库优化

- **组合索引**: `(user_id, activity_id)` 用于高频查询
- **唯一索引**: `out_trade_no` 用于幂等性校验
- **原子操作**: 使用SQL的`UPDATE ... WHERE ...`避免乐观锁冲突

### 2. 查询优化

```sql
-- 避免全表扫描，使用索引
SELECT * FROM trade_order
WHERE user_id = ? AND activity_id = ?
ORDER BY create_time DESC
```

### 3. 批量操作

```java
// 批量结算，减少数据库交互
public void settleCompletedOrder(String orderId) {
    List<TradeOrder> tradeOrders = tradeOrderRepository.findByOrderId(orderId);
    // 批量更新
}
```

---

## 🧪 测试建议

### 单元测试

1. **聚合测试**: TradeOrder的状态流转逻辑
2. **领域服务测试**: LockOrderService、SettlementService的业务逻辑
3. **过滤器测试**: 各个过滤器的规则校验

### 集成测试

1. **锁单流程**: 端到端测试锁单流程
2. **支付回调**: 模拟支付系统回调
3. **拼团成团**: 测试多用户参与拼团场景
4. **退单流程**: 测试各种退单场景

### 压力测试

1. **并发锁单**: 模拟高并发下的超卖问题
2. **幂等性**: 测试重复请求的幂等性
3. **数据库性能**: 测试大数据量下的查询性能

---

## 🚀 部署说明

### 环境要求

- **JDK**: 21+
- **Spring Boot**: 3.2.0
- **MySQL**: 8.2.0+
- **Redis**: 7.0+（用于人群标签缓存）

### 配置项

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/group_buy_market
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

### 数据库初始化

执行脚本: `my-group-by-market-start/src/main/resources/db/migration/V1__init_schema.sql`

---

## 📝 接口文档

### API列表

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 锁单 | POST | `/api/trade/lock` | 创建交易订单 |
| 支付回调 | POST | `/api/trade/payment/success/{tradeOrderId}` | 支付成功通知 |
| 退单 | POST | `/api/trade/refund/{tradeOrderId}` | 申请退单 |
| 查询 | GET | `/api/trade/{tradeOrderId}` | 查询订单详情 |

### Swagger文档

访问地址: `http://localhost:8080/doc.html`

---

## 🔄 后续优化方向

### 1. 功能增强

- [ ] 支持部分退款
- [ ] 添加订单超时自动退单机制
- [ ] 实现通知重试机制
- [ ] 支持订单评价功能

### 2. 性能优化

- [ ] 引入Redis缓存热点数据
- [ ] 优化数据库查询（分页、索引优化）
- [ ] 引入消息队列解耦支付回调
- [ ] 实现读写分离

### 3. 可观测性

- [ ] 添加链路追踪（Sleuth + Zipkin）
- [ ] 完善日志体系（ELK）
- [ ] 添加业务监控指标（Prometheus + Grafana）

---

## 📚 参考资料

- **DDD**: 《领域驱动设计》- Eric Evans
- **微服务**: 《微服务架构设计模式》- Chris Richardson
- **代码规范**: 《阿里巴巴Java开发手册》

---

## 👥 维护者

- **开发团队**: 开发团队
- **创建日期**: 2026-01-04
- **最后更新**: 2026-01-05

---

**文档版本**: v1.0.0
