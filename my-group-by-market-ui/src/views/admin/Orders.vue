<template>
  <div class="orders-page">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input
        v-model="searchForm.keyword"
        placeholder="订单号/用户ID"
        clearable
        style="width: 200px;"
        @keyup.enter="handleSearch"
      />
      <el-select v-model="searchForm.status" placeholder="订单状态" clearable style="width: 120px;">
        <el-option label="待支付" value="CREATE" />
        <el-option label="已支付" value="PAID" />
        <el-option label="已完成" value="SETTLED" />
        <el-option label="已退款" value="REFUND" />
        <el-option label="已超时" value="TIMEOUT" />
      </el-select>
      <el-date-picker
        v-model="searchForm.dateRange"
        type="daterange"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        format="YYYY-MM-DD"
        value-format="YYYY-MM-DD"
        style="width: 240px;"
      />
      <el-button type="primary" @click="handleSearch">搜索</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <!-- 订单列表 -->
    <div class="table-container">
      <el-table v-loading="loading" :data="orderList" style="width: 100%">
        <el-table-column prop="tradeOrderId" label="订单号" width="180" />
        <el-table-column prop="userId" label="用户ID" width="120" />
        <el-table-column prop="spuName" label="商品" min-width="150">
          <template #default="{ row }">
            <div class="goods-cell">
              <p class="goods-name">{{ row.spuName }}</p>
              <p class="sku-name">{{ row.skuName }}</p>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="originalPrice" label="原价" width="100">
          <template #default="{ row }">
            ¥{{ row.originalPrice }}
          </template>
        </el-table-column>
        <el-table-column prop="payPrice" label="实付" width="100">
          <template #default="{ row }">
            <span class="pay-price">¥{{ row.payPrice }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="下单时间" width="160">
          <template #default="{ row }">
            {{ formatDate(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="handleViewDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          layout="total, prev, pager, next"
          @current-change="fetchList"
        />
      </div>
    </div>

    <!-- 订单详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="订单详情" width="600px">
      <el-descriptions :column="2" border v-if="currentOrder">
        <el-descriptions-item label="订单号">{{ currentOrder.tradeOrderId }}</el-descriptions-item>
        <el-descriptions-item label="外部交易号">{{ currentOrder.outTradeNo }}</el-descriptions-item>
        <el-descriptions-item label="用户ID">{{ currentOrder.userId }}</el-descriptions-item>
        <el-descriptions-item label="订单状态">
          <el-tag :type="getStatusTagType(currentOrder.status)" size="small">
            {{ getStatusText(currentOrder.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="商品名称" :span="2">{{ currentOrder.spuName }}</el-descriptions-item>
        <el-descriptions-item label="规格">{{ currentOrder.skuName }}</el-descriptions-item>
        <el-descriptions-item label="活动ID">{{ currentOrder.activityId }}</el-descriptions-item>
        <el-descriptions-item label="原价">¥{{ currentOrder.originalPrice }}</el-descriptions-item>
        <el-descriptions-item label="实付">¥{{ currentOrder.payPrice }}</el-descriptions-item>
        <el-descriptions-item label="拼团订单ID">{{ currentOrder.orderId }}</el-descriptions-item>
        <el-descriptions-item label="拼团进度">
          {{ currentOrder.completeCount }}/{{ currentOrder.targetCount }}
        </el-descriptions-item>
        <el-descriptions-item label="下单时间">{{ formatDate(currentOrder.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="支付时间">{{ formatDate(currentOrder.payTime) }}</el-descriptions-item>
        <el-descriptions-item label="退款原因" :span="2" v-if="currentOrder.refundReason">
          {{ currentOrder.refundReason }}
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { adminApi } from '@/api/admin'
import dayjs from 'dayjs'

const loading = ref(false)
const orderList = ref([])
const detailDialogVisible = ref(false)
const currentOrder = ref(null)

const searchForm = reactive({
  keyword: '',
  status: '',
  dateRange: []
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const fetchList = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      size: pagination.size,
      keyword: searchForm.keyword,
      status: searchForm.status
    }
    if (searchForm.dateRange?.length === 2) {
      params.startDate = searchForm.dateRange[0]
      params.endDate = searchForm.dateRange[1]
    }

    const res = await adminApi.getOrders(params)
    // 后端返回的成功码是字符串 "00000"
    if (res.code === '00000') {
      orderList.value = res.data.list || []
      pagination.total = res.data.total || 0
    }
  } catch (error) {
    console.error('获取订单列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  fetchList()
}

const handleReset = () => {
  searchForm.keyword = ''
  searchForm.status = ''
  searchForm.dateRange = []
  handleSearch()
}

const handleViewDetail = async (row) => {
  try {
    const res = await adminApi.getOrder(row.tradeOrderId || row.orderId)
    // 后端返回的成功码是字符串 "00000"
    if (res.code === '00000') {
      currentOrder.value = res.data
      detailDialogVisible.value = true
    }
  } catch (error) {
    console.error('获取订单详情失败:', error)
  }
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

onMounted(() => {
  fetchList()
})
</script>

<style lang="scss" scoped>
.orders-page {
  .search-bar {
    display: flex;
    gap: 12px;
    margin-bottom: 20px;
    flex-wrap: wrap;
  }

  .table-container {
    background: #fff;
    border-radius: 8px;
    padding: 20px;
  }

  .goods-cell {
    .goods-name {
      font-size: 14px;
      color: #333;
    }

    .sku-name {
      font-size: 12px;
      color: #999;
    }
  }

  .pay-price {
    color: #f56c6c;
    font-weight: bold;
  }

  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
