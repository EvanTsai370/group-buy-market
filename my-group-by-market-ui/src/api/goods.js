import request from '@/utils/request'

export const goodsApi = {
  // 获取 SPU 列表
  getSpuList(params = {}) {
    return request.get('/goods/spu/list', { params })
  },

  // 获取 SPU 详情
  getSpuDetail(spuId) {
    return request.get(`/goods/spu/${spuId}`)
  },

  // 价格试算
  trialPrice(skuId, params = {}) {
    return request.get(`/goods/${skuId}/trial`, { params })
  },

  // 获取 SPU 下的拼团队伍列表
  getTeamList(spuId) {
    return request.get(`/goods/${spuId}/teams`)
  }
}
