<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listRefunds, type AdminRefundSummary } from '../services/refundApi'
import { REFUND_STATUS_LABEL, refundStatusType } from '../utils/statusLabels'
import { formatMoney } from '../utils/money'

const router = useRouter()
const refunds = ref<AdminRefundSummary[]>([])
const loading = ref(true)
const statusFilter = ref('')
const orderNoFilter = ref('')

async function load() {
  loading.value = true
  try {
    const { data } = await listRefunds({
      pageNo: 1,
      pageSize: 50,
      status: statusFilter.value || undefined,
      orderNo: orderNoFilter.value || undefined,
    })
    refunds.value = data.data.items
  } finally {
    loading.value = false
  }
}

function openDetail(refundNo: string) {
  router.push(`/refunds/${refundNo}`)
}

onMounted(load)
</script>

<template>
  <div class="admin-page">
    <header class="admin-page__header">
      <div class="admin-page__header-text">
        <h2>退款管理</h2>
        <p>审核用户退款申请并跟踪渠道退款结果</p>
      </div>
    </header>

    <div class="admin-page__filters">
      <el-select v-model="statusFilter" placeholder="退款状态" clearable style="width: 160px" @change="load">
        <el-option v-for="(label, key) in REFUND_STATUS_LABEL" :key="key" :label="label" :value="key" />
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

    <el-table v-loading="loading" :data="refunds" stripe style="width: 100%">
      <el-table-column prop="refundNo" label="退款单号" min-width="160" show-overflow-tooltip />
      <el-table-column prop="orderNo" label="订单号" min-width="160" show-overflow-tooltip />
      <el-table-column prop="userId" label="用户 ID" width="90" />
      <el-table-column label="退款金额" width="120" align="right">
        <template #default="{ row }">
          <span class="cell-price">{{ formatMoney(row.amount, row.currency) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="refundStatusType(row.status)" size="small">
            {{ REFUND_STATUS_LABEL[row.status] ?? row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="applyReason" label="申请原因" min-width="160" show-overflow-tooltip />
      <el-table-column label="申请时间" width="170">
        <template #default="{ row }">
          {{ new Date(row.createdAt).toLocaleString('zh-CN') }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="88" fixed="right">
        <template #default="{ row }">
          <el-button size="small" link type="primary" @click="openDetail(row.refundNo)">审核</el-button>
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
