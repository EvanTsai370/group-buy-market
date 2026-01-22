import request from '@/utils/request'

export const adminApi = {
  // ========== 仪表盘 ==========
  getDashboardStats() {
    return request.get('/admin/dashboard/stats')
  },

  // ========== 活动管理 ==========
  getActivities(params = {}) {
    return request.get('/admin/activities', { params })
  },

  getActivity(activityId) {
    return request.get(`/admin/activities/${activityId}`)
  },

  createActivity(data) {
    return request.post('/admin/activities', data)
  },

  updateActivity(activityId, data) {
    return request.put(`/admin/activities/${activityId}`, data)
  },

  updateActivityStatus(activityId, status) {
    return request.put(`/admin/activities/${activityId}/status`, { status })
  },

  // ========== 折扣管理 ==========
  getDiscounts() {
    return request.get('/admin/activities/discounts')
  },

  getDiscountsPage(params = {}) {
    return request.get('/admin/activities/discounts/page', { params })
  },

  createDiscount(data) {
    return request.post('/admin/activities/discount', data)
  },

  // ========== SPU 选择器 ==========
  getSpuOptions() {
    return request.get('/admin/activities/spu/options')
  },

  getSpuPage(params = {}) {
    return request.get('/admin/activities/spu/page', { params })
  },

  // ========== 商品管理 ==========
  getSpuList(params = {}) {
    return request.get('/admin/goods/spu', { params })
  },

  getSpu(spuId) {
    return request.get(`/admin/goods/spu/${spuId}`)
  },

  createSpu(data) {
    return request.post('/admin/goods/spu', data)
  },

  updateSpu(spuId, data) {
    return request.put(`/admin/goods/spu/${spuId}`, data)
  },

  onSaleSpu(spuId) {
    return request.post(`/admin/goods/spu/${spuId}/on-sale`)
  },

  offSaleSpu(spuId) {
    return request.post(`/admin/goods/spu/${spuId}/off-sale`)
  },


  // SKU 管理
  // Note: List SKUs for specific SPU is done by getSpu(spuId) which returns skuList

  createSku(data) {
    return request.post('/admin/goods/sku', data)
  },

  updateSku(skuId, data) {
    return request.put(`/admin/goods/sku/${skuId}`, data)
  },

  addSkuStock(skuId, quantity) {
    return request.post(`/admin/goods/sku/${skuId}/add-stock`, null, { params: { quantity } })
  },

  // ========== 订单管理 ==========
  getOrders(params = {}) {
    return request.get('/admin/orders', { params })
  },

  getOrder(orderId) {
    return request.get(`/admin/orders/${orderId}`)
  },

  // ========== 用户管理 ==========
  getUsers(params = {}) {
    return request.get('/admin/users', { params })
  },

  getUser(userId) {
    return request.get(`/admin/users/${userId}`)
  },

  updateUserStatus(userId, status) {
    return request.put(`/admin/users/${userId}/status`, { status })
  },

  resetUserPassword(userId) {
    return request.post(`/admin/users/${userId}/reset-password`)
  },

  createAdmin(data) {
    return request.post('/admin/users/admin', data)
  },

  // ========== 人群标签 ==========
  getTags(params = {}) {
    return request.get('/admin/tags', { params })
  },

  getTag(tagId) {
    return request.get(`/admin/tags/${tagId}`)
  },

  createTag(data) {
    return request.post('/admin/tags', data)
  },

  updateTag(tagId, data) {
    return request.put(`/admin/tags/${tagId}`, data)
  },

  calculateTag(tagId) {
    return request.post(`/admin/tags/${tagId}/calculate`)
  },

  // ========== 流控设置/系统配置 ==========
  getAllConfigs() {
    return request.get('/admin/config/all')
  },

  updateConfig(key, value) {
    return request.post('/admin/config/update', null, { params: { key, value } })
  }
}
