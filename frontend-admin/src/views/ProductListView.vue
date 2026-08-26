<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import http from '../services/http'
import { PRODUCT_STATUS_LABEL, productStatusType } from '../utils/statusLabels'
import { formatMoney } from '../utils/money'

const SERVICE_TYPE_LABEL: Record<string, string> = {
  CHATGPT: 'ChatGPT',
  X_PREMIUM_PLUS: 'X Premium+',
  GEMINI_PRO_ULTRA: 'Gemini Pro / Ultra',
  CLAUDE: 'Claude',
  CLAUDE_API: 'Claude API',
  GENERAL: '通用',
}
const products = ref<any[]>([])
const loading = ref(true)
const router = useRouter()

async function load() {
  loading.value = true
  try {
    const { data } = await http.get('/products')
    products.value = data.data
  } finally {
    loading.value = false
  }
}

async function toggleShelf(row: any) {
  const status = row.status === 'ON_SHELF' ? 'OFF_SHELF' : 'ON_SHELF'
  await http.post(`/products/${row.id}/shelf`, { status })
  ElMessage.success(status === 'ON_SHELF' ? '已上架' : '已下架')
  load()
}

onMounted(load)
</script>

<template>
  <div class="admin-page">
    <header class="admin-page__header">
      <div class="admin-page__header-text">
        <h2>产品管理</h2>
        <p>管理 AI 订阅产品的上架与定价</p>
      </div>
      <div class="admin-page__actions">
        <el-button type="primary" :icon="Plus" @click="router.push('/products/new')">
          新建产品
        </el-button>
      </div>
    </header>

    <el-table v-loading="loading" :data="products" stripe style="width: 100%">
      <el-table-column prop="productCode" label="编码" min-width="120" show-overflow-tooltip />
      <el-table-column prop="name" label="名称" min-width="160" />
      <el-table-column label="服务类型" width="140">
        <template #default="{ row }">
          {{ SERVICE_TYPE_LABEL[row.serviceType] ?? row.serviceType }}
        </template>
      </el-table-column>
      <el-table-column label="售价" width="120" align="right">
        <template #default="{ row }">
          <span class="cell-price">{{ formatMoney(row.salePrice, row.currency) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="periodDays" label="周期(天)" width="88" align="center" />
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="productStatusType(row.status)" size="small">
            {{ PRODUCT_STATUS_LABEL[row.status] ?? row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button size="small" link type="primary" @click="router.push(`/products/${row.id}/edit`)">
            编辑
          </el-button>
          <el-button
            size="small"
            link
            :type="row.status === 'ON_SHELF' ? 'warning' : 'primary'"
            @click="toggleShelf(row)"
          >
            {{ row.status === 'ON_SHELF' ? '下架' : '上架' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>
