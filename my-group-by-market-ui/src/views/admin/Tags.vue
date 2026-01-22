<template>
  <div class="tags-page">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input
        v-model="searchForm.keyword"
        placeholder="搜索标签名称"
        clearable
        style="width: 200px;"
        @keyup.enter="handleSearch"
      />
      <el-select v-model="searchForm.status" placeholder="标签状态" clearable style="width: 120px;">
        <el-option label="草稿" value="DRAFT" />
        <el-option label="计算中" value="CALCULATING" />
        <el-option label="已完成" value="COMPLETED" />
        <el-option label="失败" value="FAILED" />
      </el-select>
      <el-button type="primary" @click="handleSearch">搜索</el-button>
      <el-button @click="handleReset">重置</el-button>
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        创建标签
      </el-button>
    </div>

    <!-- 标签列表 -->
    <div class="table-container">
      <el-table v-loading="loading" :data="tagList" style="width: 100%">
        <el-table-column prop="tagId" label="标签ID" width="120" />
        <el-table-column prop="tagName" label="标签名称" min-width="150" />
        <el-table-column prop="tagRule" label="标签规则" min-width="200">
          <template #default="{ row }">
            <el-tooltip :content="row.tagRule" placement="top">
              <span class="rule-text">{{ row.tagRule }}</span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column prop="userCount" label="用户数" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160">
          <template #default="{ row }">
            {{ formatDate(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button
              v-if="row.status === 'DRAFT' || row.status === 'FAILED'"
              text
              type="success"
              @click="handleCalculate(row)"
            >
              计算
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
      :title="isEdit ? '编辑标签' : '创建标签'"
      width="600px"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="标签名称" prop="tagName">
          <el-input v-model="form.tagName" placeholder="请输入标签名称" />
        </el-form-item>
        <el-form-item label="标签规则" prop="tagRule">
          <el-input
            v-model="form.tagRule"
            type="textarea"
            :rows="4"
            placeholder="请输入 JSON 格式的标签规则"
          />
        </el-form-item>
        <el-form-item label="规则说明">
          <div class="rule-help">
            <p>示例：{"age": {"$gte": 18, "$lte": 35}, "city": "北京"}</p>
            <p>支持的操作符：$eq, $ne, $gt, $gte, $lt, $lte, $in, $nin</p>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api/admin'
import dayjs from 'dayjs'

const loading = ref(false)
const tagList = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref(null)

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
  tagId: '',
  tagName: '',
  tagRule: ''
})

const rules = {
  tagName: [{ required: true, message: '请输入标签名称', trigger: 'blur' }],
  tagRule: [{ required: true, message: '请输入标签规则', trigger: 'blur' }]
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await adminApi.getTags({
      page: pagination.page,
      size: pagination.size,
      keyword: searchForm.keyword,
      status: searchForm.status
    })
    if (res.code === '00000') {
      tagList.value = res.data.list || []
      pagination.total = res.data.total || 0
    }
  } catch (error) {
    console.error('获取标签列表失败:', error)
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
    tagId: '',
    tagName: '',
    tagRule: ''
  })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  Object.assign(form, {
    tagId: row.tagId,
    tagName: row.tagName,
    tagRule: row.tagRule
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  // 验证 JSON 格式
  try {
    JSON.parse(form.tagRule)
  } catch (e) {
    ElMessage.error('标签规则必须是有效的 JSON 格式')
    return
  }

  submitting.value = true
  try {
    let res
    if (isEdit.value) {
      res = await adminApi.updateTag(form.tagId, form)
    } else {
      res = await adminApi.createTag(form)
    }

    if (res.code === '00000') {
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

const handleCalculate = async (row) => {
  try {
    await ElMessageBox.confirm('确定要计算该标签的用户群吗？', '提示', { type: 'info' })
    const res = await adminApi.calculateTag(row.tagId)
    if (res.code === '00000') {
      ElMessage.success('计算任务已提交')
      fetchList()
    } else {
      ElMessage.error(res.message || '提交失败')
    }
  } catch (e) {
    // 用户取消
  }
}

const formatDate = (date) => {
  return date ? dayjs(date).format('YYYY-MM-DD HH:mm') : '-'
}

const getStatusTagType = (status) => {
  switch (status) {
    case 'DRAFT': return 'info'
    case 'CALCULATING': return 'warning'
    case 'COMPLETED': return 'success'
    case 'FAILED': return 'danger'
    default: return 'info'
  }
}

const getStatusText = (status) => {
  switch (status) {
    case 'DRAFT': return '草稿'
    case 'CALCULATING': return '计算中'
    case 'COMPLETED': return '已完成'
    case 'FAILED': return '失败'
    default: return status
  }
}

onMounted(() => {
  fetchList()
})
</script>

<style lang="scss" scoped>
.tags-page {
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

  .rule-text {
    display: block;
    max-width: 200px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }

  .rule-help {
    font-size: 12px;
    color: #999;

    p {
      margin: 4px 0;
    }
  }
}
</style>
