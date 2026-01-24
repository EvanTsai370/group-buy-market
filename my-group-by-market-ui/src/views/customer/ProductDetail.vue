<template>
  <div v-loading="loading" class="product-detail">
    <template v-if="product">
      <!-- 商品基本信息 -->
      <div class="product-main">
        <div class="product-image">
          <img :src="product.mainImage || defaultImage" :alt="product.spuName" />
        </div>

        <div class="product-info">
          <h1 class="product-name">{{ product.spuName }}</h1>
          <p class="product-desc">{{ product.description }}</p>

          <!-- 价格信息 -->
          <div class="price-section">
            <div class="group-price">
              <span class="label">拼团价</span>
              <span class="price">¥{{ trialResult?.discountPrice || selectedSku?.groupPrice || '--' }}</span>
            </div>
            <div class="original-price">
              <span class="label">原价</span>
              <span class="price">¥{{ selectedSku?.originalPrice || '--' }}</span>
            </div>
          </div>

          <!-- SKU 选择 -->
          <div v-if="product.skuList?.length > 0" class="sku-section">
            <div class="section-title">规格选择</div>
            <div class="sku-list">
              <div
                v-for="sku in product.skuList"
                :key="sku.skuId"
                class="sku-item"
                :class="{ active: selectedSku?.skuId === sku.skuId, disabled: sku.availableStock <= 0 }"
                @click="selectSku(sku)"
              >
                {{ sku.goodsName }}
                <span v-if="sku.availableStock <= 0" class="sold-out">售罄</span>
              </div>
            </div>
          </div>

          <!-- 活动信息 -->
          <div v-if="product.activity" class="activity-section">
            <div class="activity-card">
              <div class="activity-header">
                <el-tag type="danger">{{ product.activity.target }}人拼团</el-tag>
                <span class="activity-time">
                  <el-icon><Clock /></el-icon>
                  活动截止: {{ formatDate(product.activity.endTime) }}
                </span>
              </div>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="action-section">
            <el-button
              type="danger"
              size="large"
              :disabled="!canParticipate"
              @click="handleCreateTeam"
            >
              <el-icon><Plus /></el-icon>
              发起拼团
            </el-button>
            <el-button
              v-if="teamList.length > 0"
              type="primary"
              size="large"
              :disabled="!canParticipate"
              @click="showTeamDialog = true"
            >
              <el-icon><Connection /></el-icon>
              参与拼团 ({{ teamList.length }})
            </el-button>
          </div>

          <div v-if="!canParticipate && trialResult?.reason" class="cannot-participate">
            <el-alert :title="trialResult.reason" type="warning" :closable="false" />
          </div>
        </div>
      </div>

      <!-- 进行中的拼团 -->
      <div v-if="teamList.length > 0" class="team-section">
        <div class="section-header">
          <h3>正在拼团</h3>
          <span class="sub-title">加入已有团队，更快成团</span>
        </div>
        <div class="team-list">
          <div v-for="team in teamList" :key="team.orderId" class="team-card">
            <div class="team-leader">
              <el-avatar :size="40">{{ team.leaderNickname?.charAt(0) }}</el-avatar>
              <span class="name">{{ team.leaderNickname }}</span>
            </div>
            <div class="team-progress">
              <span>还差 <strong>{{ team.targetCount - team.currentCount }}</strong> 人成团</span>
              <el-progress
                :percentage="(team.currentCount / team.targetCount) * 100"
                :show-text="false"
              />
            </div>
            <div class="team-countdown">
              <el-icon><Clock /></el-icon>
              <span>{{ formatCountdown(team.remainingSeconds) }}</span>
            </div>
            <el-button type="primary" size="small" @click="handleJoinTeam(team)">
              参与
            </el-button>
          </div>
        </div>
      </div>
    </template>

    <!-- 参团弹窗 -->
    <el-dialog v-model="showTeamDialog" title="选择拼团队伍" width="500px">
      <div class="team-dialog-list">
        <div
          v-for="team in teamList"
          :key="team.orderId"
          class="team-dialog-item"
          @click="handleJoinTeam(team)"
        >
          <div class="leader-info">
            <el-avatar :size="32">{{ team.leaderNickname?.charAt(0) }}</el-avatar>
            <span>{{ team.leaderNickname }} 的团</span>
          </div>
          <div class="team-status">
            {{ team.currentCount }}/{{ team.targetCount }}人
          </div>
          <div class="countdown">
            {{ formatCountdown(team.remainingSeconds) }}
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { goodsApi } from '@/api/goods'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const product = ref(null)
const selectedSku = ref(null)
const trialResult = ref(null)
const teamList = ref([])
const showTeamDialog = ref(false)
const defaultImage = 'https://via.placeholder.com/400x400?text=No+Image'

const spuId = computed(() => route.params.spuId)
const canParticipate = computed(() => trialResult.value?.canParticipate !== false)

// 获取商品详情
const fetchProduct = async () => {
  loading.value = true
  try {
    const res = await goodsApi.getSpuDetail(spuId.value)
    if (res.code === '00000') {
      // 将后端返回的扁平活动字段转换为嵌套的 activity 对象
      const data = res.data
      if (data.hasActivity && data.activityId) {
        data.activity = {
          activityId: data.activityId,
          activityName: data.activityName,
          target: data.targetCount,
          endTime: data.activityEndTime,
          validTime: data.validTime
        }
      }
      product.value = data
      
      // 默认选中第一个有库存的 SKU
      if (data.skuList?.length > 0) {
        const availableSku = data.skuList.find(s => s.availableStock > 0)
        if (availableSku) {
          selectedSku.value = availableSku
        }
      }
    }
  } catch (error) {
    console.error('获取商品详情失败:', error)
  } finally {
    loading.value = false
  }
}

// 获取拼团队伍列表
const fetchTeamList = async () => {
  try {
    const res = await goodsApi.getTeamList(spuId.value)
    if (res.code === '00000') {
      teamList.value = res.data || []
    }
  } catch (error) {
    console.error('获取拼团列表失败:', error)
  }
}

// 价格试算
const fetchTrialPrice = async () => {
  if (!selectedSku.value) return
  try {
    const res = await goodsApi.trialPrice(selectedSku.value.skuId)
    if (res.code === '00000') {
      trialResult.value = res.data
    }
  } catch (error) {
    console.error('价格试算失败:', error)
  }
}

// 选择 SKU
const selectSku = (sku) => {
  if (sku.availableStock <= 0) return
  selectedSku.value = sku
}

// SKU 变化时重新试算价格
watch(selectedSku, () => {
  if (selectedSku.value) {
    fetchTrialPrice()
  }
})

// 发起拼团
const handleCreateTeam = () => {
  if (!selectedSku.value) {
    ElMessage.warning('请选择商品规格')
    return
  }
  if (!product.value.activity?.activityId) {
    ElMessage.warning('当前商品没有进行中的拼团活动')
    return
  }
  router.push({
    path: '/customer/lock-order',
    query: {
      spuId: spuId.value,
      skuId: selectedSku.value.skuId,
      activityId: product.value.activity.activityId,
      // 传递商品信息，避免 LockOrder 页面再次调用 API
      spuName: product.value.spuName,
      skuName: selectedSku.value.goodsName,
      mainImage: product.value.mainImage,
      originalPrice: selectedSku.value.originalPrice,
      payPrice: trialResult.value?.discountPrice || selectedSku.value.groupPrice,
      targetCount: product.value.activity.target,
      validHours: Math.floor((product.value.activity.validTime || 86400) / 3600),
      // 营销归因字段
      source: 'WEB',
      channel: 'PRODUCT_DETAIL'
    }
  })
}

// 参与拼团
const handleJoinTeam = (team) => {
  if (!selectedSku.value) {
    ElMessage.warning('请选择商品规格')
    return
  }
  if (!product.value.activity?.activityId) {
    ElMessage.warning('当前商品没有进行中的拼团活动')
    return
  }
  showTeamDialog.value = false
  router.push({
    path: '/customer/lock-order',
    query: {
      spuId: spuId.value,
      skuId: selectedSku.value.skuId,
      activityId: product.value.activity.activityId,
      orderId: team.orderId,
      // 传递商品信息,避免 LockOrder 页面再次调用 API
      spuName: product.value.spuName,
      skuName: selectedSku.value.goodsName,
      mainImage: product.value.mainImage,
      originalPrice: selectedSku.value.originalPrice,
      payPrice: trialResult.value?.discountPrice || selectedSku.value.groupPrice,
      targetCount: product.value.activity.target,
      validHours: Math.floor((product.value.activity.validTime || 86400) / 3600),
      // 营销归因字段
      source: 'WEB',
      channel: 'JOIN_TEAM'
    }
  })
}

const formatDate = (time) => {
  return dayjs(time).format('MM-DD HH:mm')
}

const formatCountdown = (seconds) => {
  if (!seconds || seconds <= 0) return '即将结束'
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  if (hours > 24) {
    return `${Math.floor(hours / 24)}天${hours % 24}时`
  }
  return `${hours}时${minutes}分`
}

onMounted(() => {
  fetchProduct()
  fetchTeamList()
})
</script>

<style lang="scss" scoped>
.product-detail {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
}

.product-main {
  display: flex;
  gap: 40px;

  @media (max-width: 768px) {
    flex-direction: column;
  }
}

.product-image {
  flex-shrink: 0;
  width: 400px;
  height: 400px;
  border-radius: 8px;
  overflow: hidden;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  @media (max-width: 768px) {
    width: 100%;
    height: 300px;
  }
}

.product-info {
  flex: 1;

  .product-name {
    font-size: 24px;
    color: #333;
    margin-bottom: 12px;
  }

  .product-desc {
    color: #666;
    font-size: 14px;
    margin-bottom: 20px;
  }
}

.price-section {
  display: flex;
  align-items: baseline;
  gap: 24px;
  padding: 16px;
  background: #fff8f8;
  border-radius: 8px;
  margin-bottom: 20px;

  .group-price {
    .label {
      font-size: 12px;
      color: #f56c6c;
      margin-right: 4px;
    }
    .price {
      font-size: 32px;
      font-weight: bold;
      color: #f56c6c;
    }
  }

  .original-price {
    .label {
      font-size: 12px;
      color: #999;
      margin-right: 4px;
    }
    .price {
      font-size: 16px;
      color: #999;
      text-decoration: line-through;
    }
  }
}

.sku-section {
  margin-bottom: 20px;

  .section-title {
    font-size: 14px;
    color: #333;
    margin-bottom: 12px;
  }

  .sku-list {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
  }

  .sku-item {
    padding: 8px 16px;
    border: 1px solid #ddd;
    border-radius: 4px;
    cursor: pointer;
    font-size: 14px;
    transition: all 0.3s;

    &:hover {
      border-color: #409eff;
    }

    &.active {
      border-color: #409eff;
      color: #409eff;
      background: #ecf5ff;
    }

    &.disabled {
      color: #ccc;
      cursor: not-allowed;
      background: #f5f5f5;

      &:hover {
        border-color: #ddd;
      }
    }

    .sold-out {
      font-size: 12px;
      color: #999;
      margin-left: 4px;
    }
  }
}

.activity-section {
  margin-bottom: 20px;

  .activity-card {
    padding: 12px 16px;
    background: #fef0f0;
    border-radius: 8px;

    .activity-header {
      display: flex;
      align-items: center;
      justify-content: space-between;

      .activity-time {
        display: flex;
        align-items: center;
        gap: 4px;
        font-size: 12px;
        color: #f56c6c;
      }
    }
  }
}

.action-section {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;

  .el-button {
    flex: 1;
  }
}

.cannot-participate {
  margin-top: 12px;
}

.team-section {
  margin-top: 40px;
  padding-top: 24px;
  border-top: 1px solid #eee;

  .section-header {
    margin-bottom: 16px;

    h3 {
      font-size: 18px;
      color: #333;
      margin-bottom: 4px;
    }

    .sub-title {
      font-size: 12px;
      color: #999;
    }
  }
}

.team-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.team-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: #f9f9f9;
  border-radius: 8px;

  .team-leader {
    display: flex;
    align-items: center;
    gap: 8px;
    min-width: 120px;

    .name {
      font-size: 14px;
      color: #333;
    }
  }

  .team-progress {
    flex: 1;

    span {
      font-size: 12px;
      color: #666;
      margin-bottom: 4px;
      display: block;

      strong {
        color: #f56c6c;
      }
    }
  }

  .team-countdown {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 12px;
    color: #f56c6c;
    min-width: 80px;
  }
}

.team-dialog-list {
  .team-dialog-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px;
    border-radius: 8px;
    cursor: pointer;
    transition: background 0.3s;

    &:hover {
      background: #f5f7fa;
    }

    .leader-info {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .team-status {
      color: #409eff;
    }

    .countdown {
      font-size: 12px;
      color: #f56c6c;
    }
  }
}
</style>
