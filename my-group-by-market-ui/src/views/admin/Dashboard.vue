<template>
  <div v-loading="loading" class="dashboard-page">
    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon" style="background: #409eff;">
          <el-icon :size="24"><ShoppingCart /></el-icon>
        </div>
        <div class="stat-info">
          <p class="stat-value">{{ stats.todayOrders || 0 }}</p>
          <p class="stat-label">今日订单</p>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon" style="background: #67c23a;">
          <el-icon :size="24"><Money /></el-icon>
        </div>
        <div class="stat-info">
          <p class="stat-value">¥{{ stats.todayGMV || '0.00' }}</p>
          <p class="stat-label">今日GMV</p>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon" style="background: #e6a23c;">
          <el-icon :size="24"><User /></el-icon>
        </div>
        <div class="stat-info">
          <p class="stat-value">{{ stats.todayUsers || 0 }}</p>
          <p class="stat-label">今日新增用户</p>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon" style="background: #f56c6c;">
          <el-icon :size="24"><Flag /></el-icon>
        </div>
        <div class="stat-info">
          <p class="stat-value">{{ stats.activeActivities || 0 }}</p>
          <p class="stat-label">进行中活动</p>
        </div>
      </div>
    </div>

    <!-- 快捷操作 -->
    <div class="section-card">
      <div class="section-header">
        <h3>快捷操作</h3>
      </div>
      <div class="quick-actions">
        <el-button type="primary" @click="$router.push('/admin/activities')">
          <el-icon><Plus /></el-icon>
          创建活动
        </el-button>
        <el-button @click="$router.push('/admin/goods')">
          <el-icon><Goods /></el-icon>
          商品管理
        </el-button>
        <el-button @click="$router.push('/admin/orders')">
          <el-icon><List /></el-icon>
          订单查询
        </el-button>
        <el-button @click="$router.push('/admin/users')">
          <el-icon><User /></el-icon>
          用户管理
        </el-button>
      </div>
    </div>

    <!-- 最近订单 -->
    <div class="section-card">
      <div class="section-header">
        <h3>最近订单</h3>
        <el-button text type="primary" @click="$router.push('/admin/orders')">
          查看全部
        </el-button>
      </div>
      <el-table :data="recentOrders" style="width: 100%">
        <el-table-column prop="tradeOrderId" label="订单号" width="180" />
        <el-table-column prop="spuName" label="商品" />
        <el-table-column prop="payPrice" label="金额" width="100">
          <template #default="{ row }">
            ¥{{ row.payPrice }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createTime) }}
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { adminApi } from '@/api/admin'
import dayjs from 'dayjs'

const loading = ref(false)
const stats = ref({})
const recentOrders = ref([])

const fetchDashboard = async () => {
  loading.value = true
  try {
    const res = await adminApi.getDashboardStats()
    if (res.code === '00000') {
      stats.value = res.data || {}
      recentOrders.value = res.data.recentOrders || []
    }
  } catch (error) {
    console.error('获取仪表盘数据失败:', error)
  } finally {
    loading.value = false
  }
}

const formatDate = (date) => {
  return date ? dayjs(date).format('MM-DD HH:mm') : '-'
}

const getStatusTagType = (status) => {
  switch (status) {
    case 'CREATE': return 'warning'
    case 'PAID': return ''
    case 'SETTLED': return 'success'
    case 'REFUND': return 'info'
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

onMounted(() => {
  fetchDashboard()
})
</script>

<style lang="scss" scoped>
.dashboard-page {
  .stats-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 20px;
    margin-bottom: 20px;

    @media (max-width: 1200px) {
      grid-template-columns: repeat(2, 1fr);
    }

    @media (max-width: 768px) {
      grid-template-columns: 1fr;
    }
  }

  .stat-card {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 20px;
    background: #fff;
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

    .stat-icon {
      width: 56px;
      height: 56px;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
    }

    .stat-info {
      .stat-value {
        font-size: 24px;
        font-weight: bold;
        color: #333;
        margin-bottom: 4px;
      }

      .stat-label {
        font-size: 14px;
        color: #999;
      }
    }
  }

  .section-card {
    background: #fff;
    border-radius: 8px;
    padding: 20px;
    margin-bottom: 20px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

    .section-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;

      h3 {
        font-size: 16px;
        color: #333;
      }
    }
  }

  .quick-actions {
    display: flex;
    gap: 12px;
    flex-wrap: wrap;
  }
}
</style>
