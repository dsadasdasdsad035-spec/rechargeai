<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listFulfillmentTasks, type FulfillmentTaskSummary } from '../services/fulfillmentApi'
import {
  FULFILLMENT_STATUS_LABEL,
  fulfillmentStatusType,
} from '../utils/statusLabels'

const router = useRouter()
const tasks = ref<FulfillmentTaskSummary[]>([])
const loading = ref(true)
const statusFilter = ref('')
const orderNoFilter = ref('')

async function load() {
  loading.value = true
  try {
    const { data } = await listFulfillmentTasks({
      pageNo: 1,
      pageSize: 50,
      status: statusFilter.value || undefined,
      orderNo: orderNoFilter.value || undefined,
    })
    tasks.value = data.data.items
  } finally {
    loading.value = false
  }
}

function openDetail(taskNo: string) {
  router.push(`/fulfillment/${taskNo}`)
}

onMounted(load)
</script>

<template>
  <div class="admin-page">
    <header class="admin-page__header">
      <div class="admin-page__header-text">
        <h2>履约任务</h2>
        <p>分派并处理支付成功后的订阅履约任务</p>
      </div>
    </header>

    <div class="admin-page__filters">
      <el-select v-model="statusFilter" placeholder="任务状态" clearable style="width: 160px" @change="load">
        <el-option
          v-for="(label, key) in FULFILLMENT_STATUS_LABEL"
          :key="key"
          :label="label"
          :value="key"
        />
      </el-select>
      <el-input
        v-model="orderNoFilter"
        placeholder="订单号"
        clearable
        style="width: 200px"
        @keyup.enter="load"
        @clear="load"
      />
      <el-button type="primary" @click="load">查询</el-button>
    </div>

    <el-table v-loading="loading" :data="tasks" stripe style="width: 100%">
      <el-table-column prop="taskNo" label="任务号" min-width="160" show-overflow-tooltip />
      <el-table-column prop="orderNo" label="订单号" min-width="160" show-overflow-tooltip />
      <el-table-column prop="productName" label="产品" min-width="120" />
      <el-table-column label="状态" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="fulfillmentStatusType(row.status)" size="small">
            {{ FULFILLMENT_STATUS_LABEL[row.status] ?? row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="assigneeName" label="处理人" width="100">
        <template #default="{ row }">
          {{ row.assigneeName ?? '未分派' }}
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="170">
        <template #default="{ row }">
          {{ new Date(row.createdAt).toLocaleString('zh-CN') }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="88" fixed="right">
        <template #default="{ row }">
          <el-button size="small" link type="primary" @click="openDetail(row.taskNo)">处理</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<style scoped>
.admin-page__filters {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}
</style>
