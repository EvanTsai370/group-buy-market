<template>
  <div v-loading="loading" class="group-progress-page">
    <template v-if="progress">
      <!-- 拼团状态卡片 -->
      <div class="status-card" :class="statusClass">
        <div class="status-icon">
          <el-icon v-if="progress.status === 'SUCCESS'" :size="48"><CircleCheckFilled /></el-icon>
          <el-icon v-else-if="progress.status === 'FAILED'" :size="48"><CircleCloseFilled /></el-icon>
          <el-icon v-else :size="48"><Clock /></el-icon>
        </div>
        <div class="status-info">
          <h2>{{ statusText }}</h2>
          <p v-if="progress.status === 'PENDING'">
            还差 <strong>{{ progress.remainingCount }}</strong> 人成团
          </p>
          <p v-else-if="progress.status === 'SUCCESS'">
            恭喜！拼团已成功
          </p>
          <p v-else>
            很遗憾，拼团未成功
          </p>
        </div>
      </div>

      <!-- 进度条 -->
      <div class="progress-section">
        <div class="progress-header">
          <span>拼团进度</span>
          <span>{{ progress.completeCount }}/{{ progress.targetCount }}</span>
        </div>
        <el-progress
          :percentage="progress.progress"
          :stroke-width="12"
          :color="progressColor"
        />
        <div v-if="progress.status === 'PENDING' && progress.remainingSeconds > 0" class="countdown">
          <el-icon><Clock /></el-icon>
          <span>剩余 {{ formatCountdown(progress.remainingSeconds) }}</span>
        </div>
      </div>

      <!-- 商品信息 -->
      <div class="goods-section">
        <div class="section-title">商品信息</div>
        <div class="goods-card">
          <div class="goods-info">
            <h3>{{ progress.activityName }}</h3>
            <p>{{ progress.spuName }}</p>
          </div>
        </div>
      </div>

      <!-- 成员列表 -->
      <div class="members-section">
        <div class="section-title">
          <span>拼团成员</span>
          <span class="member-count">{{ progress.members?.length || 0 }}人</span>
        </div>
        <div class="member-list">
          <div
            v-for="(member, index) in progress.members"
            :key="member.userId"
            class="member-item"
          >
            <div class="member-info">
              <el-avatar :size="40" :src="member.avatar">
                {{ member.nickname?.charAt(0) }}
              </el-avatar>
              <div class="member-detail">
                <span class="nickname">
                  {{ member.nickname }}
                  <el-tag v-if="member.isLeader" size="small" type="warning">团长</el-tag>
                </span>
                <span class="sku-name">{{ member.skuName }}</span>
              </div>
            </div>
            <div class="member-status">
              <el-tag :type="getStatusTagType(member.status)" size="small">
                {{ getStatusText(member.status) }}
              </el-tag>
            </div>
          </div>
          <!-- 空位 -->
          <div
            v-for="i in (progress.targetCount - (progress.members?.length || 0))"
            :key="`empty-${i}`"
            class="member-item empty"
          >
            <div class="member-info">
              <el-avatar :size="40" :icon="Plus" />
              <span class="nickname">等待加入</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 分享按钮 -->
      <div v-if="progress.status === 'PENDING'" class="share-section">
        <el-button type="primary" size="large" @click="handleShare">
          <el-icon><Share /></el-icon>
          邀请好友参团
        </el-button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Share } from '@element-plus/icons-vue'
import { tradeApi } from '@/api/trade'

const route = useRoute()

const loading = ref(false)
const progress = ref(null)

let refreshTimer = null

const orderId = route.params.orderId

const statusClass = computed(() => {
  if (!progress.value) return ''
  switch (progress.value.status) {
    case 'SUCCESS': return 'success'
    case 'FAILED': return 'failed'
    default: return 'pending'
  }
})

const statusText = computed(() => {
  if (!progress.value) return ''
  switch (progress.value.status) {
    case 'SUCCESS': return '拼团成功'
    case 'FAILED': return '拼团失败'
    default: return '拼团中'
  }
})

const progressColor = computed(() => {
  if (!progress.value) return '#409eff'
  switch (progress.value.status) {
    case 'SUCCESS': return '#67c23a'
    case 'FAILED': return '#f56c6c'
    default: return '#409eff'
  }
})

// 获取拼团进度
const fetchProgress = async () => {
  loading.value = true
  try {
    const res = await tradeApi.getOrderProgress(orderId)
    if (res.code === '00000') {
      progress.value = res.data
    }
  } catch (error) {
    console.error('获取拼团进度失败:', error)
  } finally {
    loading.value = false
  }
}

// 格式化倒计时
const formatCountdown = (seconds) => {
  if (!seconds || seconds <= 0) return '即将结束'
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const secs = seconds % 60
  if (hours > 0) {
    return `${hours}小时${minutes}分${secs}秒`
  }
  return `${minutes}分${secs}秒`
}

// 获取状态标签类型
const getStatusTagType = (status) => {
  switch (status) {
    case 'PAID':
    case 'SETTLED':
      return 'success'
    case 'CREATE':
      return 'warning'
    default:
      return 'info'
  }
}

// 获取状态文本
const getStatusText = (status) => {
  switch (status) {
    case 'CREATE': return '待支付'
    case 'PAID': return '已支付'
    case 'SETTLED': return '已结算'
    default: return status
  }
}

// 分享
const handleShare = () => {
  const shareUrl = window.location.href
  if (navigator.clipboard) {
    navigator.clipboard.writeText(shareUrl)
    ElMessage.success('链接已复制，快去分享给好友吧！')
  } else {
    ElMessage.info(`分享链接：${shareUrl}`)
  }
}

onMounted(() => {
  fetchProgress()
  // 每 10 秒刷新一次（如果是 PENDING 状态）
  refreshTimer = setInterval(() => {
    if (progress.value?.status === 'PENDING') {
      fetchProgress()
    }
  }, 10000)
})

onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
})
</script>

<style lang="scss" scoped>
.group-progress-page {
  max-width: 600px;
  margin: 0 auto;
}

.status-card {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 30px;
  border-radius: 12px;
  margin-bottom: 20px;

  &.pending {
    background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
    color: #fff;
  }

  &.success {
    background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
    color: #fff;
  }

  &.failed {
    background: linear-gradient(135deg, #f56c6c 0%, #f89898 100%);
    color: #fff;
  }

  .status-info {
    h2 {
      font-size: 24px;
      margin-bottom: 8px;
    }

    p {
      font-size: 14px;
      opacity: 0.9;

      strong {
        font-size: 20px;
      }
    }
  }
}

.progress-section {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 16px;

  .progress-header {
    display: flex;
    justify-content: space-between;
    margin-bottom: 12px;
    font-size: 14px;
    color: #666;
  }

  .countdown {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 4px;
    margin-top: 12px;
    font-size: 14px;
    color: #f56c6c;
  }
}

.goods-section,
.members-section {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 16px;

  .section-title {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 16px;
    font-weight: bold;
    color: #333;
    margin-bottom: 16px;

    .member-count {
      font-size: 14px;
      font-weight: normal;
      color: #999;
    }
  }
}

.goods-card {
  .goods-info {
    h3 {
      font-size: 16px;
      color: #333;
      margin-bottom: 8px;
    }

    p {
      font-size: 14px;
      color: #666;
    }
  }
}

.member-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.member-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  background: #f9f9f9;
  border-radius: 8px;

  &.empty {
    opacity: 0.6;

    .el-avatar {
      background: #ddd;
    }
  }

  .member-info {
    display: flex;
    align-items: center;
    gap: 12px;

    .member-detail {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }

    .nickname {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 14px;
      color: #333;
    }

    .sku-name {
      font-size: 12px;
      color: #999;
    }
  }
}

.share-section {
  margin-top: 20px;

  .el-button {
    width: 100%;
  }
}
</style>
