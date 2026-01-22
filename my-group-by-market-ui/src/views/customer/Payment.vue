<template>
  <div v-loading="loading" class="payment-page">
    <div class="page-header">
      <h2>订单支付</h2>
    </div>

    <template v-if="tradeOrder">
      <!-- 倒计时 -->
      <div class="countdown-section">
        <div class="countdown-card" :class="{ urgent: countdown < 300 }">
          <el-icon :size="24"><Clock /></el-icon>
          <div class="countdown-info">
            <span class="label">请在以下时间内完成支付</span>
            <span class="time">{{ formatCountdown(countdown) }}</span>
          </div>
        </div>
      </div>

      <!-- 订单信息 -->
      <div class="order-section">
        <div class="section-title">订单信息</div>
        <div class="order-info">
          <div class="info-row">
            <span class="label">订单编号</span>
            <span class="value">{{ tradeOrder.tradeOrderId }}</span>
          </div>
          <div class="info-row">
            <span class="label">商品名称</span>
            <span class="value">{{ tradeOrder.spuName }}</span>
          </div>
          <div class="info-row">
            <span class="label">商品规格</span>
            <span class="value">{{ tradeOrder.skuName }}</span>
          </div>
          <div class="info-row total">
            <span class="label">支付金额</span>
            <span class="value price">¥{{ tradeOrder.payPrice }}</span>
          </div>
        </div>
      </div>

      <!-- 支付方式 -->
      <div class="payment-section">
        <div class="section-title">支付方式</div>
        <div class="payment-methods">
          <div
            class="payment-method active"
          >
            <img src="https://gw.alipayobjects.com/mdn/member_frontWeb/afts/img/A*h7o9Q4g2KiUAAAAAAAAAAABkARQnAQ" alt="支付宝" />
            <span>支付宝</span>
            <el-icon class="check-icon"><CircleCheckFilled /></el-icon>
          </div>
        </div>
      </div>

      <!-- 支付按钮 -->
      <div class="action-section">
        <el-button @click="handleCancel">取消订单</el-button>
        <el-button type="danger" size="large" :loading="paying" @click="handlePay">
          立即支付 ¥{{ tradeOrder.payPrice }}
        </el-button>
      </div>

      <!-- 支付表单（隐藏，用于提交到支付宝） -->
      <div v-html="payForm" class="pay-form-container" />
    </template>

    <!-- 支付状态弹窗 -->
    <el-dialog v-model="showStatusDialog" title="支付确认" width="400px" :close-on-click-modal="false">
      <div class="status-dialog-content">
        <p>请确认支付是否完成</p>
      </div>
      <template #footer>
        <el-button @click="checkPaymentStatus">我已完成支付</el-button>
        <el-button type="primary" @click="showStatusDialog = false">继续支付</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { tradeApi } from '@/api/trade'
import { paymentApi } from '@/api/payment'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const paying = ref(false)
const tradeOrder = ref(null)
const countdown = ref(1800) // 30分钟
const payForm = ref('')
const showStatusDialog = ref(false)

let countdownTimer = null

const tradeOrderId = route.params.tradeOrderId

// 获取订单详情
const fetchTradeOrder = async () => {
  loading.value = true
  try {
    const res = await tradeApi.getTradeOrder(tradeOrderId)
    if (res.code === 0) {
      tradeOrder.value = res.data

      // 计算剩余时间
      if (res.data.createTime) {
        const createTime = new Date(res.data.createTime).getTime()
        const now = Date.now()
        const elapsed = Math.floor((now - createTime) / 1000)
        countdown.value = Math.max(0, 1800 - elapsed)
      }

      // 检查订单状态
      if (res.data.status !== 'CREATE') {
        if (res.data.status === 'PAID' || res.data.status === 'SETTLED') {
          ElMessage.success('订单已支付')
          router.push(`/customer/progress/${res.data.orderId}`)
        } else {
          ElMessage.warning('订单已失效')
          router.push('/customer/orders')
        }
      }
    } else {
      ElMessage.error(res.message || '获取订单失败')
      router.back()
    }
  } catch (error) {
    console.error('获取订单失败:', error)
    router.back()
  } finally {
    loading.value = false
  }
}

// 开始倒计时
const startCountdown = () => {
  countdownTimer = setInterval(() => {
    if (countdown.value > 0) {
      countdown.value--
    } else {
      clearInterval(countdownTimer)
      ElMessage.warning('订单已超时')
      router.push('/customer/orders')
    }
  }, 1000)
}

// 格式化倒计时
const formatCountdown = (seconds) => {
  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
}

// 发起支付
const handlePay = async () => {
  paying.value = true
  try {
    const res = await paymentApi.createPayment({
      tradeOrderId: tradeOrderId
    })
    if (res.code === '00000' && res.data.formHtml) {
      // 显示确认弹窗
      showStatusDialog.value = true

      // 插入支付宝表单并自动提交
      payForm.value = res.data.formHtml
      setTimeout(() => {
        const form = document.querySelector('.pay-form-container form')
        if (form) {
          form.target = '_blank'
          form.submit()
        }
      }, 100)
    } else {
      ElMessage.error(res.message || '创建支付失败')
    }
  } catch (error) {
    ElMessage.error('支付失败，请重试')
  } finally {
    paying.value = false
  }
}

// 检查支付状态
const checkPaymentStatus = async () => {
  try {
    const res = await paymentApi.queryPayment(tradeOrderId)
    if (res.code === 0) {
      if (res.data.status === 'PAID' || res.data.status === 'SETTLED') {
        ElMessage.success('支付成功')
        router.push(`/customer/progress/${tradeOrder.value.orderId}`)
      } else {
        ElMessage.warning('支付未完成，请继续支付')
      }
    }
  } catch (error) {
    ElMessage.error('查询支付状态失败')
  }
  showStatusDialog.value = false
}

// 取消订单
const handleCancel = async () => {
  try {
    await ElMessageBox.confirm('确定要取消订单吗？', '提示', {
      type: 'warning'
    })

    const res = await tradeApi.refund(tradeOrderId, { reason: '用户取消' })
    if (res.code === 0) {
      ElMessage.success('订单已取消')
      router.push('/customer/orders')
    } else {
      ElMessage.error(res.message || '取消失败')
    }
  } catch (e) {
    // 用户取消确认
  }
}

onMounted(() => {
  fetchTradeOrder()
  startCountdown()
})

onUnmounted(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
  }
})
</script>

<style lang="scss" scoped>
.payment-page {
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

.countdown-section {
  margin-bottom: 16px;

  .countdown-card {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 20px;
    background: #fff7e6;
    border-radius: 8px;
    color: #fa8c16;

    &.urgent {
      background: #fff2f0;
      color: #ff4d4f;
    }

    .countdown-info {
      display: flex;
      flex-direction: column;
      gap: 4px;

      .label {
        font-size: 14px;
      }

      .time {
        font-size: 24px;
        font-weight: bold;
      }
    }
  }
}

.order-section,
.payment-section {
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

.order-info {
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

      &.price {
        font-size: 20px;
        font-weight: bold;
        color: #f56c6c;
      }
    }

    &.total {
      padding-top: 16px;
      margin-top: 8px;
      border-top: 1px solid #eee;
    }
  }
}

.payment-methods {
  .payment-method {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 16px;
    border: 2px solid #eee;
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.3s;

    &.active {
      border-color: #409eff;
      background: #ecf5ff;
    }

    img {
      width: 32px;
      height: 32px;
    }

    .check-icon {
      margin-left: auto;
      color: #409eff;
      font-size: 20px;
    }
  }
}

.action-section {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.pay-form-container {
  display: none;
}

.status-dialog-content {
  text-align: center;
  padding: 20px 0;

  p {
    font-size: 16px;
    color: #333;
  }
}
</style>
