<script setup lang="ts">
import { onMounted, ref } from 'vue'
import http from '../services/http'

const orders = ref<any[]>([])
const detail = ref<any>(null)
const dialogVisible = ref(false)

async function load() {
  const { data } = await http.get('/orders', { params: { pageNo: 1, pageSize: 50 } })
  orders.value = data.data.items
}

async function showDetail(orderNo: string) {
  const { data } = await http.get(`/orders/${orderNo}`)
  detail.value = data.data
  dialogVisible.value = true
}

async function exportCsv() {
  window.open('/admin/api/orders/export', '_blank')
}

onMounted(load)
</script>

<template>
  <h2>订单管理（只读）</h2>
  <el-button type="primary" @click="exportCsv" style="margin-bottom: 12px">导出 CSV</el-button>
  <el-table :data="orders" style="width: 100%">
    <el-table-column prop="orderNo" label="订单号" />
    <el-table-column prop="productName" label="产品" />
    <el-table-column prop="amount" label="金额" />
    <el-table-column prop="orderStatus" label="订单状态" />
    <el-table-column prop="paymentStatus" label="支付状态" />
    <el-table-column label="操作">
      <template #default="{ row }">
        <el-button size="small" @click="showDetail(row.orderNo)">详情</el-button>
      </template>
    </el-table-column>
  </el-table>
  <el-dialog v-model="dialogVisible" title="订单详情" width="480px">
    <template v-if="detail">
      <p>订单号：{{ detail.orderNo }}</p>
      <p>AI 账号：{{ detail.targetAccountMasked }}</p>
      <p>状态：{{ detail.orderStatus }} / {{ detail.paymentStatus }}</p>
    </template>
  </el-dialog>
</template>
