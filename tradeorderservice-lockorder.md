## TradeOrderService.lockOrder() 安全性与功能完整性分析

---

## 执行摘要

从项目整体视角分析 `TradeOrderService.lockOrder()` 方法，发现 **7 个关键问题**，其中 **3 个高危漏洞** 可能直接导致 **业务逻辑错误** 和 **数据不一致**，需要优先修复。

---

## 问题清单

### 🔴 高危问题

---

### 1. 缺少 Account 参团次数扣减（Critical）

#### 问题描述

* `lockOrder()` 方法**没有调用** `Account.deductCount()` 扣减用户的参团次数
* 虽然过滤链可能校验了次数限制，但**并未实际扣减**
* 用户可以无限次参与同一活动

#### 影响

* 用户可绕过 `participationLimit` 限制
* 活动参与次数控制完全失效
* 可能导致营销成本失控

#### 证据

* `Account.java:69–90`
* `deductCount()` 负责扣减次数并发出领域事件
* `lockOrder()` 中未调用该方法

#### 修复建议

```java
// 在 lockOrder() 中添加
// 7. 扣减用户参团次数
Account account = accountRepository
    .findByUserIdAndActivityId(cmd.getUserId(), cmd.getActivityId())
    .orElseGet(() -> {
        String accountId = "ACC" + idGenerator.nextId();
        return Account.create(accountId, cmd.getUserId(), cmd.getActivityId(), null);
    });

account.deductCount(activity);  // 扣减次数
accountRepository.save(account);
```

---

### 2. 缺少幂等性检查（Critical）

#### 问题描述

* `TradeOrderService.lockOrder()` **未检查** `outTradeNo` 是否已存在
* 幂等性校验仅存在于 `LockOrderService.lockOrder()`（第 83–87 行）
* 应用服务层缺失幂等防护

#### 影响

* 用户重复点击会创建多个 `Order` 和 `TradeOrder`
* 浪费系统资源
* 可能导致参团次数被多次扣减

#### 证据

* `LockOrderService.java:83–87`
* 幂等性逻辑位于领域服务层，但应用服务层缺失

#### 修复建议

```java
// 在 lockOrder() 开始处添加
// 0. 幂等性检查
Optional<TradeOrder> existingTradeOrder =
    tradeOrderRepository.findByOutTradeNo(cmd.getOutTradeNo());

if (existingTradeOrder.isPresent()) {
    log.warn("【TradeOrderService】交易单号已存在, 返回已有订单, outTradeNo: {}",
        cmd.getOutTradeNo());
    return tradeOrderAssembler.toVO(existingTradeOrder.get());
}
```

---

### 3. 缺少事务回滚补偿机制（High）

#### 问题描述

* `lockOrderService.lockOrder()` 失败时，前面创建的 `Order` 不会回滚
* `Account` 次数扣减成功但后续失败，**不会补偿**
* 容易产生数据不一致

#### 影响

* `Order` 创建成功但 `TradeOrder` 未创建
* 用户参团次数被扣减但锁单失败
* 数据库中存在“孤儿订单”

#### 修复建议

```java
@Transactional(rollbackFor = Exception.class)
public TradeOrderVO lockOrder(LockOrderCmd cmd) {
    Account account = null;

    try {
        // ... 业务逻辑 ...

        account = loadOrCreateAccount(cmd.getUserId(), cmd.getActivityId());
        account.deductCount(activity);
        accountRepository.save(account);

        TradeOrder tradeOrder = lockOrderService.lockOrder(...);
        return tradeOrderAssembler.toVO(tradeOrder);

    } catch (Exception e) {
        // 补偿逻辑
        if (account != null) {
            account.compensateCount();
            accountRepository.save(account);
        }
        throw e;
    }
}
```

---

### 🟡 中危问题

---

### 4. createOrderIfNeeded 潜在并发问题（Medium）

#### 问题描述

* 当 `orderId` 为空时会创建新 `Order`
* 多个并发请求（如团长发起拼团）可能创建多个 `Order`
* `outTradeNo` 幂等性在此之前尚未生效

#### 影响

* 可能创建重复拼团订单
* 浪费 `orderId`、`teamId`

#### 修复建议

* 在创建 `Order` 前先检查 `outTradeNo`
* 或使用分布式锁保护 `Order` 创建流程

---

### 5. 缺少 Order 状态校验（Medium）

#### 问题描述

* 加入已有拼团时未校验 `Order` 状态
* 未检查是否已过期、已满、已取消

#### 影响

* 用户可能加入已结束拼团
* 虽然 `LockOrderService` 会校验，但失败过晚

#### 修复建议

```java
private String createOrderIfNeeded(...) {
    String orderId = cmd.getOrderId();

    if (orderId != null && !orderId.isEmpty()) {
        Order existingOrder = orderRepository.findById(orderId)
            .orElseThrow(() -> new BizException("拼团订单不存在"));

        existingOrder.validateLock();
        return orderId;
    }

    // 创建新订单...
}
```

---

### 6. 价格计算依赖 userId，但未校验身份（Medium）

#### 问题描述

* 折扣计算使用 `cmd.getUserId()`
* 某些计算器可能基于用户标签（CrowdTag）
* 未校验 userId 是否可信

#### 影响

* 恶意用户可能伪造 userId 获取他人折扣

#### 修复建议

* Controller 层从 JWT / Session 获取真实 userId
* 不信任前端传入的 userId

---

### 🟢 低危问题

---

### 7. 日志可能泄露敏感信息（Low）

#### 问题描述

* 日志中记录了 `payPrice`
* 可能涉及敏感价格信息

#### 修复建议

* 价格脱敏或加密
* 或仅在 DEBUG 级别记录

---

## 架构层面问题

### 1. 职责划分不清晰

* `TradeOrderService` 同时负责：

   * 价格计算
   * Order 创建
   * Account 扣减
   * 锁单调用

**建议**：

* 引入 Saga 或流程编排器
* 明确每一步为独立事务步骤

---

### 2. 缺少分布式锁

* 高并发下可能并行创建 Order
* 数据库乐观锁不足以协调流程

**建议**：

* 使用 Redis 分布式锁
* 锁粒度：`activity:{activityId}:create_order`

---

## 推荐修复优先级

| 优先级 | 问题                       | 影响     | 工作量 |
| --- | ------------------------ | ------ | --- |
| P0  | 缺少 Account 参团次数扣减        | 业务逻辑错误 | 中   |
| P0  | 缺少幂等性检查                  | 重复订单   | 低   |
| P1  | 缺少事务回滚补偿                 | 数据不一致  | 高   |
| P2  | createOrderIfNeeded 并发问题 | 资源浪费   | 中   |
| P2  | 缺少 Order 状态校验            | 用户体验   | 低   |
| P3  | userId 可信问题              | 安全风险   | 低   |
| P4  | 日志敏感信息                   | 合规风险   | 低   |

---

## 测试建议

### 1. 并发测试

```java
@Test
void testConcurrentLockOrder() {
    // 模拟 100 个用户同时锁单
    CountDownLatch latch = new CountDownLatch(100);
    // 验证只创建一个 Order
    // 验证 lockCount 正确
}
```

### 2. 幂等性测试

```java
@Test
void testIdempotency() {
    // 使用相同 outTradeNo 调用两次
    // 验证只创建一个 TradeOrder
}
```

### 3. 参团次数测试

```java
@Test
void testParticipationLimit() {
    // 活动限制 3 次
    // 第 4 次锁单失败
}
```

---

## 总结

### ✅ 做得好的地方

* 后端价格计算与校验，防止篡改
* 使用过滤链进行前置校验
* 整体代码结构清晰

### ❌ 需要改进的地方

* 缺少 Account 次数扣减（最严重）
* 缺少应用层幂等性
* 缺少失败补偿机制

**建议**：优先修复 **P0 / P1** 问题，并进行充分的并发与压力测试。
