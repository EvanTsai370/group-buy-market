<template>
  <div v-loading="loading" class="lock-order-page">
    <div class="page-header">
      <h2>确认订单</h2>
    </div>

    <template v-if="orderInfo">
      <!-- 商品信息 -->
      <div class="order-section">
        <div class="section-title">商品信息</div>
        <div class="goods-card">
          <img :src="orderInfo.mainImage || defaultImage" :alt="orderInfo.spuName" class="goods-image" />
          <div class="goods-info">
            <h3>{{ orderInfo.spuName }}</h3>
            <p class="sku-name">规格：{{ orderInfo.skuName }}</p>
            <div class="price-row">
              <span class="group-price">¥{{ orderInfo.payPrice }}</span>
              <span class="original-price">¥{{ orderInfo.originalPrice }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 拼团信息 -->
      <div class="order-section">
        <div class="section-title">拼团信息</div>
        <div class="info-card">
          <div class="info-row">
            <span class="label">拼团类型</span>
            <span class="value">
              <el-tag v-if="orderId" type="success">参与拼团</el-tag>
              <el-tag v-else type="danger">发起拼团</el-tag>
            </span>
          </div>
          <div class="info-row">
            <span class="label">成团人数</span>
            <span class="value">{{ orderInfo.targetCount }}人成团</span>
          </div>
          <div class="info-row">
            <span class="label">拼团有效期</span>
            <span class="value">{{ orderInfo.validHours || 24 }}小时</span>
          </div>
        </div>
      </div>

      <!-- 价格明细 -->
      <div class="order-section">
        <div class="section-title">价格明细</div>
        <div class="price-detail">
          <div class="price-row">
            <span>商品原价</span>
            <span>¥{{ orderInfo.originalPrice }}</span>
          </div>
          <div class="price-row discount">
            <span>拼团优惠</span>
            <span>-¥{{ (orderInfo.originalPrice - orderInfo.payPrice).toFixed(2) }}</span>
          </div>
          <div class="price-row total">
            <span>实付金额</span>
            <span class="total-price">¥{{ orderInfo.payPrice }}</span>
          </div>
        </div>
      </div>

      <!-- 提交按钮 -->
      <div class="submit-section">
        <div class="total-info">
          <span>合计：</span>
          <span class="amount">¥{{ orderInfo.payPrice }}</span>
        </div>
        <el-button type="danger" size="large" :loading="submitting" @click="handleSubmit">
          提交订单
        </el-button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { goodsApi } from '@/api/goods'
import { tradeApi } from '@/api/trade'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const orderInfo = ref(null)
const defaultImage = 'https://via.placeholder.com/120x120?text=No+Image'

const spuId = computed(() => route.query.spuId)
const skuId = computed(() => route.query.skuId)
const activityId = computed(() => route.query.activityId)
const orderId = computed(() => route.query.orderId) // 参团时存在

// 获取订单预览信息
const fetchOrderInfo = async () => {
  if (!skuId.value) {
    ElMessage.error('参数错误')
    router.back()
    return
  }

  loading.value = true
  try {
    // 获取价格试算结果
    const trialRes = await goodsApi.trialPrice(skuId.value, { activityId: activityId.value })
    if (trialRes.code === 0) {
      orderInfo.value = trialRes.data
    } else {
      ElMessage.error(trialRes.message || '获取订单信息失败')
      router.back()
    }
  } catch (error) {
    console.error('获取订单信息失败:', error)
    router.back()
  } finally {
    loading.value = false
  }
}

// 提交订单
const handleSubmit = async () => {
  if (!orderInfo.value) return

  submitting.value = true
  try {
    const data = {
      skuId: skuId.value,
      activityId: activityId.value,
      orderId: orderId.value || null,
      outTradeNo: generateOutTradeNo()
    }

    const res = await tradeApi.lockOrder(data)
    if (res.code === '00000' && res.data) {
      ElMessage.success('下单成功，请尽快支付')
      // 跳转到支付页面
      router.push(`/customer/payment/${res.data.tradeOrderId}`)
    } else {
      ElMessage.error(res.message || '下单失败')
    }
  } catch (error) {
    ElMessage.error('下单失败，请重试')
  } finally {
    submitting.value = false
  }
}

// 生成外部交易号
const generateOutTradeNo = () => {
  const timestamp = Date.now()
  const random = Math.floor(Math.random() * 10000).toString().padStart(4, '0')
  return `OUT${timestamp}${random}`
}

onMounted(() => {
  fetchOrderInfo()
})
</script>

<style lang="scss" scoped>
.lock-order-page {
  max-width: 800px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;

  h2 {
    font-size: 20px;
    color: #333;
  }
}

.order-section {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 16px;

  .section-title {
    font-size: 16px;
    font-weight: bold;
    color: #333;
    margin-bottom: 16px;
    padding-bottom: 12px;
    border-bottom: 1px solid #eee;
  }
}

.goods-card {
  display: flex;
  gap: 16px;

  .goods-image {
    width: 100px;
    height: 100px;
    border-radius: 8px;
    object-fit: cover;
  }

  .goods-info {
    flex: 1;

    h3 {
      font-size: 16px;
      color: #333;
      margin-bottom: 8px;
    }

    .sku-name {
      font-size: 14px;
      color: #999;
      margin-bottom: 12px;
    }

    .price-row {
      .group-price {
        font-size: 20px;
        font-weight: bold;
        color: #f56c6c;
      }

      .original-price {
        font-size: 14px;
        color: #999;
        text-decoration: line-through;
        margin-left: 8px;
      }
    }
  }
}

.info-card {
  .info-row {
    display: flex;
    justify-content: space-between;
    padding: 12px 0;
    border-bottom: 1px dashed #eee;

    &:last-child {
      border-bottom: none;
    }

    .label {
      color: #666;
    }

    .value {
      color: #333;
    }
  }
}

.price-detail {
  .price-row {
    display: flex;
    justify-content: space-between;
    padding: 8px 0;
    font-size: 14px;
    color: #666;

    &.discount {
      color: #f56c6c;
    }

    &.total {
      padding-top: 16px;
      margin-top: 8px;
      border-top: 1px solid #eee;
      font-size: 16px;
      color: #333;

      .total-price {
        font-size: 24px;
        font-weight: bold;
        color: #f56c6c;
      }
    }
  }
}

.submit-section {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 24px;

  .total-info {
    font-size: 14px;
    color: #666;

    .amount {
      font-size: 24px;
      font-weight: bold;
      color: #f56c6c;
    }
  }

  .el-button {
    min-width: 150px;
  }
}
</style>
