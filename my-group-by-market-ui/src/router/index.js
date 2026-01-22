import { createRouter, createWebHistory } from 'vue-router'
import NProgress from 'nprogress'
import { useUserStore } from '@/stores/user'

// 路由配置
const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  // 认证相关
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/Login.vue'),
    meta: { title: '登录', guest: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/Register.vue'),
    meta: { title: '注册', guest: true }
  },
  // C端页面
  {
    path: '/customer',
    component: () => import('@/layouts/CustomerLayout.vue'),
    meta: { requiresAuth: true, role: 'USER' },
    children: [
      {
        path: '',
        redirect: '/customer/home'
      },
      {
        path: 'home',
        name: 'CustomerHome',
        component: () => import('@/views/customer/Home.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'product/:spuId',
        name: 'ProductDetail',
        component: () => import('@/views/customer/ProductDetail.vue'),
        meta: { title: '商品详情' }
      },
      {
        path: 'lock-order',
        name: 'LockOrder',
        component: () => import('@/views/customer/LockOrder.vue'),
        meta: { title: '确认订单' }
      },
      {
        path: 'payment/:tradeOrderId',
        name: 'Payment',
        component: () => import('@/views/customer/Payment.vue'),
        meta: { title: '支付' }
      },
      {
        path: 'progress/:orderId',
        name: 'GroupProgress',
        component: () => import('@/views/customer/GroupProgress.vue'),
        meta: { title: '拼团进度', public: true }
      },
      {
        path: 'profile',
        name: 'CustomerProfile',
        component: () => import('@/views/customer/Profile.vue'),
        meta: { title: '个人中心' }
      },
      {
        path: 'orders',
        name: 'CustomerOrders',
        component: () => import('@/views/customer/Orders.vue'),
        meta: { title: '我的订单' }
      }
    ]
  },
  // Admin端页面
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { requiresAuth: true, role: 'ADMIN' },
    children: [
      {
        path: '',
        redirect: '/admin/dashboard'
      },
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/Dashboard.vue'),
        meta: { title: '仪表盘' }
      },
      {
        path: 'activities',
        name: 'AdminActivities',
        component: () => import('@/views/admin/Activities.vue'),
        meta: { title: '活动管理' }
      },
      {
        path: 'goods',
        name: 'AdminGoods',
        component: () => import('@/views/admin/Goods.vue'),
        meta: { title: '商品管理' }
      },
      {
        path: 'orders',
        name: 'AdminOrders',
        component: () => import('@/views/admin/Orders.vue'),
        meta: { title: '订单管理' }
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/Users.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: 'tags',
        name: 'AdminTags',
        component: () => import('@/views/admin/Tags.vue'),
        meta: { title: '人群标签' }
      },
      {
        path: 'settings',
        name: 'AdminSettings',
        component: () => import('@/views/admin/Settings.vue'),
        meta: { title: '流控设置' }
      }
    ]
  },
  // 404
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue'),
    meta: { title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  NProgress.start()

  // 设置页面标题
  document.title = to.meta.title ? `${to.meta.title} - 拼团营销系统` : '拼团营销系统'

  const userStore = useUserStore()
  const isLoggedIn = userStore.isLoggedIn
  const userRole = userStore.role

  // 公开页面直接放行
  if (to.meta.public) {
    next()
    return
  }

  // 游客页面（登录/注册），已登录用户跳转到对应首页
  if (to.meta.guest) {
    if (isLoggedIn) {
      next(userRole === 'ADMIN' ? '/admin/dashboard' : '/customer/home')
    } else {
      next()
    }
    return
  }

  // 需要认证的页面
  if (to.meta.requiresAuth) {
    if (!isLoggedIn) {
      next({ path: '/login', query: { redirect: to.fullPath } })
      return
    }

    // 角色检查
    if (to.meta.role && to.meta.role !== userRole) {
      // 角色不匹配，跳转到对应首页
      const targetPath = userRole === 'ADMIN' ? '/admin/dashboard' : '/customer/home'

      // 防止无限重定向：如果目标页面就是当前页面，说明角色配置有误或状态异常
      if (to.path === targetPath) {
        userStore.logout()
        next('/login')
        return
      }

      next(targetPath)
    }
  }

  next()
})

router.afterEach(() => {
  NProgress.done()
})

export default router
