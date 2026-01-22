import request from '@/utils/request'

export const authApi = {
  // 登录
  login(data) {
    return request.post('/auth/login', data)
  },

  // 注册
  register(data) {
    return request.post('/auth/register', data)
  },

  // 刷新 Token
  refreshToken(refreshToken) {
    return request.post('/auth/refresh', { refreshToken })
  }
}
