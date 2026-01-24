<template>
  <div v-loading="loading" class="settings-page">
    <div class="settings-card">
      <h3>流控设置</h3>

      <el-form :model="settings" label-width="150px" class="settings-form">
        <el-divider content-position="left">流量控制</el-divider>

        <el-form-item label="系统降级开关">
          <el-switch
            v-model="settings.downgradeSwitch"
            active-text="开启"
            inactive-text="关闭"
            @change="handleSwitchChange"
          />
          <span class="form-hint">开启后拒绝所有新订单（Key: activity.downgrade.switch）</span>
        </el-form-item>

        <el-form-item label="用户切量比例">
          <el-slider 
            v-model="settings.userCutRange" 
            :min="0" 
            :max="100" 
            show-input 
            @change="handleRangeChange"
          />
          <span class="form-hint">%（0表示全部拒绝，100表示全量放行）（Key: activity.cut.range）</span>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="fetchSettings">刷新配置</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '@/api/admin'

const loading = ref(false)

const settings = reactive({
  downgradeSwitch: false,
  userCutRange: 0
})

const fetchSettings = async () => {
  loading.value = true
  try {
    const res = await adminApi.getAllConfigs()
    if (res.code === '00000' && res.data) {
      // Map backend keys to frontend model
      settings.downgradeSwitch = res.data['activity.downgrade.switch'] === 'true'
      settings.userCutRange = parseInt(res.data['activity.cut.range'] || '0', 10)
    }
  } catch (error) {
    console.error('获取设置失败:', error)
    ElMessage.error('获取配置失败')
  } finally {
    loading.value = false
  }
}

const updateConfig = async (key, value) => {
  try {
    const res = await adminApi.updateConfig(key, String(value))
    if (res.code === '00000') {
      ElMessage.success('更新成功')
    } else {
      ElMessage.error(res.message || '更新失败')
      // Refresh to rollback on error
      fetchSettings()
    }
  } catch (error) {
    ElMessage.error('更新失败')
    fetchSettings()
  }
}

const handleSwitchChange = (val) => {
  updateConfig('activity.downgrade.switch', val)
}

const handleRangeChange = (val) => {
  updateConfig('activity.cut.range', val)
}

onMounted(() => {
  fetchSettings()
})
</script>

<style lang="scss" scoped>
.settings-page {
  max-width: 800px;

  .settings-card {
    background: #fff;
    border-radius: 8px;
    padding: 24px;

    h3 {
      font-size: 18px;
      color: #333;
      margin-bottom: 20px;
    }
  }

  .settings-form {
    .el-form-item {
      margin-bottom: 24px;
    }

    .el-slider {
      width: 300px;
    }
  }

  .form-hint {
    margin-left: 12px;
    color: #999;
    font-size: 12px;
  }
}
</style>
