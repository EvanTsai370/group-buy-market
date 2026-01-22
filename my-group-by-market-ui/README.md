# 拼团营销系统前端

基于 Vue 3 + Vite + Element Plus 构建的拼团营销系统前端。

## 技术栈

- Vue 3 (Composition API)
- Vite 5
- Element Plus
- Vue Router 4
- Pinia (状态管理)
- Axios (HTTP 请求)
- Sass (样式)

## 项目结构

```
src/
├── api/           # API 接口封装
├── assets/        # 静态资源
├── components/    # 公共组件
├── composables/   # 组合式函数
├── layouts/       # 布局组件
│   ├── CustomerLayout.vue  # C端布局
│   └── AdminLayout.vue     # Admin端布局
├── router/        # 路由配置
├── stores/        # Pinia 状态管理
├── styles/        # 全局样式
├── utils/         # 工具函数
└── views/         # 页面组件
    ├── auth/      # 认证页面 (登录/注册)
    ├── customer/  # C端页面
    └── admin/     # Admin端页面
```

## 开发

```bash
# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 构建生产版本
npm run build

# 预览生产构建
npm run preview
```

## 页面路由

### 认证
- `/login` - 登录
- `/register` - 注册

### C端 (需要 USER 角色)
- `/customer/home` - 商品列表首页
- `/customer/product/:spuId` - 商品详情
- `/customer/lock-order` - 确认订单
- `/customer/payment/:tradeOrderId` - 支付
- `/customer/progress/:orderId` - 拼团进度
- `/customer/profile` - 个人中心
- `/customer/orders` - 我的订单

### Admin端 (需要 ADMIN 角色)
- `/admin/dashboard` - 仪表盘
- `/admin/activities` - 活动管理
- `/admin/goods` - 商品管理
- `/admin/orders` - 订单管理
- `/admin/users` - 用户管理
- `/admin/tags` - 人群标签
- `/admin/settings` - 系统设置

## 配置

### 开发代理

开发环境下，`/api` 路径会自动代理到 `http://localhost:8080`，确保后端服务已启动。

### 环境变量

可以在项目根目录创建 `.env.local` 文件配置环境变量：

```
VITE_API_BASE_URL=http://localhost:8080
```

## API 对接

后端 API 文档：启动后端服务后访问 `/doc.html`

### 主要 API 模块

- `auth` - 认证相关 (登录/注册/刷新Token)
- `goods` - C端商品 (列表/详情/价格试算/拼团队伍)
- `trade` - 交易 (锁单/退款/进度查询)
- `payment` - 支付 (创建支付/查询状态)
- `user` - 用户中心 (资料/订单)
- `admin` - 管理后台 (活动/商品/订单/用户/标签/设置)
