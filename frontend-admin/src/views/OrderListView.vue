<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Download } from '@element-plus/icons-vue'
import http from '../services/http'
import {
  ORDER_STATUS_LABEL,
  PAYMENT_STATUS_LABEL,
  orderStatusType,
  paymentStatusType,
} from '../utils/statusLabels'

const orders = ref<any[]>([])
const detail = ref<any>(null)
const dialogVisible = ref(false)
const loading = ref(true)

async function load() {
  loading.value = true
  try {
    const { data } = await http.get('/orders', { params: { pageNo: 1, pageSize: 50 } })
    orders.value = data.data.items
  } finally {
    loading.value = false
  }
}

async function showDetail(orderNo: string) {
  const { data } = await http.get(`/orders/${orderNo}`)
  detail.value = data.data
  dialogVisible.value = true
}

function exportCsv() {
  window.open('/admin/api/orders/export', '_blank')
}

onMounted(load)
</script>

<template>
  <div class="admin-page">
    <header class="admin-page__header">
      <div class="admin-page__header-text">
        <h2>订单管理</h2>
        <p>只读查看订单列表，支持导出 CSV</p>
      </div>
      <div class="admin-page__actions">
        <el-button type="primary" :icon="Download" @click="exportCsv">导出 CSV</el-button>
      </div>
    </header>

    <el-table v-loading="loading" :data="orders" stripe style="width: 100%">
      <el-table-column prop="orderNo" label="订单号" min-width="160" show-overflow-tooltip />
      <el-table-column prop="productName" label="产品" min-width="120" />
      <el-table-column label="金额" width="100" align="right">
        <template #default="{ row }">
          <span class="cell-price">¥{{ row.amount }}</span>
        </template>
      </el-table-column>
      <el-table-column label="订单状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="orderStatusType(row.orderStatus)" size="small">
            {{ ORDER_STATUS_LABEL[row.orderStatus] ?? row.orderStatus }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="支付状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="paymentStatusType(row.paymentStatus)" size="small">
            {{ PAYMENT_STATUS_LABEL[row.paymentStatus] ?? row.paymentStatus }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="88" fixed="right">
        <template #default="{ row }">
          <el-button size="small" link type="primary" @click="showDetail(row.orderNo)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" title="订单详情" width="480px" destroy-on-close>
      <div v-if="detail" class="detail-list">
        <div class="detail-list__row">
          <span class="detail-list__label">订单号</span>
          <span class="detail-list__value">{{ detail.orderNo }}</span>
        </div>
        <div class="detail-list__row">
          <span class="detail-list__label">产品</span>
          <span class="detail-list__value">{{ detail.productName }}</span>
        </div>
        <div class="detail-list__row">
          <span class="detail-list__label">金额</span>
          <span class="detail-list__value cell-price">¥{{ detail.amount }}</span>
        </div>
        <div class="detail-list__row">
          <span class="detail-list__label">AI 账号</span>
          <span class="detail-list__value">{{ detail.targetAccountPlain || detail.targetAccountMasked }}</span>
        </div>
        <div v-if="detail.accountTokenPlain || detail.accountTokenMasked" class="detail-list__row">
          <span class="detail-list__label">Session Token</span>
          <span class="detail-list__value detail-list__value--mono">{{ detail.accountTokenPlain || detail.accountTokenMasked }}</span>
        </div>
        <div class="detail-list__row">
          <span class="detail-list__label">订单状态</span>
          <span class="detail-list__value">
            <el-tag :type="orderStatusType(detail.orderStatus)" size="small">
              {{ ORDER_STATUS_LABEL[detail.orderStatus] ?? detail.orderStatus }}
            </el-tag>
          </span>
        </div>
        <div class="detail-list__row">
          <span class="detail-list__label">支付状态</span>
          <span class="detail-list__value">
            <el-tag :type="paymentStatusType(detail.paymentStatus)" size="small">
              {{ PAYMENT_STATUS_LABEL[detail.paymentStatus] ?? detail.paymentStatus }}
            </el-tag>
          </span>
        </div>
      </div>
    </el-dialog>
  </div>
</template>
