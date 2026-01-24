<template>
  <div class="goods-page">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input
        v-model="searchForm.keyword"
        placeholder="搜索商品名称"
        clearable
        style="width: 200px;"
        @keyup.enter="handleSearch"
      />
      <el-select v-model="searchForm.status" placeholder="商品状态" clearable style="width: 120px;">
        <el-option label="在售" value="ON_SALE" />
        <el-option label="下架" value="OFF_SALE" />
      </el-select>
      <el-button type="primary" @click="handleSearch">搜索</el-button>
      <el-button @click="handleReset">重置</el-button>
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        添加商品
      </el-button>
    </div>

    <!-- 商品列表 -->
    <div class="table-container">
      <el-table v-loading="loading" :data="goodsList" style="width: 100%">
        <el-table-column prop="spuId" label="SPU ID" width="120" />
        <el-table-column label="商品图片" width="100">
          <template #default="{ row }">
            <el-image
              :src="row.mainImage || defaultImage"
              style="width: 60px; height: 60px;"
              fit="cover"
            />
          </template>
        </el-table-column>
        <el-table-column prop="spuName" label="商品名称" min-width="150" />
        <el-table-column prop="minPrice" label="最低价" width="100">
          <template #default="{ row }">
            ¥{{ row.minPrice }}
          </template>
        </el-table-column>
        <el-table-column prop="skuCount" label="SKU数量" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ON_SALE' ? 'success' : 'info'" size="small">
              {{ row.status === 'ON_SALE' ? '在售' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button text type="primary" @click="handleManageSku(row)">SKU</el-button>
            <el-button
              v-if="row.status === 'OFF_SALE'"
              text
              type="success"
              @click="handleChangeStatus(row, 'ON_SALE')"
            >
              上架
            </el-button>
            <el-button
              v-if="row.status === 'ON_SALE'"
              text
              type="warning"
              @click="handleChangeStatus(row, 'OFF_SALE')"
            >
              下架
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

    <!-- SPU 编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑商品' : '添加商品'"
      width="600px"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="商品名称" prop="spuName">
          <el-input v-model="form.spuName" placeholder="请输入商品名称" />
        </el-form-item>
        <el-form-item label="商品描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入商品描述" />
        </el-form-item>
        <el-form-item label="主图" prop="mainImage">
          <el-upload
            class="main-image-uploader"
            :action="uploadUrl"
            :headers="uploadHeaders"
            :show-file-list="false"
            :on-success="handleMainImageSuccess"
            :before-upload="beforeMainImageUpload"
          >
            <img v-if="form.mainImage" :src="form.mainImage" class="main-image" />
            <el-icon v-else class="main-image-uploader-icon"><Plus /></el-icon>
          </el-upload>
          <div class="upload-tip">建议尺寸：800x800px，支持jpg/png/gif，最大5MB</div>
        </el-form-item>
        <el-form-item label="分类ID" prop="categoryId">
          <el-input v-model="form.categoryId" placeholder="请输入分类ID" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>



    <!-- SKU 编辑/新增弹窗 -->
    <el-dialog
      v-model="skuFormVisible"
      :title="isEditSku ? '编辑 SKU' : '添加 SKU'"
      width="500px"
    >
      <el-form ref="skuFormRef" :model="skuForm" :rules="skuRules" label-width="100px">
        <el-form-item label="规格名称" prop="goodsName">
          <el-input v-model="skuForm.goodsName" placeholder="例如：红色 L码" />
        </el-form-item>
        <el-form-item label="价格" prop="originalPrice">
          <el-input-number v-model="skuForm.originalPrice" :min="0.01" :precision="2" :step="1" />
        </el-form-item>
        <el-form-item label="初始库存" prop="stock" v-if="!isEditSku">
          <el-input-number v-model="skuForm.stock" :min="0" :step="1" />
        </el-form-item>
        <el-form-item label="SKU图片" prop="skuImage">
          <el-upload
            class="sku-image-uploader"
            :action="uploadUrl"
            :headers="uploadHeaders"
            :show-file-list="false"
            :on-success="handleSkuImageSuccess"
            :before-upload="beforeSkuImageUpload"
          >
            <img v-if="skuForm.skuImage" :src="skuForm.skuImage" class="sku-image" />
            <el-icon v-else class="sku-image-uploader-icon"><Plus /></el-icon>
          </el-upload>
          <div class="upload-tip">建议尺寸：400x400px</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="skuFormVisible = false">取消</el-button>
        <el-button type="primary" :loading="skuSubmitting" @click="handleSubmitSku">确定</el-button>
      </template>
    </el-dialog>

    <!-- 增加库存弹窗 -->
    <el-dialog v-model="stockDialogVisible" title="增加库存" width="400px">
      <el-form ref="stockFormRef" :model="stockForm" :rules="stockRules" label-width="100px">
        <el-form-item label="增加数量" prop="quantity">
          <el-input-number v-model="stockForm.quantity" :min="1" :step="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stockDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="stockSubmitting" @click="handleSubmitStock">确定</el-button>
      </template>
    </el-dialog>

    <!-- SKU 管理弹窗 -->
    <el-dialog
      v-model="skuDialogVisible"
      title="SKU 管理"
      width="900px"
    >
      <div class="sku-toolbar">
        <el-button type="primary" size="small" @click="handleAddSku">添加 SKU</el-button>
      </div>
      <el-table :data="skuList" style="width: 100%">
        <el-table-column prop="skuId" label="SKU ID" width="120" />
        <el-table-column prop="goodsName" label="规格名称">
          <template #default="{ row }">
             {{ row.goodsName || row.description }}
          </template>
        </el-table-column>
        <el-table-column prop="price" label="价格" width="100">
          <template #default="{ row }">
            ¥{{ row.price }}
          </template>
        </el-table-column>
        <el-table-column prop="stock" label="总库存" width="100" />
        <el-table-column prop="frozenStock" label="冻结库存" width="100" />
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button text type="primary" @click="handleEditSku(row)">编辑</el-button>
            <el-button text type="success" @click="handleAddStock(row)">补货</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { adminApi } from '@/api/admin'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const loading = ref(false)
const goodsList = ref([])
// SPU Dialogs
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref(null)

// SKU Dialogs
const skuDialogVisible = ref(false)
const skuFormVisible = ref(false)
const isEditSku = ref(false)
const skuSubmitting = ref(false)
const skuFormRef = ref(null)
const skuList = ref([])
const currentSpuId = ref('')

// Stock Dialog
const stockDialogVisible = ref(false)
const stockSubmitting = ref(false)
const stockFormRef = ref(null)

const defaultImage = 'https://via.placeholder.com/60x60?text=No+Image'

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
  spuId: '',
  spuName: '',
  description: '',
  mainImage: '',
  categoryId: ''
})

const skuForm = reactive({
  skuId: '',
  spuId: '',
  goodsName: '',
  specInfo: '',
  originalPrice: 0,
  stock: 0,
  skuImage: ''
})

const stockForm = reactive({
  skuId: '',
  quantity: 0
})

const rules = {
  spuName: [{ required: true, message: '请输入商品名称', trigger: 'blur' }]
}

const skuRules = {
  goodsName: [{ required: true, message: '请输入SKU名称', trigger: 'blur' }],
  originalPrice: [{ required: true, message: '请输入价格', trigger: 'blur' }],
  stock: [{ required: true, message: '请输入初始库存', trigger: 'blur' }]
}

const stockRules = {
  quantity: [{ required: true, message: '请输入增加数量', trigger: 'blur' }]
}

// 图片上传配置
const uploadUrl = '/api/admin/goods/upload/image'  // 使用相对路径，通过 Vite 代理
const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${userStore.token}`
}))

// 主图上传处理
const beforeMainImageUpload = (file) => {
  const isImage = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp'].includes(file.type)
  const isLt5M = file.size / 1024 / 1024 < 5

  if (!isImage) {
    ElMessage.error('只能上传 JPG/PNG/GIF/WEBP 格式的图片!')
    return false
  }
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB!')
    return false
  }
  return true
}

const handleMainImageSuccess = (response) => {
  if (response.code === '00000') {
    form.mainImage = response.data
    ElMessage.success('主图上传成功')
  } else {
    ElMessage.error(response.msg || '上传失败')
  }
}

// SKU图片上传处理
const beforeSkuImageUpload = (file) => {
  const isImage = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp'].includes(file.type)
  const isLt5M = file.size / 1024 / 1024 < 5

  if (!isImage) {
    ElMessage.error('只能上传 JPG/PNG/GIF/WEBP 格式的图片!')
    return false
  }
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB!')
    return false
  }
  return true
}

const handleSkuImageSuccess = (response) => {
  if (response.code === '00000') {
    skuForm.skuImage = response.data
    ElMessage.success('SKU图片上传成功')
  } else {
    ElMessage.error(response.msg || '上传失败')
  }
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await adminApi.getSpuList({
      page: pagination.page,
      size: pagination.size,
      keyword: searchForm.keyword,
      status: searchForm.status
    })
    if (res.code === '00000' && res.data) {
      goodsList.value = res.data.list || []
      pagination.total = res.data.total || 0
    }
  } catch (error) {
    console.error('获取商品列表失败:', error)
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

// SPU Operations
const handleAdd = () => {
  isEdit.value = false
  Object.assign(form, {
    spuId: '',
    spuName: '',
    description: '',
    mainImage: '',
    categoryId: ''
  })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  Object.assign(form, {
    spuId: row.spuId,
    spuName: row.spuName,
    description: row.description,
    mainImage: row.mainImage,
    categoryId: row.categoryId
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    let res
    if (isEdit.value) {
      res = await adminApi.updateSpu(form.spuId, form)
    } else {
      res = await adminApi.createSpu(form)
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

const handleChangeStatus = async (row, status) => {
  const actionText = status === 'ON_SALE' ? '上架' : '下架'
  try {
    await ElMessageBox.confirm(`确定要${actionText}该商品吗？`, '提示', { type: 'warning' })
    let res
    if (status === 'ON_SALE') {
      res = await adminApi.onSaleSpu(row.spuId)
    } else {
      res = await adminApi.offSaleSpu(row.spuId)
    }
    
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

// SKU Operations
const fetchSkuList = async (spuId) => {
  try {
    const res = await adminApi.getSpu(spuId)
    if (res.code === '00000') {
      skuList.value = res.data.skuList || []
    }
  } catch (error) {
    console.error('获取 SKU 列表失败:', error)
    ElMessage.error('获取 SKU 列表失败')
  }
}

const handleManageSku = (row) => {
  currentSpuId.value = row.spuId
  skuList.value = [] // clear first
  fetchSkuList(row.spuId)
  skuDialogVisible.value = true
}

const handleAddSku = () => {
  isEditSku.value = false
  Object.assign(skuForm, {
    skuId: '',
    spuId: currentSpuId.value,
    goodsName: '',
    specInfo: '',
    originalPrice: 0,
    stock: 0,
    skuImage: ''
  })
  skuFormVisible.value = true
}

const handleEditSku = (row) => {
  isEditSku.value = true
  Object.assign(skuForm, {
    skuId: row.skuId,
    spuId: currentSpuId.value,
    goodsName: row.goodsName || row.description, // Fallback if name missing
    specInfo: row.specInfo,
    originalPrice: row.price,
    stock: row.stock, // Note: Stock distinct from add-stock usually, but backend doesn't support stock update in simple update. Display only?
    skuImage: row.skuImage
  })
  skuFormVisible.value = true
}

const handleSubmitSku = async () => {
  const valid = await skuFormRef.value.validate().catch(() => false)
  if (!valid) return

  skuSubmitting.value = true
  try {
    let res
    if (isEditSku.value) {
      // Update logic (without stock)
      res = await adminApi.updateSku(skuForm.skuId, {
        goodsName: skuForm.goodsName,
        specInfo: skuForm.specInfo,
        originalPrice: skuForm.originalPrice,
        skuImage: skuForm.skuImage
      })
    } else {
      // Create logic (requires stock)
      res = await adminApi.createSku({
        spuId: currentSpuId.value,
        goodsName: skuForm.goodsName,
        specInfo: skuForm.specInfo,
        originalPrice: skuForm.originalPrice,
        stock: skuForm.stock,
        skuImage: skuForm.skuImage
      })
    }

    if (res.code === '00000') {
      ElMessage.success(isEditSku.value ? 'SKU更新成功' : 'SKU创建成功')
      skuFormVisible.value = false
      fetchSkuList(currentSpuId.value) // Refresh list
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } finally {
    skuSubmitting.value = false
  }
}

const handleAddStock = (row) => {
  stockForm.skuId = row.skuId
  stockForm.quantity = 0
  stockDialogVisible.value = true
}

const handleSubmitStock = async () => {
  const valid = await stockFormRef.value.validate().catch(() => false)
  if (!valid) return
  
  stockSubmitting.value = true
  try {
    const res = await adminApi.addSkuStock(stockForm.skuId, stockForm.quantity)
    if (res.code === '00000') {
      ElMessage.success('库存增加成功')
      stockDialogVisible.value = false
      fetchSkuList(currentSpuId.value)
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } finally {
    stockSubmitting.value = false
  }
}

onMounted(() => {
  fetchList()
})
</script>

<style lang="scss" scoped>
.goods-page {
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

  .sku-toolbar {
    margin-bottom: 16px;
  }

  // 图片上传样式
  .main-image-uploader,
  .sku-image-uploader {
    :deep(.el-upload) {
      border: 1px dashed #d9d9d9;
      border-radius: 6px;
      cursor: pointer;
      position: relative;
      overflow: hidden;
      transition: border-color 0.3s;

      &:hover {
        border-color: #409eff;
      }
    }
  }

  .main-image-uploader {
    :deep(.el-upload) {
      width: 178px;
      height: 178px;
    }

    .main-image {
      width: 178px;
      height: 178px;
      display: block;
      object-fit: cover;
    }

    .main-image-uploader-icon {
      font-size: 28px;
      color: #8c939d;
      width: 178px;
      height: 178px;
      line-height: 178px;
      text-align: center;
    }
  }

  .sku-image-uploader {
    :deep(.el-upload) {
      width: 100px;
      height: 100px;
    }

    .sku-image {
      width: 100px;
      height: 100px;
      display: block;
      object-fit: cover;
    }

    .sku-image-uploader-icon {
      font-size: 24px;
      color: #8c939d;
      width: 100px;
      height: 100px;
      line-height: 100px;
      text-align: center;
    }
  }

  .upload-tip {
    font-size: 12px;
    color: #999;
    margin-top: 8px;
  }
}
</style>
