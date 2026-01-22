import request from '@/utils/request'

export const paymentApi = {
  // 创建支付（获取支付宝表单）
  createPayment(data) {
    return request.post('/payment/create', data)
  },

  // 查询支付状态
  queryPayment(tradeOrderId) {
    return request.get('/payment/query', { params: { tradeOrderId } })
  }
}
