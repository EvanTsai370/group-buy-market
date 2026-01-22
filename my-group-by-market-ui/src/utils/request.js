import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'

// 创建 axios 实例
const request = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()

    // 添加 Token
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }

    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    const res = response.data

    // 业务状态码判断
    if (res.code !== '00000') {
      ElMessage.error(res.msg || '请求失败')

      // Token 过期
      if (res.code === 401) {
        const userStore = useUserStore()
        userStore.logout()
      }

      return Promise.reject(new Error(res.msg || '请求失败'))
    }

    return res
  },
  async (error) => {
    const { response } = error

    if (response) {
      switch (response.status) {
        case 401:
          // Token 过期，尝试刷新
          const userStore = useUserStore()
          const refreshed = await userStore.refreshAccessToken()
          if (refreshed) {
            // 重试原请求
            return request(error.config)
          }
          ElMessage.error('登录已过期，请重新登录')
          break
        case 403:
          ElMessage.error('没有权限访问')
          router.push('/login')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          ElMessage.error('服务器内部错误')
          break
        default:
          ElMessage.error(response.data?.message || '请求失败')
      }
    } else {
      ElMessage.error('网络连接失败，请检查网络')
    }

    return Promise.reject(error)
  }
)

export default request
