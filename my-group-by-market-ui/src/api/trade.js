import request from '@/utils/request'

export const tradeApi = {
  // 锁单（参与拼团）
  lockOrder(data) {
    return request.post('/trade/lock', data)
  },

  // 查询交易订单详情
  getTradeOrder(tradeOrderId) {
    return request.get(`/trade/${tradeOrderId}`)
  },

  // 申请退款
  refund(tradeOrderId, data = {}) {
    return request.post(`/trade/refund/${tradeOrderId}`, data)
  },

  // 查询拼团进度
  getOrderProgress(orderId) {
    return request.get(`/trade/order/${orderId}/progress`)
  }
}
