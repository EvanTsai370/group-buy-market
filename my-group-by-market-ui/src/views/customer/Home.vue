<template>
  <div class="home-page">
    <div class="page-header">
      <h2>热门拼团</h2>
      <p>参与拼团，享超值优惠</p>
    </div>

    <!-- 商品列表 -->
    <div v-loading="loading" class="goods-list">
      <template v-if="goodsList.length > 0">
        <div
          v-for="item in goodsList"
          :key="item.spuId"
          class="goods-card"
          @click="goDetail(item.spuId)"
        >
          <div class="goods-image">
            <img :src="item.mainImage || defaultImage" :alt="item.spuName" />
            <el-tag v-if="item.activity" class="activity-tag" type="danger" effect="dark">
              {{ item.activity.target }}人团
            </el-tag>
          </div>
          <div class="goods-info">
            <h3 class="goods-name">{{ item.spuName }}</h3>
            <p class="goods-desc">{{ item.description }}</p>
            <div class="goods-price">
              <span class="current-price">
                ¥{{ item.activity?.discountPrice || item.minPrice }}
              </span>
              <span v-if="item.activity" class="original-price">¥{{ item.minPrice }}</span>
            </div>
            <div v-if="item.activity" class="activity-info">
              <el-icon><Clock /></el-icon>
              <span>剩余 {{ formatTime(item.activity.endTime) }}</span>
            </div>
          </div>
        </div>
      </template>
      <el-empty v-else description="暂无拼团商品" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { goodsApi } from '@/api/goods'
import dayjs from 'dayjs'

const router = useRouter()

const loading = ref(false)
const goodsList = ref([])
const defaultImage = 'https://via.placeholder.com/300x300?text=No+Image'

const fetchGoodsList = async () => {
  loading.value = true
  try {
    const res = await goodsApi.getSpuList()
    if (res.code === '00000' && res.data) {
      goodsList.value = res.data || []
    }
  } catch (error) {
    console.error('获取商品列表失败:', error)
  } finally {
    loading.value = false
  }
}

const goDetail = (spuId) => {
  router.push(`/customer/product/${spuId}`)
}

const formatTime = (time) => {
  if (!time) return ''
  const end = dayjs(time)
  const now = dayjs()
  const diff = end.diff(now, 'hour')
  if (diff < 24) {
    return `${diff}小时`
  }
  return `${Math.floor(diff / 24)}天`
}

onMounted(() => {
  fetchGoodsList()
})
</script>

<style lang="scss" scoped>
.home-page {
  .page-header {
    text-align: center;
    margin-bottom: 30px;

    h2 {
      font-size: 28px;
      color: #333;
      margin-bottom: 8px;
    }

    p {
      color: #999;
    }
  }
}

.goods-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.goods-card {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  }

  .goods-image {
    position: relative;
    height: 200px;
    overflow: hidden;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }

    .activity-tag {
      position: absolute;
      top: 10px;
      left: 10px;
    }
  }

  .goods-info {
    padding: 16px;

    .goods-name {
      font-size: 16px;
      color: #333;
      margin-bottom: 8px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .goods-desc {
      font-size: 12px;
      color: #999;
      margin-bottom: 12px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .goods-price {
      margin-bottom: 8px;

      .current-price {
        font-size: 20px;
        font-weight: bold;
        color: #f56c6c;
      }

      .original-price {
        font-size: 14px;
        color: #999;
        text-decoration: line-through;
        margin-left: 8px;
      }
    }

    .activity-info {
      display: flex;
      align-items: center;
      gap: 4px;
      font-size: 12px;
      color: #f56c6c;
    }
  }
}
</style>
