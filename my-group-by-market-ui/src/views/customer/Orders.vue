<template>
  <div class="orders-page">
    <div class="page-header">
      <h2>我的订单</h2>
    </div>

    <!-- 状态筛选 -->
    <div class="filter-section">
      <el-radio-group v-model="statusFilter" @change="handleFilterChange">
        <el-radio-button label="">全部</el-radio-button>
        <el-radio-button label="CREATE">待支付</el-radio-button>
        <el-radio-button label="PAID">已支付</el-radio-button>
        <el-radio-button label="SETTLED">已完成</el-radio-button>
        <el-radio-button label="REFUND">已退款</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 订单列表 -->
    <div v-loading="loading" class="order-list">
      <template v-if="orderList.length > 0">
        <div v-for="order in orderList" :key="order.tradeOrderId" class="order-card">
          <div class="order-header">
            <span class="order-no">订单号：{{ order.tradeOrderId }}</span>
            <el-tag :type="getStatusTagType(order.status)" size="small">
              {{ getStatusText(order.status) }}
            </el-tag>
          </div>

          <div class="order-content">
            <div class="goods-info">
              <img :src="order.mainImage || defaultImage" :alt="order.spuName" class="goods-image" />
              <div class="goods-detail">
                <h4>{{ order.spuName }}</h4>
                <p class="sku-name">{{ order.skuName }}</p>
                <div class="price">
                  <span class="pay-price">¥{{ order.payPrice }}</span>
                  <span class="original-price">¥{{ order.originalPrice }}</span>
                </div>
              </div>
            </div>

            <div class="order-info">
              <p>下单时间：{{ formatDate(order.createTime) }}</p>
              <p v-if="order.status === 'PAID' || order.status === 'SETTLED'">
                拼团进度：{{ order.completeCount }}/{{ order.targetCount }}
              </p>
            </div>
          </div>

          <div class="order-actions">
            <el-button
              v-if="order.status === 'CREATE'"
              type="danger"
              size="small"
              @click="handlePay(order)"
            >
              去支付
            </el-button>
            <el-button
              v-if="order.status === 'PAID' || order.status === 'SETTLED'"
              type="primary"
              size="small"
              @click="handleViewProgress(order)"
            >
              查看进度
            </el-button>
            <el-button
              v-if="order.status === 'CREATE' || order.status === 'PAID'"
              size="small"
              @click="handleRefund(order)"
            >
              申请退款
            </el-button>
          </div>
        </div>
      </template>
      <el-empty v-else description="暂无订单" />
    </div>

    <!-- 分页 -->
    <div v-if="total > pageSize" class="pagination">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="fetchOrders"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { userApi } from '@/api/user'
import { tradeApi } from '@/api/trade'
import dayjs from 'dayjs'

const router = useRouter()

const loading = ref(false)
const orderList = ref([])
const statusFilter = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const defaultImage = 'https://via.placeholder.com/80x80?text=No+Image'

const fetchOrders = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value
    }
    if (statusFilter.value) {
      params.status = statusFilter.value
    }

    const res = await userApi.getOrders(params)
    if (res.code === 0) {
      orderList.value = res.data.list || []
      total.value = res.data.total || 0
    }
  } catch (error) {
    console.error('获取订单列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleFilterChange = () => {
  currentPage.value = 1
  fetchOrders()
}

const formatDate = (date) => {
  return date ? dayjs(date).format('YYYY-MM-DD HH:mm') : '-'
}

const getStatusTagType = (status) => {
  switch (status) {
    case 'CREATE': return 'warning'
    case 'PAID': return ''
    case 'SETTLED': return 'success'
    case 'REFUND': return 'info'
    case 'TIMEOUT': return 'danger'
    default: return 'info'
  }
}

const getStatusText = (status) => {
  switch (status) {
    case 'CREATE': return '待支付'
    case 'PAID': return '已支付'
    case 'SETTLED': return '已完成'
    case 'REFUND': return '已退款'
    case 'TIMEOUT': return '已超时'
    default: return status
  }
}

const handlePay = (order) => {
  router.push(`/customer/payment/${order.tradeOrderId}`)
}

const handleViewProgress = (order) => {
  router.push(`/customer/progress/${order.orderId}`)
}

const handleRefund = async (order) => {
  try {
    await ElMessageBox.confirm('确定要申请退款吗？', '提示', {
      type: 'warning'
    })

    const res = await tradeApi.refund(order.tradeOrderId, { reason: '用户申请退款' })
    if (res.code === 0) {
      ElMessage.success('退款申请成功')
      fetchOrders()
    } else {
      ElMessage.error(res.message || '退款申请失败')
    }
  } catch (e) {
    // 用户取消
  }
}

onMounted(() => {
  fetchOrders()
})
</script>

<style lang="scss" scoped>
.orders-page {
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

.filter-section {
  margin-bottom: 20px;
}

.order-list {
  min-height: 300px;
}

.order-card {
  background: #fff;
  border-radius: 8px;
  margin-bottom: 16px;
  overflow: hidden;

  .order-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 16px;
    background: #f9f9f9;
    border-bottom: 1px solid #eee;

    .order-no {
      font-size: 12px;
      color: #999;
    }
  }

  .order-content {
    padding: 16px;

    .goods-info {
      display: flex;
      gap: 12px;
      margin-bottom: 12px;

      .goods-image {
        width: 80px;
        height: 80px;
        border-radius: 4px;
        object-fit: cover;
      }

      .goods-detail {
        flex: 1;

        h4 {
          font-size: 14px;
          color: #333;
          margin-bottom: 4px;
        }

        .sku-name {
          font-size: 12px;
          color: #999;
          margin-bottom: 8px;
        }

        .price {
          .pay-price {
            font-size: 16px;
            font-weight: bold;
            color: #f56c6c;
          }

          .original-price {
            font-size: 12px;
            color: #999;
            text-decoration: line-through;
            margin-left: 8px;
          }
        }
      }
    }

    .order-info {
      font-size: 12px;
      color: #999;

      p {
        margin-bottom: 4px;
      }
    }
  }

  .order-actions {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
    padding: 12px 16px;
    border-top: 1px solid #eee;
  }
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
