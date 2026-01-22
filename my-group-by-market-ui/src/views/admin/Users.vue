<template>
  <div class="users-page">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input
        v-model="searchForm.keyword"
        placeholder="用户名/昵称/手机号"
        clearable
        style="width: 200px;"
        @keyup.enter="handleSearch"
      />
      <el-select v-model="searchForm.status" placeholder="用户状态" clearable style="width: 120px;">
        <el-option label="正常" value="ACTIVE" />
        <el-option label="禁用" value="DISABLED" />
        <el-option label="锁定" value="LOCKED" />
      </el-select>
      <el-select v-model="searchForm.role" placeholder="用户角色" clearable style="width: 120px;">
        <el-option label="普通用户" value="USER" />
        <el-option label="管理员" value="ADMIN" />
      </el-select>
      <el-button type="primary" @click="handleSearch">搜索</el-button>
      <el-button @click="handleReset">重置</el-button>
      <el-button type="primary" @click="handleCreateAdmin">
        <el-icon><Plus /></el-icon>
        创建管理员
      </el-button>
    </div>

    <!-- 用户列表 -->
    <div class="table-container">
      <el-table v-loading="loading" :data="userList" style="width: 100%">
        <el-table-column prop="userId" label="用户ID" width="120" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="nickname" label="昵称" width="120" />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column prop="role" label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'danger' : ''" size="small">
              {{ row.role === 'ADMIN' ? '管理员' : '用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="160">
          <template #default="{ row }">
            {{ formatDate(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'ACTIVE'"
              text
              type="warning"
              @click="handleChangeStatus(row, 'DISABLED')"
            >
              禁用
            </el-button>
            <el-button
              v-if="row.status === 'DISABLED' || row.status === 'LOCKED'"
              text
              type="success"
              @click="handleChangeStatus(row, 'ACTIVE')"
            >
              启用
            </el-button>
            <el-button text type="primary" @click="handleResetPassword(row)">重置密码</el-button>
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

    <!-- 创建管理员弹窗 -->
    <el-dialog v-model="dialogVisible" title="创建管理员" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
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
const userList = ref([])
const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref(null)

const searchForm = reactive({
  keyword: '',
  status: '',
  role: ''
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const form = reactive({
  username: '',
  nickname: '',
  password: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在3到20个字符', trigger: 'blur' }
  ],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ]
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await adminApi.getUsers({
      page: pagination.page,
      size: pagination.size,
      keyword: searchForm.keyword,
      status: searchForm.status,
      role: searchForm.role
    })
    if (res.code === '00000') {
      userList.value = res.data.list || []
      pagination.total = res.data.total || 0
    }
  } catch (error) {
    console.error('获取用户列表失败:', error)
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
  searchForm.role = ''
  handleSearch()
}

const handleCreateAdmin = () => {
  Object.assign(form, {
    username: '',
    nickname: '',
    password: ''
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const res = await adminApi.createAdmin(form)
    if (res.code === '00000') {
      ElMessage.success('创建成功')
      dialogVisible.value = false
      fetchList()
    } else {
      ElMessage.error(res.message || '创建失败')
    }
  } finally {
    submitting.value = false
  }
}

const handleChangeStatus = async (row, status) => {
  const actionText = status === 'ACTIVE' ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(`确定要${actionText}该用户吗？`, '提示', { type: 'warning' })
    const res = await adminApi.updateUserStatus(row.userId, status)
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

const handleResetPassword = async (row) => {
  try {
    await ElMessageBox.confirm('确定要重置该用户的密码吗？', '提示', { type: 'warning' })
    const res = await adminApi.resetUserPassword(row.userId)
    if (res.code === '00000') {
      ElMessage.success('密码已重置')
    } else {
      ElMessage.error(res.message || '重置失败')
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
    case 'ACTIVE': return 'success'
    case 'DISABLED': return 'danger'
    case 'LOCKED': return 'warning'
    default: return 'info'
  }
}

const getStatusText = (status) => {
  switch (status) {
    case 'ACTIVE': return '正常'
    case 'DISABLED': return '禁用'
    case 'LOCKED': return '锁定'
    default: return status
  }
}

onMounted(() => {
  fetchList()
})
</script>

<style lang="scss" scoped>
.users-page {
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

  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
