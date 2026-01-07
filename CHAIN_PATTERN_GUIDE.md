# 责任链模式使用指南

## 📚 概述

本项目提供了两种责任链模式实现，分别适用于不同的业务场景：
- **Model1（单例链）**：简单流程，全局单链
- **Model2（多例链）**：复杂业务，多条独立链

---

## 🎯 快速选择

| 场景 | 推荐模式 | 原因 |
|------|---------|------|
| 拼团试算流程（5个节点，固定流程） | Model1 | 流程固定，全局只有一条链 |
| 交易规则过滤（多种规则组合） | Model2 | 需要动态组合，支持多条链 |
| 简单参数校验 | Model1 | 实现简单 |
| 多渠道业务流程（普通/VIP/快速通道） | Model2 | 需要多条独立链 |

---

## 📖 Model1 使用教程

### 1. 定义数据模型

```java
// 请求参数
public class TrialRequest {
    private String userId;
    private String goodsId;
}

// 动态上下文（在节点间传递数据）
public class TrialContext {
    private Activity activity;
    private Sku sku;
}

// 响应结果
public class TrialResponse {
    private boolean success;
    private BigDecimal price;
}
```

### 2. 实现节点

```java
@Component
public class ValidationNode extends AbstractChainNode<TrialRequest, TrialContext, TrialResponse> {

    @Override
    public TrialResponse execute(TrialRequest request, TrialContext context) throws Exception {
        // 1. 参数校验
        if (StringUtils.isBlank(request.getUserId())) {
            return TrialResponse.error("用户ID不能为空");
        }

        // 2. 继续下一个节点
        return nextNode(request, context);
    }
}

@Component
public class BusinessNode extends AbstractChainNode<TrialRequest, TrialContext, TrialResponse> {

    @Override
    public TrialResponse execute(TrialRequest request, TrialContext context) throws Exception {
        // 业务逻辑
        BigDecimal price = calculatePrice(request, context);
        return TrialResponse.success(price);
    }
}
```

### 3. 组装链路

```java
@Configuration
public class TrialChainConfig {

    @Resource
    private ValidationNode validationNode;

    @Resource
    private BusinessNode businessNode;

    @PostConstruct
    public void init() {
        // 手动组装链路
        validationNode.appendNext(businessNode);
    }

    @Bean
    public IChainNode<TrialRequest, TrialContext, TrialResponse> trialChain() {
        return validationNode; // 返回头节点
    }
}
```

### 4. 使用

```java
@Service
public class TrialService {

    @Resource
    private IChainNode<TrialRequest, TrialContext, TrialResponse> trialChain;

    public TrialResponse trial(TrialRequest request) throws Exception {
        TrialContext context = new TrialContext();
        return trialChain.execute(request, context);
    }
}
```

---

## 📖 Model2 使用教程

### 1. 定义数据模型（同 Model1）

### 2. 实现处理器（只负责业务逻辑）

```java
@Component
public class ValidationHandler implements IChainHandler<TrialRequest, TrialContext, TrialResponse> {

    @Override
    public TrialResponse handle(TrialRequest request, TrialContext context) throws Exception {
        if (StringUtils.isBlank(request.getUserId())) {
            return TrialResponse.error("用户ID不能为空");
        }
        return pass(request, context); // 放行
    }
}

@Component
public class AuthHandler implements IChainHandler<TrialRequest, TrialContext, TrialResponse> {

    @Override
    public TrialResponse handle(TrialRequest request, TrialContext context) throws Exception {
        if (!checkAuth(request.getUserId())) {
            return TrialResponse.error("权限不足");
        }
        return pass(request, context); // 放行
    }
}

@Component
public class BusinessHandler implements IChainHandler<TrialRequest, TrialContext, TrialResponse> {

    @Override
    public TrialResponse handle(TrialRequest request, TrialContext context) throws Exception {
        BigDecimal price = calculatePrice(request, context);
        return TrialResponse.success(price);
    }
}
```

### 3. 组装链路（在配置类中）

```java
@Configuration
public class TrialChainConfig {

    /**
     * 普通流程：参数校验 → 权限校验 → 业务处理
     */
    @Bean("normalTrialChain")
    public ChainExecutor<TrialRequest, TrialContext, TrialResponse> normalTrialChain(
            ValidationHandler validation,
            AuthHandler auth,
            BusinessHandler business) {
        return new ChainExecutor<>("普通流程", validation, auth, business);
    }

    /**
     * VIP流程：参数校验 → 业务处理（跳过权限校验）
     */
    @Bean("vipTrialChain")
    public ChainExecutor<TrialRequest, TrialContext, TrialResponse> vipTrialChain(
            ValidationHandler validation,
            BusinessHandler business) {
        return new ChainExecutor<>("VIP流程", validation, business);
    }
}
```

### 4. 使用

```java
@Service
public class TrialService {

    @Resource(name = "normalTrialChain")
    private ChainExecutor<TrialRequest, TrialContext, TrialResponse> normalChain;

    @Resource(name = "vipTrialChain")
    private ChainExecutor<TrialRequest, TrialContext, TrialResponse> vipChain;

    public TrialResponse trial(TrialRequest request, boolean isVip) throws Exception {
        ChainExecutor<TrialRequest, TrialContext, TrialResponse> chain =
            isVip ? vipChain : normalChain;

        TrialContext context = new TrialContext();
        return chain.execute(request, context);
    }
}
```

---

## 🔥 高级用法

### 1. 动态添加处理器

```java
ChainExecutor<Request, Context, Response> chain = new ChainExecutor<>("动态链");
chain.addHandler(new ValidationHandler())
     .addHandler(new BusinessHandler());
```

### 2. 打印链路信息（调试）

```java
chain.printChainInfo();

// 输出：
// ========================================
// 责任链: 普通流程
// 处理器数量: 3
// 处理器列表:
//   [1] ValidationHandler
//   [2] AuthHandler
//   [3] BusinessHandler
// ========================================
```

### 3. 在处理器中修改上下文

```java
@Component
public class DataLoaderHandler implements IChainHandler<Request, Context, Response> {

    @Override
    public Response handle(Request request, Context context) throws Exception {
        // 加载数据并写入上下文
        Activity activity = activityRepository.findById(request.getActivityId());
        context.setActivity(activity);

        // 放行到下一个处理器
        return pass(request, context);
    }
}
```

---

## ⚠️ 注意事项

### Model1 注意事项

1. **避免循环引用**
   ```java
   // ❌ 错误：会导致无限循环
   nodeA.appendNext(nodeB);
   nodeB.appendNext(nodeA);
   ```

2. **确保链路完整**
   ```java
   // ❌ 错误：最后一个节点没有调用 nextNode()
   public Response execute(Request req, Context ctx) {
       // 忘记调用 nextNode()
       return null;
   }

   // ✅ 正确
   public Response execute(Request req, Context ctx) {
       return nextNode(req, ctx);
   }
   ```

### Model2 注意事项

1. **返回值规则**
   ```java
   // ✅ 放行到下一个处理器
   return pass(request, context);  // 或者 return null;

   // ✅ 中断链路并返回结果
   return Response.error("校验失败");
   ```

2. **不要在处理器中调用下一个处理器**
   ```java
   // ❌ 错误：Model2 不需要手动调用下一个处理器
   public Response handle(Request req, Context ctx) {
       // 不需要这样做！
       return nextHandler.handle(req, ctx);
   }

   // ✅ 正确：直接返回 null 即可
   public Response handle(Request req, Context ctx) {
       return pass(req, ctx);
   }
   ```

---

## 📊 性能对比

| 维度 | Model1 | Model2 |
|------|--------|--------|
| 内存占用 | 低（单链） | 稍高（多链） |
| 执行效率 | 高（直接调用） | 高（for循环） |
| 代码复杂度 | 低 | 中 |
| 扩展性 | 低 | 高 |

---

## 🎓 最佳实践

1. **优先使用 Model2**：除非场景非常简单，否则推荐使用 Model2
2. **处理器单一职责**：每个处理器只做一件事
3. **上下文传递数据**：避免处理器之间直接依赖
4. **及时中断链路**：不满足条件时立即返回，避免无效执行
5. **记录日志**：在关键节点记录日志，便于排查问题

---

**文档版本**：v1.0
**更新时间**：2026-01-04
**维护者**：开发团队
