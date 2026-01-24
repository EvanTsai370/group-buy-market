# 拼团营销系统 (Group Buying Platform)

<div align="center">

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Build Status](https://img.shields.io/badge/build-passing-success.svg)]()

一个基于 DDD（领域驱动设计）的拼团营销系统，支持多人拼团、库存管理、支付结算等完整的电商营销功能。

[特性](#特性) • [快速开始](#快速开始) • [架构](#架构) • [API文档](#api文档) • [贡献](#贡献)

</div>

---

## 📖 项目简介

本系统是一个拼团营销平台，采用 **领域驱动设计（DDD）** 架构，提供完整的拼团购物解决方案。用户可以发起拼团或加入已有团队，达到目标人数后享受优惠价格。系统支持实时库存管理、支付对接、订单结算等核心电商功能。

### 业务场景

- **拼团购买**：用户发起或加入拼团，达到人数自动成团
- **精准营销**：支持人群标签，实现精准用户触达
- **库存管理**：实时库存监控，防止超卖
- **支付结算**：对接支付宝，支持支付和退款
- **订单管理**：完整的订单生命周期管理

---

## ✨ 特性

### 核心功能

- **拼团机制**
  - SPU 拼团模式（同商品不同规格可拼团）
  - 虚拟/真实成团方式
  - 实时拼团进度查询
  - 自动超时处理

- **营销能力**
  - 人群标签精准营销（可见不可参与/严格模式/全开放）
  - 多种折扣策略（直减/百分比/固定价/满减）
  - 参与次数限制
  - 流量控制和灰度发布

- **库存管理**
  - 库存预占机制
  - 库存冻结/解冻
  - 防止高并发超卖

- **支付系统**
  - 支付宝沙箱对接
  - 异步回调处理
  - 支付状态查询
  - 退款支持

- **安全保障**
  - JWT Token 认证
  - 乐观锁防并发
  - 分布式锁
  - 幂等性保护
  - 签名验证

### 技术亮点

- 🏗️ **DDD 架构**：清晰的分层架构，职责分明
- 🚀 **高并发**：Redis 缓存 + 原子操作 + 乐观锁
- 🔄 **异步处理**：RabbitMQ 延迟队列 + 死信队列
- 📊 **数据一致性**：领域事件 + 最终一致性
- 🎯 **设计模式**：策略模式、责任链模式、工厂模式
- 🔐 **安全加固**：多层防护，P0/P1/P2 安全措施

---

## 🛠️ 技术栈

### 后端技术

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 21 | 编程语言 |
| Spring Boot | 3.2.0 | 应用框架 |
| MyBatis-Plus | 3.5.15 | ORM 框架 |
| MySQL | 8.2.0 | 关系型数据库 |
| Redis | Latest | 缓存 + 分布式锁 |
| RabbitMQ | Latest | 消息队列 |
| MapStruct | 1.5.5 | 对象映射 |
| Lombok | 1.18.36 | 代码简化 |
| Knife4j | 4.5.0 | API 文档 |

### 前端技术

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.4.21 | 渐进式 JavaScript 框架 |
| Vite | 5.1.6 | 下一代前端构建工具 |
| Vue Router | 4.3.0 | 官方路由管理器 |
| Pinia | 2.1.7 | 状态管理（Vue 官方推荐） |
| Element Plus | 2.6.1 | Vue 3 组件库 |
| Axios | 1.6.7 | HTTP 客户端 |
| NProgress | 0.2.0 | 进度条 |
| Day.js | 1.11.10 | 日期处理库 |

### 基础设施

| 技术 | 说明 |
|------|------|
| Nginx | 反向代理服务器（统一入口） |
| Docker | 容器化部署 |
| Docker Compose | 多容器编排 |

---

## 🚀 快速开始

### 方式一：Docker 快速启动（推荐）

使用 Docker Compose 一键启动基础设施服务（MySQL、Redis、RabbitMQ），无需手动安装。

#### 前置要求

- Docker 20.10+
- Docker Compose 2.0+
- JDK 21+（运行 Java 应用）
- Maven 3.6+（构建 Java 应用）
- Node.js 18+（运行前端）
- Nginx（可选，作为统一入口）

#### 启动步骤

1. **克隆项目**

```bash
git clone https://github.com/EvanTsai370/group-buy-market.git
cd my-group-by-market
```

2. **启动基础设施服务**

```bash
docker-compose up -d
```

这将自动启动以下服务：
- **MySQL 8.2.0**（端口：3306）
  - 数据库名：`group_buy_market`
  - root 密码：`123456`
  - 字符集：utf8mb4（支持 Emoji）

- **Redis 7.2**（端口：6379）
  - 无密码访问
  - 数据持久化到 Docker Volume

- **RabbitMQ 3.13 with Management**（端口：5672, 15672）
  - 用户名/密码：`guest/guest`
  - 自动安装延迟消息插件（rabbitmq_delayed_message_exchange）
  - 管理界面：http://localhost:15672

3. **查看服务状态**

```bash
docker-compose ps
```

期望输出：
```
NAME              IMAGE                       STATUS
market-mysql      mysql:8.2.0                 Up
market-rabbitmq   my-group-by-market-rabbitmq Up (healthy)
market-redis      redis:7.2                   Up
```

4. **等待 MySQL 初始化完成**

首次启动 MySQL 需要初始化，建议等待 30 秒后再启动应用：

```bash
# 查看 MySQL 日志，等待看到 "ready for connections" 提示
docker-compose logs -f mysql
```

5. **构建并运行 Java 应用**

```bash
# 构建项目
mvn clean install

# 运行应用（使用 dev 环境配置）
mvn spring-boot:run -pl my-group-by-market-start
```

后端启动成功后，访问 API 文档：http://localhost:8080/doc.html

6. **运行前端（可选）**

```bash
# 进入前端目录
cd my-group-by-market-ui

# 安装依赖（首次运行需要）
npm install

# 启动开发服务器
npm run dev
```

前端启动成功后，访问：http://localhost:3000

7. **配置并启动 Nginx（可选，推荐）**

Nginx 作为统一入口，代理前后端服务，提供更接近生产环境的体验。

**macOS (Homebrew)：**

```bash
# 安装 Nginx
brew install nginx

# 复制配置文件到 Nginx 配置目录
cp nginx.conf /opt/homebrew/etc/nginx/servers/my-group-by-market.conf

# 测试配置文件
nginx -t

# 启动 Nginx
brew services start nginx

# 或者直接启动（不加入开机启动）
nginx
```

**Linux (Ubuntu/Debian)：**

```bash
# 安装 Nginx
sudo apt update
sudo apt install nginx

# 复制配置文件
sudo cp nginx.conf /etc/nginx/sites-available/my-group-by-market
sudo ln -s /etc/nginx/sites-available/my-group-by-market /etc/nginx/sites-enabled/

# 测试配置文件
sudo nginx -t

# 重启 Nginx
sudo systemctl restart nginx
```

**访问方式：**

- **统一入口（Nginx）**：http://localhost:8888
  - 前端页面：http://localhost:8888/
  - 后端 API：http://localhost:8888/api/
  - 静态文件：http://localhost:8888/files/

- **直接访问（不经过 Nginx）**：
  - 前端：http://localhost:3000
  - 后端：http://localhost:8080
  - RabbitMQ 管理界面：http://localhost:15672

**Nginx 配置说明：**

项目提供的 `nginx.conf` 包含以下功能：
- 监听端口：8888
- 反向代理后端 API（/api/ → localhost:8080）
- 反向代理前端（/ → localhost:3000，支持 Vite HMR）
- 静态文件服务（/files/ → /tmp/my-group-buy-market/upload/）
- CORS 跨域配置
- 文件上传大小限制：5MB

8. **访问服务汇总**

| 服务 | 地址 | 说明 |
|------|------|------|
| 前端页面 | http://localhost:8888 | 通过 Nginx 访问（推荐） |
| 前端页面 | http://localhost:3000 | 直接访问 Vite 开发服务器 |
| 后端 API | http://localhost:8888/api/ | 通过 Nginx 访问 |
| 后端 API | http://localhost:8080 | 直接访问后端 |
| API 文档 | http://localhost:8080/doc.html | Knife4j 文档 |
| RabbitMQ 管理 | http://localhost:15672 | 用户名/密码：guest/guest |

9. **查看日志**

```bash
# 查看所有基础设施服务日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f mysql
docker-compose logs -f rabbitmq
docker-compose logs -f redis

# 查看 Nginx 日志（macOS Homebrew）
tail -f /opt/homebrew/var/log/nginx/my-group-by-market-access.log
tail -f /opt/homebrew/var/log/nginx/my-group-by-market-error.log

# 查看 Nginx 日志（Linux）
sudo tail -f /var/log/nginx/access.log
sudo tail -f /var/log/nginx/error.log
```

10. **停止服务**

```bash
# 停止 Docker 基础设施服务
docker-compose down

# 停止并删除数据卷（慎用，会清空所有数据）
docker-compose down -v

# 停止 Nginx（macOS）
brew services stop nginx
# 或者
nginx -s stop

# 停止 Nginx（Linux）
sudo systemctl stop nginx

# 停止 Java 应用和前端（Ctrl+C）
```

#### 配置说明

**后端配置（`application-dev.yml`）：**

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/group_buy_market?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8
    username: root
    password: 123456
  data:
    redis:
      host: localhost
      port: 6379
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
```

**前端配置（`vite.config.js`）：**

```javascript
server: {
  port: 3000  // 前端开发服务器端口
}
```

**Nginx 配置（`nginx.conf`）：**

```nginx
server {
  listen 8888;  # Nginx 监听端口

  # 后端 API 代理
  location /api/ {
    proxy_pass http://localhost:8080/api/;
  }

  # 静态文件服务
  location /files/ {
    alias /tmp/my-group-buy-market/upload/;
  }

  # 前端代理（支持 Vite HMR）
  location / {
    proxy_pass http://localhost:3000;
  }
}
```

#### 数据持久化

Docker Compose 使用命名卷（Named Volumes）持久化数据：
- `mysql_data` - MySQL 数据库文件
- `redis_data` - Redis 持久化数据
- `rabbitmq_data` - RabbitMQ 消息和配置

即使删除容器，只要不执行 `docker-compose down -v`，数据就不会丢失。

#### 故障排查

**问题：RabbitMQ 启动失败**
- 检查延迟插件是否下载成功：`docker-compose logs rabbitmq`
- 如果网络问题导致插件下载失败，可以手动下载后放入 `docker/rabbitmq/` 目录

**问题：MySQL 连接失败**
- 确保 MySQL 已完全启动：`docker-compose logs mysql | grep "ready for connections"`
- 检查数据库是否自动创建：`docker exec -it market-mysql mysql -uroot -p123456 -e "SHOW DATABASES;"`

**问题：端口冲突**
- 如果本地已有服务占用端口，修改 `docker-compose.yml` 中的端口映射
- 例如将 MySQL 端口改为 `"3307:3306"`

**问题：Nginx 启动失败**
- 检查端口 8888 是否被占用：`lsof -i :8888`（macOS/Linux）
- 修改 `nginx.conf` 中的监听端口
- 确保前后端服务已启动（Nginx 会代理到 localhost:3000 和 localhost:8080）

**问题：前端请求 404**
- 确保通过 Nginx 访问（http://localhost:8888）
- 检查后端服务是否启动（http://localhost:8080/doc.html 应该可访问）
- 查看 Nginx 错误日志排查问题

**问题：文件上传失败**
- 检查上传目录是否存在：`/tmp/my-group-buy-market/upload/`
- 确保目录有写权限：`chmod 755 /tmp/my-group-buy-market/upload/`
- 检查文件大小是否超过 5MB 限制

---

### 方式二：完全本地开发环境

适合不使用 Docker 的场景，需要手动安装所有依赖。

#### 环境要求

- **JDK 21+** - 必须使用 Java 21
- **Maven 3.6+** - 构建工具
- **MySQL 8.0+** - 数据库
- **Redis 6.0+** - 缓存
- **RabbitMQ 3.9+** - 消息队列（需安装延迟插件）
- **Node.js 18+** - 前端运行环境

#### 安装步骤

1. **克隆项目**

```bash
git clone https://github.com/yourusername/my-group-by-market.git
cd my-group-by-market
```

2. **配置数据库**

创建数据库：

```sql
CREATE DATABASE group_buying DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

修改配置文件 `my-group-by-market-start/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/group_buying?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8
    username: your_username
    password: your_password
```

3. **配置 Redis**

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: your_password  # 如果有密码
```

4. **配置 RabbitMQ**

安装延迟插件：

```bash
rabbitmq-plugins enable rabbitmq_delayed_message_exchange
```

配置连接：

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
```

5. **配置支付宝（可选）**

如需使用支付功能，在 `application.yml` 中配置支付宝沙箱信息：

```yaml
alipay:
  app-id: your_app_id
  private-key: your_private_key
  public-key: alipay_public_key
  gateway-url: https://openapi-sandbox.dl.alipaydev.com/gateway.do
```

6. **构建项目**

```bash
mvn clean install
```

7. **运行后端**

```bash
mvn spring-boot:run -pl my-group-by-market-start
```

访问 API 文档：http://localhost:8080/doc.html

8. **运行前端**

```bash
cd my-group-by-market-ui
npm install
npm run dev
```

访问前端页面：http://localhost:3000

9. **配置 Nginx（可选，推荐）**

参考"方式一"中的 Nginx 配置步骤，启动 Nginx 后访问：http://localhost:8888

这样可以获得完整的开发体验，包括：
- 统一的访问入口
- 前后端请求代理
- 静态文件服务
- CORS 跨域处理

---

## 🏗️ 架构设计

### DDD 分层架构

```
┌─────────────────────────────────────────────┐
│         Interfaces 接口层                    │
│  (Controllers, Assemblers, DTOs)           │
└─────────────────┬───────────────────────────┘
                  │
┌─────────────────▼───────────────────────────┐
│         Application 应用层                   │
│  (Services, Commands, Queries, Results)    │
└─────────────────┬───────────────────────────┘
                  │
┌─────────────────▼───────────────────────────┐
│         Domain 领域层                        │
│  (Aggregates, Entities, Value Objects,     │
│   Domain Services, Repository Interfaces)  │
└─────────────────┬───────────────────────────┘
                  │
┌─────────────────▼───────────────────────────┐
│         Infrastructure 基础设施层            │
│  (Repository Impl, Cache, MQ, Gateway)     │
└─────────────────────────────────────────────┘
```

### 核心聚合

| 聚合 | 职责 |
|------|------|
| **Activity** | 拼团活动管理 |
| **Order** | 拼团订单（团队组建） |
| **TradeOrder** | 交易订单（支付、结算） |
| **Account** | 用户参团次数限制 |
| **User** | 用户认证授权 |
| **Spu/Sku** | 商品管理 |
| **CrowdTag** | 人群标签 |

### 模块结构

```
my-group-by-market/
├── my-group-by-market-common/          # 共享工具
├── my-group-by-market-domain/          # 领域层（纯业务逻辑）
├── my-group-by-market-infrastructure/  # 基础设施（持久化、缓存、MQ）
├── my-group-by-market-application/     # 应用层（用例编排）
├── my-group-by-market-interfaces/      # 接口层（REST API）
├── my-group-by-market-start/           # 启动模块
└── my-group-by-market-ui/              # 前端
```

---

## 📚 核心功能

### 1. 拼团流程

```
用户锁单 → 支付 → 拼团中 → 成团 → 结算 → 完成
         ↓
      未支付超时 → 自动退单
         ↓
      用户退款 → 释放资源
```

### 2. 状态机

**TradeOrder 状态流转：**

```
CREATE → PAID → SETTLED
  ↓       ↓
TIMEOUT  REFUND
```

### 3. 关键设计模式

- **策略模式**：折扣计算、退款策略、通知策略
- **责任链模式**：交易规则过滤链
- **工厂模式**：聚合创建
- **仓储模式**：领域模型持久化
- **领域事件**：解耦和最终一致性

---

## 📡 API 文档

运行项目后访问：http://localhost:8080/doc.html

### 核心接口

#### C 端接口

| 接口 | 说明 |
|------|------|
| `GET /api/goods/spu/list` | 商品列表 |
| `GET /api/goods/spu/{spuId}` | 商品详情 |
| `GET /api/goods/{skuId}/trial` | 价格试算 |
| `POST /api/trade/lock` | 锁单（参与拼团） |
| `POST /api/trade/refund/{tradeOrderId}` | 退款 |
| `GET /api/order/{orderId}/progress` | 拼团进度 |
| `POST /api/payment/create` | 创建支付 |

#### 管理后台

| 接口 | 说明 |
|------|------|
| `GET /api/admin/dashboard` | 数据统计 |
| `POST /api/admin/activity` | 创建活动 |
| `POST /api/admin/goods/spu` | 创建商品 |
| `GET /api/admin/users` | 用户管理 |

---

## 🔧 开发指南

### 代码规范

- **分层严格隔离**：不同层的对象不复用
- **领域模型纯净**：Domain 层无框架依赖
- **依赖倒置**：接口在 Domain 层，实现在 Infrastructure 层
- **注释使用中文**：便于团队沟通

### 命名约定

| 层 | 对象类型 | 命名 |
|----|---------|------|
| Interfaces | 入参 | `XxxRequest` |
| Interfaces | 出参 | `XxxResponse` |
| Application | 命令 | `XxxCmd` |
| Application | 结果 | `XxxResult` |
| Domain | 聚合 | 直接业务名称 |
| Infrastructure | 持久化 | `XxxPO` |

### 测试命令

```bash
# 运行所有测试
mvn test

# 运行特定测试
mvn clean install -DskipTests
mvn test -pl my-group-by-market-start -Dtest=ClassName#methodName
```

### 数据库迁移

遵循 Flyway 约定，在 `my-group-by-market-start/src/main/resources/db/migration/` 目录下创建：

```
V{版本号}__{描述}.sql
例如：V6__add_new_feature.sql
```

---

## 🤝 贡献

欢迎贡献代码、报告问题或提出建议！

### 贡献步骤

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 提交 Pull Request

### 开发规范

- 遵循 DDD 架构原则
- 编写单元测试
- 更新相关文档
- 代码注释使用中文

---

## 📄 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件

---

<div align="center">

**如果这个项目对你有帮助，请给个 ⭐️ Star 支持一下！**

</div>
