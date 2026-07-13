<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { CopyDocument, Download, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import http from '../services/http'
import {
  ORDER_STATUS_LABEL,
  PAYMENT_STATUS_LABEL,
  orderStatusType,
  paymentStatusType,
} from '../utils/statusLabels'
import { maskSecret } from '../utils/secretDisplay'
import { formatMoney } from '../utils/money'

const orders = ref<any[]>([])
const detail = ref<any>(null)
const dialogVisible = ref(false)
const loading = ref(true)
const exporting = ref(false)
const copyingToken = ref(false)
const total = ref(0)
const pageNo = ref(1)
const pageSize = ref(20)
const orderNoFilter = ref('')
const userIdFilter = ref('')
const orderStatusFilter = ref('')
const paymentStatusFilter = ref('')
const createdRange = ref<[string, string] | null>(null)

const sessionTokenDisplay = computed(() => {
  return detail.value?.accountTokenMasked || maskSecret(detail.value?.accountTokenPlain)
})

function paidMoney(row: any) {
  return row?.paidAmount == null ? '-' : formatMoney(row.paidAmount, row.paidCurrency)
}

async function load() {
  loading.value = true
  try {
    const { data } = await http.get('/orders', { params: buildQueryParams(true) })
    orders.value = data.data.items
    total.value = data.data.total
    pageNo.value = data.data.pageNo
    pageSize.value = data.data.pageSize
  } finally {
    loading.value = false
  }
}

function buildQueryParams(includePagination: boolean) {
  const params: Record<string, unknown> = {
    orderNo: orderNoFilter.value.trim() || undefined,
    userId: userIdFilter.value.trim() || undefined,
    orderStatus: orderStatusFilter.value || undefined,
    paymentStatus: paymentStatusFilter.value || undefined,
  }
  if (createdRange.value?.length === 2) {
    params.startTime = createdRange.value[0]
    params.endTime = createdRange.value[1]
  }
  if (includePagination) {
    params.pageNo = pageNo.value
    params.pageSize = pageSize.value
  }
  return params
}

function searchOrders() {
  pageNo.value = 1
  load()
}

function resetFilters() {
  orderNoFilter.value = ''
  userIdFilter.value = ''
  orderStatusFilter.value = ''
  paymentStatusFilter.value = ''
  createdRange.value = null
  pageNo.value = 1
  load()
}

function onPageChange(page: number) {
  pageNo.value = page
  load()
}

function onPageSizeChange(size: number) {
  pageSize.value = size
  pageNo.value = 1
  load()
}

async function showDetail(orderNo: string) {
  const { data } = await http.get(`/orders/${orderNo}`)
  detail.value = data.data
  dialogVisible.value = true
}

async function exportCsv() {
  exporting.value = true
  try {
    const response = await http.get('/orders/export', {
      params: buildQueryParams(false),
      responseType: 'blob',
    })
    const blob = response.data instanceof Blob
      ? response.data
      : new Blob([response.data], { type: 'text/csv;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = exportFilename(response.headers?.['content-disposition'])
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
  } catch {
    ElMessage.error('导出失败，请稍后重试')
  } finally {
    exporting.value = false
  }
}

function exportFilename(disposition?: string) {
  const match = disposition?.match(/filename="?([^";]+)"?/)
  return match?.[1] || 'orders.csv'
}

async function copySessionToken() {
  const token = detail.value?.accountTokenPlain?.trim()
  if (!token) {
    ElMessage.warning('暂无完整 Session Token 可复制')
    return
  }
  copyingToken.value = true
  try {
    await copyText(token)
    ElMessage.success('Session Token 已复制')
  } catch {
    ElMessage.error('复制失败，请检查浏览器剪贴板权限')
  } finally {
    copyingToken.value = false
  }
}

async function copyText(text: string) {
  if (navigator.clipboard?.writeText) {
    await navigator.clipboard.writeText(text)
    return
  }
  const textarea = document.createElement('textarea')
  textarea.value = text
  textarea.setAttribute('readonly', 'true')
  textarea.style.position = 'fixed'
  textarea.style.left = '-9999px'
  document.body.appendChild(textarea)
  textarea.select()
  const copied = document.execCommand('copy')
  document.body.removeChild(textarea)
  if (!copied) {
    throw new Error('copy failed')
  }
}

onMounted(load)
</script>

<template>
  <div class="admin-page">
    <header class="admin-page__header">
      <div class="admin-page__header-text">
        <h2>订单管理</h2>
        <p>只读查看订单列表，支持条件筛选、分页查询与导出 CSV</p>
      </div>
      <div class="admin-page__actions">
        <el-button type="primary" :icon="Download" :loading="exporting" @click="exportCsv">导出 CSV</el-button>
      </div>
    </header>

    <div class="admin-page__filters">
      <el-input
        v-model="orderNoFilter"
        placeholder="订单号"
        clearable
        style="width: 220px"
        @keyup.enter="searchOrders"
        @clear="searchOrders"
      />
      <el-input
        v-model="userIdFilter"
        placeholder="用户 ID"
        clearable
        style="width: 140px"
        @keyup.enter="searchOrders"
        @clear="searchOrders"
      />
      <el-select v-model="orderStatusFilter" placeholder="订单状态" clearable style="width: 150px">
        <el-option v-for="(label, key) in ORDER_STATUS_LABEL" :key="key" :label="label" :value="key" />
      </el-select>
      <el-select v-model="paymentStatusFilter" placeholder="支付状态" clearable style="width: 150px">
        <el-option v-for="(label, key) in PAYMENT_STATUS_LABEL" :key="key" :label="label" :value="key" />
      </el-select>
      <el-date-picker
        v-model="createdRange"
        type="datetimerange"
        unlink-panels
        range-separator="至"
        start-placeholder="下单开始"
        end-placeholder="下单结束"
        value-format="YYYY-MM-DDTHH:mm:ssZ"
        style="width: 380px"
      />
      <el-button type="primary" :icon="Search" @click="searchOrders">查询</el-button>
      <el-button :icon="Refresh" @click="resetFilters">重置</el-button>
    </div>

    <el-table v-loading="loading" :data="orders" stripe style="width: 100%">
      <el-table-column prop="orderNo" label="订单号" min-width="160" show-overflow-tooltip />
      <el-table-column prop="productName" label="产品" min-width="120" />
      <el-table-column label="订单金额" width="120" align="right">
        <template #default="{ row }">
          <span class="cell-price">{{ formatMoney(row.amount, row.currency) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="实付金额" width="120" align="right">
        <template #default="{ row }">
          <span class="cell-price">{{ paidMoney(row) }}</span>
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
      <el-table-column label="下单时间" width="170">
        <template #default="{ row }">
          {{ new Date(row.createdAt).toLocaleString('zh-CN') }}
        </template>
      </el-table-column>
      <el-table-column label="支付时间" width="170">
        <template #default="{ row }">
          {{ row.paidAt ? new Date(row.paidAt).toLocaleString('zh-CN') : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="88" fixed="right">
        <template #default="{ row }">
          <el-button size="small" link type="primary" @click="showDetail(row.orderNo)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        background
        layout="total, sizes, prev, pager, next"
        :total="total"
        :page-size="pageSize"
        :current-page="pageNo"
        :page-sizes="[10, 20, 50, 100]"
        @current-change="onPageChange"
        @size-change="onPageSizeChange"
      />
    </div>

    <el-dialog v-model="dialogVisible" title="订单详情" width="560px" destroy-on-close>
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
          <span class="detail-list__label">订单金额</span>
          <span class="detail-list__value cell-price">{{ formatMoney(detail.amount, detail.currency) }}</span>
        </div>
        <div class="detail-list__row">
          <span class="detail-list__label">实付金额</span>
          <span class="detail-list__value cell-price">{{ paidMoney(detail) }}</span>
        </div>
        <div v-if="detail.exchangeRate" class="detail-list__row">
          <span class="detail-list__label">支付汇率</span>
          <span class="detail-list__value">1 {{ detail.currency }} = {{ detail.exchangeRate }} {{ detail.paidCurrency }}</span>
        </div>
        <div v-if="detail.targetAccountPlain || detail.targetAccountMasked" class="detail-list__row">
          <span class="detail-list__label">AI 账号</span>
          <span class="detail-list__value">{{ detail.targetAccountPlain || detail.targetAccountMasked }}</span>
        </div>
        <div v-if="detail.accountTokenPlain || detail.accountTokenMasked" class="detail-list__row">
          <span class="detail-list__label">Session Token</span>
          <span class="detail-list__value token-copy">
            <span class="token-copy__text">{{ sessionTokenDisplay }}</span>
            <el-button
              size="small"
              type="primary"
              link
              :icon="CopyDocument"
              :loading="copyingToken"
              @click="copySessionToken"
            >
              复制
            </el-button>
          </span>
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

<style scoped>
.admin-page__filters {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}

.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.token-copy {
  min-width: 0;
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.token-copy__text {
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', monospace;
}
</style>
