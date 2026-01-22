import request from '@/utils/request'

export const userApi = {
  // 获取用户资料
  getProfile() {
    return request.get('/user/profile')
  },

  // 获取用户订单列表
  getOrders(params = {}) {
    return request.get('/user/orders', { params })
  }
}
