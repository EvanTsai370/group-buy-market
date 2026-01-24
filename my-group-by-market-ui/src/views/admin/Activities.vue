<template>
  <div class="activities-page">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input
        v-model="searchForm.keyword"
        placeholder="搜索活动名称"
        clearable
        style="width: 200px;"
        @keyup.enter="handleSearch"
      />
      <el-select v-model="searchForm.status" placeholder="活动状态" clearable style="width: 120px;">
        <el-option label="草稿" value="DRAFT" />
        <el-option label="进行中" value="ACTIVE" />
        <el-option label="已结束" value="CLOSED" />
      </el-select>
      <el-button type="primary" @click="handleSearch">搜索</el-button>
      <el-button @click="handleReset">重置</el-button>
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        创建活动
      </el-button>
    </div>

    <!-- 活动列表 -->
    <div class="table-container">
      <el-table v-loading="loading" :data="activityList" style="width: 100%">
        <el-table-column prop="activityId" label="活动ID" width="120" />
        <el-table-column prop="activityName" label="活动名称" min-width="150" />
        <el-table-column prop="target" label="成团人数" width="100" />
        <el-table-column prop="validTime" label="拼团有效期" width="120">
          <template #default="{ row }">
            {{ Math.floor(row.validTime / 3600) }} 小时
          </template>
        </el-table-column>
        <el-table-column prop="participationLimit" label="参与限制" width="120">
          <template #default="{ row }">
            {{ row.participationLimit }} 次/人
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="活动时间" width="180">
          <template #default="{ row }">
            <div class="time-range">
              <p>{{ formatDate(row.startTime) }}</p>
              <p>{{ formatDate(row.endTime) }}</p>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button
              v-if="row.status === 'DRAFT'"
              text
              type="success"
              @click="handleChangeStatus(row, 'ACTIVE')"
            >
              上线
            </el-button>
            <el-button
              v-if="row.status === 'ACTIVE'"
              text
              type="warning"
              @click="handleChangeStatus(row, 'CLOSED')"
            >
              下线
            </el-button>
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

    <!-- 编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑活动' : '创建活动'"
      width="700px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="活动名称" prop="activityName">
          <el-input v-model="form.activityName" placeholder="请输入活动名称" maxlength="100" show-word-limit />
        </el-form-item>

        <el-form-item label="关联SPU" prop="spuId">
          <div style="display: flex; gap: 8px; width: 100%;">
            <el-select
              v-model="form.spuId"
              placeholder="请选择商品"
              filterable
              clearable
              style="flex: 1;"
              @focus="loadSpuOptions"
            >
              <el-option
                v-for="spu in spuOptions"
                :key="spu.spuId"
                :label="spu.spuName"
                :value="spu.spuId"
              >
                <span style="float: left">{{ spu.spuName }}</span>
                <span style="float: right; color: #8492a6; font-size: 13px">{{ spu.spuId }}</span>
              </el-option>
            </el-select>
            <el-tooltip content="选择要参与拼团的商品" placement="top">
              <el-icon style="margin-top: 8px; cursor: help;"><InfoFilled /></el-icon>
            </el-tooltip>
          </div>
        </el-form-item>

        <el-form-item label="折扣配置" prop="discountId">
          <div style="display: flex; gap: 8px; width: 100%;">
            <el-select
              v-model="form.discountId"
              placeholder="请选择折扣策略"
              filterable
              clearable
              style="flex: 1;"
              @focus="loadDiscountOptions"
            >
              <el-option
                v-for="discount in discountOptions"
                :key="discount.discountId"
                :label="discount.discountName"
                :value="discount.discountId"
              >
                <div style="display: flex; justify-content: space-between;">
                  <span>{{ discount.discountName }}</span>
                  <span style="color: #8492a6; font-size: 13px">{{ getDiscountTypeText(discount.marketPlan) }}</span>
                </div>
              </el-option>
            </el-select>
            <el-button type="primary" text @click="showDiscountDialog">
              <el-icon><Plus /></el-icon>
              新建折扣
            </el-button>
          </div>
        </el-form-item>

        <el-form-item label="成团人数" prop="target">
          <el-input-number v-model="form.target" :min="2" :max="100" />
          <span class="form-hint">人</span>
        </el-form-item>

        <el-form-item label="拼团有效期" prop="validTime">
          <el-input-number v-model="form.validTime" :min="1" :max="168" />
          <span class="form-hint">小时（用户锁单后的成团倒计时）</span>
        </el-form-item>

        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker
            v-model="form.startTime"
            type="datetime"
            placeholder="选择开始时间"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%;"
          />
        </el-form-item>

        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker
            v-model="form.endTime"
            type="datetime"
            placeholder="选择结束时间"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%;"
          />
        </el-form-item>

        <el-form-item label="参与限制" prop="participationLimit">
          <el-input-number v-model="form.participationLimit" :min="1" />
          <span class="form-hint">每人最多参与次数</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 折扣创建弹窗 -->
    <el-dialog
      v-model="discountDialogVisible"
      title="创建折扣配置"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form ref="discountFormRef" :model="discountForm" :rules="discountRules" label-width="110px">
        <el-form-item label="折扣名称" prop="discountName">
          <el-input v-model="discountForm.discountName" placeholder="请输入折扣名称（唯一）" maxlength="100" show-word-limit />
        </el-form-item>

        <el-form-item label="折扣描述" prop="discountDesc">
          <el-input
            v-model="discountForm.discountDesc"
            type="textarea"
            placeholder="请输入折扣描述"
            :rows="2"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="折扣类型" prop="marketPlan">
          <el-select v-model="discountForm.marketPlan" placeholder="请选择折扣类型" style="width: 100%;">
            <el-option label="直减（减X元）" value="ZJ" />
            <el-option label="折扣（X折）" value="ZK" />
            <el-option label="N元购" value="NYG" />
            <el-option label="满减（满X减Y）" value="MJ" />
          </el-select>
        </el-form-item>

        <el-form-item label="折扣表达式" prop="marketExpr">
          <el-input v-model="discountForm.marketExpr" :placeholder="getMarketExprPlaceholder()" />
          <div class="form-hint" style="margin-top: 4px;">
            {{ getMarketExprHint() }}
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="discountDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="discountSubmitting" @click="handleCreateDiscount">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, InfoFilled } from '@element-plus/icons-vue'
import { adminApi } from '@/api/admin'
import dayjs from 'dayjs'

const loading = ref(false)
const activityList = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref(null)

// 折扣弹窗
const discountDialogVisible = ref(false)
const discountFormRef = ref(null)
const discountSubmitting = ref(false)

// 选择器选项
const spuOptions = ref([])
const discountOptions = ref([])

const searchForm = reactive({
  keyword: '',
  status: ''
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const form = reactive({
  activityId: '',
  activityName: '',
  spuId: '',
  discountId: '',
  target: 3,
  validTime: 24,
  startTime: '',
  endTime: '',
  participationLimit: 1
})

const discountForm = reactive({
  discountName: '',
  discountDesc: '',
  marketPlan: '',
  marketExpr: '',
  discountAmount: 0
})

const rules = {
  activityName: [{ required: true, message: '请输入活动名称', trigger: 'blur' }],
  spuId: [{ required: true, message: '请选择SPU', trigger: 'change' }],
  discountId: [{ required: true, message: '请选择折扣配置', trigger: 'change' }],
  target: [{ required: true, message: '请输入成团人数', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }]
}

const discountRules = {
  discountName: [
    { required: true, message: '请输入折扣名称', trigger: 'blur' },
    { min: 2, max: 100, message: '长度在 2 到 100 个字符', trigger: 'blur' }
  ],
  marketPlan: [{ required: true, message: '请选择折扣类型', trigger: 'change' }],
  marketExpr: [{ required: true, message: '请输入折扣表达式', trigger: 'blur' }]
}

// 加载 SPU 选项
const loadSpuOptions = async () => {
  if (spuOptions.value.length > 0) return

  try {
    const res = await adminApi.getSpuOptions()
    if (res.code === '00000') {
      spuOptions.value = res.data || []
    }
  } catch (error) {
    console.error('加载SPU选项失败:', error)
  }
}

// 加载折扣选项
const loadDiscountOptions = async () => {
  if (discountOptions.value.length > 0) return

  try {
    const res = await adminApi.getDiscounts()
    if (res.code === '00000') {
      discountOptions.value = res.data || []
    }
  } catch (error) {
    console.error('加载折扣选项失败:', error)
  }
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await adminApi.getActivities({
      page: pagination.page,
      size: pagination.size,
      keyword: searchForm.keyword,
      status: searchForm.status
    })
    if (res.code === '00000') {
      activityList.value = res.data.list || []
      pagination.total = res.data.total || 0
    }
  } catch (error) {
    console.error('获取活动列表失败:', error)
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
  handleSearch()
}

const handleAdd = () => {
  isEdit.value = false
  Object.assign(form, {
    activityId: '',
    activityName: '',
    spuId: '',
    discountId: '',
    target: 3,
    validTime: 24,
    startTime: '',
    endTime: '',
    participationLimit: 1
  })
  dialogVisible.value = true
}

const handleEdit = async (row) => {
  isEdit.value = true
  Object.assign(form, {
    activityId: row.activityId,
    activityName: row.activityName,
    spuId: '', // 先清空，等待加载
    discountId: row.discountId,
    target: row.target,
    validTime: Math.floor(row.validTime / 3600), // 秒转小时
    startTime: row.startTime,
    endTime: row.endTime,
    participationLimit: row.participationLimit
  })
  
  // 预加载 SPU 和折扣选项（确保下拉框能正确显示名称）
  await Promise.all([
    loadSpuOptions(),
    loadDiscountOptions()
  ])
  
  // 加载已关联的 SPU
  await loadActivitySpu(row.activityId)
  
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const data = {
      activityName: form.activityName,
      activityDesc: form.activityDesc,
      discountId: form.discountId,
      target: form.target,
      validTime: form.validTime * 3600, // 小时转秒
      participationLimit: form.participationLimit,
      startTime: form.startTime,
      endTime: form.endTime
    }

    let res
    if (isEdit.value) {
      res = await adminApi.updateActivity(form.activityId, data)
    } else {
      res = await adminApi.createActivity(data)
    }

    if (res.code === '00000') {
      // 保存或更新活动商品关联
      if (form.spuId) {
        const goodsData = {
          spuId: form.spuId,
          source: 's01', // 默认来源
          channel: 'c01' // 默认渠道
        }
        
        if (isEdit.value) {
          // 编辑时更新关联（删除旧的，创建新的）
          await adminApi.updateActivityGoods(form.activityId, goodsData)
        } else {
          // 创建时添加关联
          await adminApi.addActivityGoods(res.data.activityId, goodsData)
        }
      }
      
      ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
      dialogVisible.value = false
      fetchList()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } finally {
    submitting.value = false
  }
}

const handleChangeStatus = async (row, status) => {
  const actionText = status === 'ACTIVE' ? '上线' : '下线'
  try {
    await ElMessageBox.confirm(`确定要${actionText}该活动吗？`, '提示', { type: 'warning' })
    const res = await adminApi.updateActivityStatus(row.activityId, status)
    if (res.code === '00000') {
      ElMessage.success(`${actionText}成功`)
      fetchList()
    } else {
      ElMessage.error(res.message || `${actionText}失败`)
    }
  } catch (e) {
    // 用户取消
  }
}

// 显示折扣创建弹窗
const showDiscountDialog = () => {
  Object.assign(discountForm, {
    discountName: '',
    discountDesc: '',
    marketPlan: '',
    marketExpr: '',
    discountAmount: 0
  })
  discountDialogVisible.value = true
}

// 创建折扣
const handleCreateDiscount = async () => {
  const valid = await discountFormRef.value.validate().catch(() => false)
  if (!valid) return

  discountSubmitting.value = true
  try {
    const res = await adminApi.createDiscount(discountForm)
    if (res.code === '00000') {
      ElMessage.success('折扣创建成功')
      discountDialogVisible.value = false

      // 刷新折扣列表并自动选中新创建的折扣
      discountOptions.value = []
      await loadDiscountOptions()
      form.discountId = res.data.discountId
    } else {
      ElMessage.error(res.message || '创建失败')
    }
  } finally {
    discountSubmitting.value = false
  }
}

// 获取折扣表达式占位符
const getMarketExprPlaceholder = () => {
  switch (discountForm.marketPlan) {
    case 'ZJ': return '例如: 10 （减10元）'
    case 'ZK': return '例如: 0.9 （9折）'
    case 'NYG': return '例如: 9.9 （9.9元购）'
    case 'MJ': return '例如: 100:10 （满100减10）'
    default: return '请先选择折扣类型'
  }
}

// 获取折扣表达式提示
const getMarketExprHint = () => {
  switch (discountForm.marketPlan) {
    case 'ZJ': return '直接输入减免金额数字'
    case 'ZK': return '输入折扣比例（0-1之间，如0.9表示9折）'
    case 'NYG': return '输入固定售价'
    case 'MJ': return '格式: 满足金额:减免金额，例如 100:10'
    default: return ''
  }
}

// 获取折扣类型文本
const getDiscountTypeText = (marketPlan) => {
  switch (marketPlan) {
    case 'ZJ': return '直减'
    case 'ZK': return '折扣'
    case 'NYG': return 'N元购'
    case 'MJ': return '满减'
    default: return marketPlan
  }
}

const formatDate = (date) => {
  return date ? dayjs(date).format('MM-DD HH:mm') : '-'
}

const getStatusTagType = (status) => {
  switch (status) {
    case 'DRAFT': return 'info'
    case 'ACTIVE': return 'success'
    case 'CLOSED': return 'danger'
    default: return 'info'
  }
}

const getStatusText = (status) => {
  switch (status) {
    case 'DRAFT': return '草稿'
    case 'ACTIVE': return '进行中'
    case 'CLOSED': return '已结束'
    default: return status
  }
}

// 加载活动关联的 SPU
const loadActivitySpu = async (activityId) => {
  try {
    const res = await adminApi.getActivityGoods(activityId)
    if (res.code === '00000' && res.data && res.data.length > 0) {
      // 取第一个关联的 SPU（简化方案）
      form.spuId = res.data[0].spuId
    }
  } catch (error) {
    console.error('加载活动商品失败:', error)
  }
}

onMounted(() => {
  fetchList()
})
</script>

<style lang="scss" scoped>
.activities-page {
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

  .time-range {
    font-size: 12px;
    color: #666;

    p {
      margin: 2px 0;
    }
  }

  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }

  .form-hint {
    margin-left: 8px;
    color: #999;
    font-size: 12px;
  }
}
</style>
