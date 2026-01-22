<template>
  <div v-loading="loading" class="profile-page">
    <div class="page-header">
      <h2>个人中心</h2>
    </div>

    <template v-if="profile">
      <!-- 用户信息卡片 -->
      <div class="profile-card">
        <div class="avatar-section">
          <el-avatar :size="80" :src="profile.avatar">
            {{ profile.nickname?.charAt(0) }}
          </el-avatar>
          <div class="user-info">
            <h3>{{ profile.nickname }}</h3>
            <p class="user-id">ID: {{ profile.userId }}</p>
          </div>
        </div>

        <div class="info-list">
          <div class="info-item">
            <span class="label">用户名</span>
            <span class="value">{{ profile.username }}</span>
          </div>
          <div class="info-item">
            <span class="label">手机号</span>
            <span class="value">{{ profile.phone || '未绑定' }}</span>
          </div>
          <div class="info-item">
            <span class="label">邮箱</span>
            <span class="value">{{ profile.email || '未绑定' }}</span>
          </div>
          <div class="info-item">
            <span class="label">注册时间</span>
            <span class="value">{{ formatDate(profile.createTime) }}</span>
          </div>
        </div>
      </div>

      <!-- 快捷入口 -->
      <div class="quick-actions">
        <div class="action-item" @click="goOrders">
          <el-icon :size="32" color="#409eff"><List /></el-icon>
          <span>我的订单</span>
        </div>
        <div class="action-item" @click="goHome">
          <el-icon :size="32" color="#67c23a"><ShoppingCart /></el-icon>
          <span>继续购物</span>
        </div>
      </div>

      <!-- 退出登录 -->
      <div class="logout-section">
        <el-button type="danger" plain @click="handleLogout">退出登录</el-button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { userApi } from '@/api/user'
import dayjs from 'dayjs'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const profile = ref(null)

const fetchProfile = async () => {
  loading.value = true
  try {
    const res = await userApi.getProfile()
    if (res.code === '00000' && res.data) {
      profile.value = res.data
    }
  } catch (error) {
    console.error('获取用户资料失败:', error)
  } finally {
    loading.value = false
  }
}

const formatDate = (date) => {
  return date ? dayjs(date).format('YYYY-MM-DD') : '-'
}

const goOrders = () => {
  router.push('/customer/orders')
}

const goHome = () => {
  router.push('/customer/home')
}

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      type: 'warning'
    })
    userStore.logout()
  } catch (e) {
    // 用户取消
  }
}

onMounted(() => {
  fetchProfile()
})
</script>

<style lang="scss" scoped>
.profile-page {
  max-width: 600px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;

  h2 {
    font-size: 20px;
    color: #333;
  }
}

.profile-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 20px;

  .avatar-section {
    display: flex;
    align-items: center;
    gap: 20px;
    padding-bottom: 20px;
    border-bottom: 1px solid #eee;
    margin-bottom: 20px;

    .user-info {
      h3 {
        font-size: 20px;
        color: #333;
        margin-bottom: 4px;
      }

      .user-id {
        font-size: 12px;
        color: #999;
      }
    }
  }

  .info-list {
    .info-item {
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
}

.quick-actions {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-bottom: 20px;

  .action-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
    padding: 24px;
    background: #fff;
    border-radius: 12px;
    cursor: pointer;
    transition: all 0.3s;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    }

    span {
      font-size: 14px;
      color: #333;
    }
  }
}

.logout-section {
  text-align: center;

  .el-button {
    width: 100%;
  }
}
</style>
