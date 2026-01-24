<template>
  <div class="discounts-page">
    <!-- 搜索栏 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="折扣名称">
          <el-input v-model="searchForm.discountName" placeholder="请输入折扣名称" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="handleCreate">+ 创建折扣</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 折扣列表 -->
    <el-card class="table-card">
      <el-table v-loading="loading" :data="discountList" border stripe>
        <el-table-column prop="discountId" label="折扣ID" width="200" />
        <el-table-column prop="discountName" label="折扣名称" width="150" />
        <el-table-column prop="discountDesc" label="折扣描述" min-width="200" />
        <el-table-column prop="marketPlan" label="营销计划" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.marketPlan === 'ZJ'" type="success">直减</el-tag>
            <el-tag v-else-if="row.marketPlan === 'ZK'" type="primary">折扣</el-tag>
            <el-tag v-else-if="row.marketPlan === 'N'" type="warning">N元购</el-tag>
            <el-tag v-else-if="row.marketPlan === 'MJ'" type="info">满减</el-tag>
            <el-tag v-else>{{ row.marketPlan }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="marketExpr" label="表达式" width="100" />
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchList"
          @current-change="fetchList"
        />
      </div>
    </el-card>

    <!-- 创建/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑折扣' : '创建折扣'"
      width="600px"
      @close="handleDialogClose"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="折扣名称" prop="discountName">
          <el-input v-model="form.discountName" placeholder="请输入折扣名称" />
        </el-form-item>
        <el-form-item label="折扣描述" prop="discountDesc">
          <el-input v-model="form.discountDesc" type="textarea" placeholder="请输入折扣描述" />
        </el-form-item>
        <el-form-item label="营销计划" prop="marketPlan">
          <el-select v-model="form.marketPlan" placeholder="请选择营销计划">
            <el-option label="直减" value="ZJ" />
            <el-option label="折扣" value="ZK" />
            <el-option label="N元购" value="N" />
            <el-option label="满减" value="MJ" />
          </el-select>
        </el-form-item>
        <el-form-item label="表达式" prop="marketExpr">
          <el-input v-model="form.marketExpr" placeholder="例如：0.9（9折）或 10（减10元）" />
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
const submitting = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)

const searchForm = reactive({
  discountName: ''
})

const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

const discountList = ref([])

const form = reactive({
  discountId: '',
  discountName: '',
  discountDesc: '',
  marketPlan: '',
  marketExpr: ''
})

const rules = {
  discountName: [{ required: true, message: '请输入折扣名称', trigger: 'blur' }],
  marketPlan: [{ required: true, message: '请选择营销计划', trigger: 'change' }],
  marketExpr: [{ required: true, message: '请输入表达式', trigger: 'blur' }]
}

// 获取折扣列表
const fetchList = async () => {
  loading.value = true
  try {
    const res = await adminApi.getDiscountsPage({
      page: pagination.page,
      size: pagination.size
    })
    if (res.code === '00000') {
      discountList.value = res.data.list || []
      pagination.total = res.data.total || 0
    }
  } catch (error) {
    console.error('获取折扣列表失败:', error)
    ElMessage.error('获取折扣列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.page = 1
  fetchList()
}

// 重置
const handleReset = () => {
  searchForm.discountName = ''
  pagination.page = 1
  fetchList()
}

// 创建
const handleCreate = () => {
  isEdit.value = false
  Object.assign(form, {
    discountId: '',
    discountName: '',
    discountDesc: '',
    marketPlan: '',
    marketExpr: ''
  })
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row) => {
  isEdit.value = true
  Object.assign(form, {
    discountId: row.discountId,
    discountName: row.discountName,
    discountDesc: row.discountDesc,
    marketPlan: row.marketPlan,
    marketExpr: row.marketExpr
  })
  dialogVisible.value = true
}

// 删除
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除折扣"${row.discountName}"吗？`, '提示', {
      type: 'warning'
    })

    const res = await adminApi.deleteDiscount(row.discountId)
    if (res.code === '00000') {
      ElMessage.success('删除成功')
      fetchList()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除折扣失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

// 提交
const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const data = {
      discountName: form.discountName,
      discountDesc: form.discountDesc,
      marketPlan: form.marketPlan,
      marketExpr: form.marketExpr
    }

    let res
    if (isEdit.value) {
      res = await adminApi.updateDiscount(form.discountId, data)
    } else {
      res = await adminApi.createDiscount(data)
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

// 关闭对话框
const handleDialogClose = () => {
  formRef.value?.resetFields()
}

// 格式化日期
const formatDate = (date) => {
  return date ? dayjs(date).format('YYYY-MM-DD HH:mm:ss') : '-'
}

onMounted(() => {
  fetchList()
})
</script>

<style lang="scss" scoped>
.discounts-page {
  .search-card {
    margin-bottom: 20px;
  }

  .table-card {
    .pagination {
      margin-top: 20px;
      display: flex;
      justify-content: flex-end;
    }
  }
}
</style>
