<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  approveRefund,
  getRefundDetail,
  rejectRefund,
  retryChannelRefund,
  type AdminRefundDetail,
} from '../services/refundApi'
import { REFUND_STATUS_LABEL, refundStatusType, canApproveRefund } from '../utils/statusLabels'
import { formatMoney } from '../utils/money'

const route = useRoute()
const router = useRouter()
const refundNo = computed(() => route.params.refundNo as string)
const detail = ref<AdminRefundDetail | null>(null)
const loading = ref(true)
const actionLoading = ref(false)
const reviewComment = ref('')

const writable = computed(() => canApproveRefund())
const isPending = computed(() => detail.value?.status === 'PENDING')
const isChannelFailed = computed(() => detail.value?.status === 'CHANNEL_REFUND_FAILED')

async function load() {
  loading.value = true
  try {
    const { data } = await getRefundDetail(refundNo.value)
    detail.value = data.data
  } finally {
    loading.value = false
  }
}

async function onApprove() {
  actionLoading.value = true
  try {
    const { data } = await approveRefund(refundNo.value, reviewComment.value.trim() || undefined)
    detail.value = data.data
    reviewComment.value = ''
    ElMessage.success('已通过并发起渠道退款')
  } finally {
    actionLoading.value = false
  }
}

async function onReject() {
  if (!reviewComment.value.trim()) {
    ElMessage.warning('驳回须填写审核意见')
    return
  }
  await ElMessageBox.confirm('确认驳回该退款申请？', '确认操作', { type: 'warning' })
  actionLoading.value = true
  try {
    const { data } = await rejectRefund(refundNo.value, reviewComment.value.trim())
    detail.value = data.data
    reviewComment.value = ''
    ElMessage.success('已驳回')
  } finally {
    actionLoading.value = false
  }
}

async function onRetry() {
  actionLoading.value = true
  try {
    const { data } = await retryChannelRefund(refundNo.value)
    detail.value = data.data
    ElMessage.success(detail.value.status === 'COMPLETED' ? '渠道退款成功' : '重试完成，请查看状态')
  } finally {
    actionLoading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div v-loading="loading" class="admin-page">
    <header class="admin-page__header">
      <div class="admin-page__header-text">
        <el-button link type="primary" @click="router.push('/refunds')">← 返回列表</el-button>
        <h2>退款审核 {{ refundNo }}</h2>
        <p v-if="detail">订单 {{ detail.orderNo }} · {{ detail.productName }}</p>
      </div>
    </header>

    <template v-if="detail">
      <el-descriptions :column="2" border class="detail-block">
        <el-descriptions-item label="退款状态">
          <el-tag :type="refundStatusType(detail.status)" size="small">
            {{ REFUND_STATUS_LABEL[detail.status] ?? detail.status }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="退款金额">{{ formatMoney(detail.amount, detail.currency) }}</el-descriptions-item>
        <el-descriptions-item label="支付渠道">{{ detail.paymentChannel }}</el-descriptions-item>
        <el-descriptions-item label="用户 ID">{{ detail.userId }}</el-descriptions-item>
        <el-descriptions-item label="申请原因" :span="2">{{ detail.applyReason }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.reviewerName" label="审核人">{{ detail.reviewerName }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.reviewComment" label="审核意见">{{ detail.reviewComment }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.channelRefundNo" label="渠道退款号">{{ detail.channelRefundNo }}</el-descriptions-item>
      </el-descriptions>

      <section v-if="writable && (isPending || isChannelFailed)" class="action-panel">
        <h3>{{ isPending ? '审核操作' : '渠道重试' }}</h3>
        <p v-if="!writable" class="readonly-hint">当前角色不可审批退款。</p>
        <template v-else-if="isPending">
          <el-input
            v-model="reviewComment"
            type="textarea"
            :rows="3"
            placeholder="审核意见（驳回时必填）"
          />
          <div class="action-row">
            <el-button type="success" :loading="actionLoading" @click="onApprove">通过并退款</el-button>
            <el-button type="danger" :loading="actionLoading" @click="onReject">驳回</el-button>
          </div>
        </template>
        <template v-else-if="isChannelFailed">
          <p class="section-hint">渠道退款失败，可人工重试（最多自动重试 3 次）。</p>
          <el-button type="primary" :loading="actionLoading" @click="onRetry">重试渠道退款</el-button>
        </template>
      </section>

      <p v-else-if="!writable" class="readonly-hint">当前角色仅可查看退款单。</p>
    </template>
  </div>
</template>

<style scoped>
.detail-block {
  margin-bottom: 24px;
}

.action-panel {
  background: var(--wild-surface);
  border: 1px solid var(--wild-border);
  border-radius: 8px;
  padding: 20px;
}

.action-panel h3 {
  margin: 0 0 16px;
  font-size: 16px;
}

.action-row {
  display: flex;
  gap: 12px;
  margin-top: 12px;
}

.section-hint,
.readonly-hint {
  color: var(--el-text-color-secondary);
  font-size: 14px;
  margin: 0 0 12px;
}
</style>
