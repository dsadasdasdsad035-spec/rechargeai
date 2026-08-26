<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted, computed } from 'vue'
import StatusBadge from './ui/StatusBadge.vue'
import BaseButton from './ui/BaseButton.vue'
import FulfillmentSupplementForm from './FulfillmentSupplementForm.vue'
import RefundApplyForm from './RefundApplyForm.vue'
import { confirmFulfillment } from '../services/fulfillmentApi'
import { getOrderDetail } from '../services/orderApi'
import {
  ORDER_STATUS_LABEL,
  PAYMENT_STATUS_LABEL,
  FULFILLMENT_STATUS_LABEL,
  REFUND_STATUS_LABEL,
  orderStatusTone,
  paymentStatusTone,
} from '../utils/statusLabels'
import { formatMoney } from '../utils/money'

interface FulfillmentLog {
  logType: string
  content: string
  createdAt: string
}

interface OrderDetail {
  orderNo: string
  productName: string
  amount: number
  currency: string
  paidAmount?: number | null
  paidCurrency?: string | null
  exchangeRate?: number | null
  targetAccountMasked: string
  accountTokenMasked?: string | null
  orderStatus: string
  paymentStatus: string
  fulfillmentStatus?: string
  fulfillmentTaskStatus?: string
  subscriptionStart?: string
  subscriptionEnd?: string
  fulfillmentLogs?: FulfillmentLog[]
  refundNo?: string
  refundStatus?: string
}

const props = defineProps<{
  order: OrderDetail | null
}>()

const emit = defineEmits<{ close: [] }>()

const visible = ref(false)
const detail = ref<OrderDetail | null>(null)
const confirmLoading = ref(false)
const showRefundForm = ref(false)

const ACTIVE_REFUND_STATUSES = ['PENDING', 'APPROVED', 'REFUNDING', 'CHANNEL_REFUND_FAILED']

const canApplyRefund = computed(() => {
  if (!detail.value) return false
  if (detail.value.orderStatus !== 'FAILED') return false
  if (detail.value.paymentStatus !== 'PAID') return false
  if (!detail.value.refundStatus) return true
  return detail.value.refundStatus === 'REJECTED'
})

const hasActiveRefund = computed(() =>
  detail.value?.refundStatus ? ACTIVE_REFUND_STATUSES.includes(detail.value.refundStatus) : false,
)

watch(
  () => props.order,
  async (val) => {
    visible.value = !!val
    showRefundForm.value = false
    if (val) {
      await reloadDetail(val.orderNo)
    } else {
      detail.value = null
    }
  },
)

async function onRefundSubmitted() {
  showRefundForm.value = false
  if (detail.value) {
    await reloadDetail(detail.value.orderNo)
  }
}

async function reloadDetail(orderNo: string) {
  const { data } = await getOrderDetail(orderNo)
  detail.value = data.data
}

async function onSupplementSubmitted() {
  if (detail.value) {
    await reloadDetail(detail.value.orderNo)
  }
}

async function onConfirm() {
  if (!detail.value) return
  confirmLoading.value = true
  try {
    await confirmFulfillment(detail.value.orderNo)
    await reloadDetail(detail.value.orderNo)
  } finally {
    confirmLoading.value = false
  }
}

function close() {
  visible.value = false
  emit('close')
}

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape' && visible.value) close()
}

function formatTime(iso?: string) {
  if (!iso) return '-'
  return new Date(iso).toLocaleString('zh-CN')
}

onMounted(() => document.addEventListener('keydown', onKeydown))
onUnmounted(() => document.removeEventListener('keydown', onKeydown))
</script>

<template>
  <Teleport to="body">
    <Transition name="drawer">
      <div v-if="visible && detail" class="drawer-root" role="dialog" aria-modal="true" aria-label="订单详情">
        <button class="drawer-backdrop" aria-label="关闭" @click="close" />
        <div class="drawer-panel">
          <div class="drawer-handle" aria-hidden="true" />
          <header class="drawer-header">
            <h3>订单详情</h3>
            <button class="drawer-close" aria-label="关闭" @click="close">×</button>
          </header>

          <div class="drawer-body">
            <div class="detail-row">
              <span class="detail-label">产品</span>
              <span class="detail-value">{{ detail.productName }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">金额</span>
              <span class="detail-value detail-value--price">{{ formatMoney(detail.amount, detail.currency) }}</span>
            </div>
            <div v-if="detail.paidAmount != null" class="detail-row">
              <span class="detail-label">实付金额</span>
              <span class="detail-value detail-value--price">{{ formatMoney(detail.paidAmount, detail.paidCurrency) }}</span>
            </div>
            <div v-if="detail.targetAccountMasked" class="detail-row">
              <span class="detail-label">AI 账号</span>
              <span class="detail-value">{{ detail.targetAccountMasked }}</span>
            </div>
            <div v-if="detail.accountTokenMasked" class="detail-row">
              <span class="detail-label">Session Token</span>
              <span class="detail-value detail-value--mono">{{ detail.accountTokenMasked }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">订单号</span>
              <span class="detail-value detail-value--mono">{{ detail.orderNo }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">状态</span>
              <span class="detail-badges">
                <StatusBadge
                  :label="ORDER_STATUS_LABEL[detail.orderStatus] ?? detail.orderStatus"
                  :tone="orderStatusTone(detail.orderStatus)"
                />
                <StatusBadge
                  :label="PAYMENT_STATUS_LABEL[detail.paymentStatus] ?? detail.paymentStatus"
                  :tone="paymentStatusTone(detail.paymentStatus)"
                />
                <StatusBadge
                  v-if="detail.fulfillmentStatus"
                  :label="FULFILLMENT_STATUS_LABEL[detail.fulfillmentStatus] ?? detail.fulfillmentStatus"
                  tone="primary"
                />
              </span>
            </div>

            <template v-if="detail.subscriptionStart || detail.subscriptionEnd">
              <div class="detail-row">
                <span class="detail-label">订阅开始</span>
                <span class="detail-value">{{ formatTime(detail.subscriptionStart) }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">订阅结束</span>
                <span class="detail-value">{{ formatTime(detail.subscriptionEnd) }}</span>
              </div>
            </template>

            <section v-if="detail.fulfillmentLogs?.length" class="fulfillment-logs">
              <h4 class="section-title">履约进度</h4>
              <ul class="log-list">
                <li v-for="(log, idx) in detail.fulfillmentLogs" :key="idx" class="log-item">
                  <time class="log-time">{{ formatTime(log.createdAt) }}</time>
                  <p class="log-content">{{ log.content }}</p>
                </li>
              </ul>
            </section>

            <section v-if="detail.fulfillmentTaskStatus === 'WAIT_USER'" class="fulfillment-actions">
              <h4 class="section-title">需要您的操作</h4>
              <p class="section-hint">请按客服指引完成操作后，提交补充资料或确认已完成。</p>
              <FulfillmentSupplementForm :order-no="detail.orderNo" @submitted="onSupplementSubmitted" />
              <BaseButton variant="secondary" block :disabled="confirmLoading" @click="onConfirm">
                {{ confirmLoading ? '提交中…' : '我已完成，确认提交' }}
              </BaseButton>
            </section>

            <section v-if="detail.refundStatus" class="refund-status">
              <h4 class="section-title">退款申请</h4>
              <div class="detail-row">
                <span class="detail-label">退款单号</span>
                <span class="detail-value detail-value--mono">{{ detail.refundNo }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">状态</span>
                <StatusBadge
                  :label="REFUND_STATUS_LABEL[detail.refundStatus] ?? detail.refundStatus"
                  :tone="detail.refundStatus === 'COMPLETED' ? 'success' : detail.refundStatus === 'REJECTED' ? 'danger' : 'warning'"
                />
              </div>
            </section>

            <section v-if="canApplyRefund" class="refund-actions">
              <h4 class="section-title">申请退款</h4>
              <p v-if="detail.refundStatus === 'REJECTED'" class="section-hint">
                上次申请已被驳回，您可重新提交退款申请。
              </p>
              <BaseButton
                v-if="!showRefundForm"
                variant="primary"
                block
                @click="showRefundForm = true"
              >
                申请全额退款
              </BaseButton>
              <RefundApplyForm
                v-else
                :order-no="detail.orderNo"
                :amount="detail.paidAmount ?? detail.amount"
                :currency="detail.paidCurrency ?? detail.currency"
                @submitted="onRefundSubmitted"
              />
            </section>

            <section v-else-if="detail.orderStatus === 'FAILED' && hasActiveRefund" class="refund-actions">
              <p class="section-hint">退款申请处理中，请耐心等待审核结果。</p>
            </section>
          </div>

          <footer class="drawer-footer">
            <BaseButton variant="secondary" block @click="close">关闭</BaseButton>
          </footer>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.drawer-root {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.drawer-backdrop {
  position: absolute;
  inset: 0;
  border: none;
  background: rgb(15 23 42 / 45%);
  cursor: pointer;
}

.drawer-panel {
  position: relative;
  width: 100%;
  max-width: 480px;
  max-height: 85dvh;
  background: var(--color-surface-raised);
  border-radius: var(--radius-lg) var(--radius-lg) 0 0;
  box-shadow: var(--shadow-lg);
  display: flex;
  flex-direction: column;
  padding-bottom: var(--safe-bottom);
}

@media (min-width: 640px) {
  .drawer-root {
    align-items: center;
    padding: var(--space-lg);
  }

  .drawer-panel {
    border-radius: var(--radius-lg);
    max-height: 80dvh;
  }

  .drawer-handle {
    display: none;
  }
}

.drawer-handle {
  width: 36px;
  height: 4px;
  margin: var(--space-sm) auto var(--space-xs);
  background: var(--color-border-strong);
  border-radius: var(--radius-full);
}

.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-md) var(--space-lg);
  border-bottom: 1px solid var(--color-border);
}

.drawer-close {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  font-size: 1.5rem;
  line-height: 1;
  color: var(--color-text-muted);
  border-radius: var(--radius-sm);
  transition: background var(--duration-fast) var(--ease-out);
}

.drawer-close:hover {
  background: var(--color-neutral-bg);
}

.drawer-body {
  padding: var(--space-lg);
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.detail-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: var(--space-md);
}

.detail-label {
  flex-shrink: 0;
  font-size: 0.875rem;
  color: var(--color-text-muted);
}

.detail-value {
  text-align: right;
  color: var(--color-text-heading);
  font-weight: 500;
}

.detail-value--price {
  font-family: var(--font-display);
  font-size: 1.125rem;
  color: var(--color-primary);
}

.detail-value--mono {
  font-size: 0.8125rem;
  word-break: break-all;
  color: var(--color-text);
  font-weight: 400;
}

.detail-badges {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-xs);
  justify-content: flex-end;
}

.section-title {
  margin: 0 0 var(--space-xs);
  font-size: 0.9375rem;
  color: var(--color-text-heading);
}

.section-hint {
  margin: 0 0 var(--space-sm);
  font-size: 0.8125rem;
  color: var(--color-text-muted);
}

.fulfillment-logs {
  padding-top: var(--space-sm);
  border-top: 1px solid var(--color-border);
}

.log-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

.log-item {
  padding: var(--space-sm);
  background: var(--color-neutral-bg);
  border-radius: var(--radius-md);
}

.log-time {
  display: block;
  font-size: 0.75rem;
  color: var(--color-text-muted);
  margin-bottom: var(--space-xs);
}

.log-content {
  margin: 0;
  font-size: 0.875rem;
  color: var(--color-text);
  line-height: 1.5;
}

.fulfillment-actions {
  padding-top: var(--space-sm);
  border-top: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

.refund-status,
.refund-actions {
  padding-top: var(--space-sm);
  border-top: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

.drawer-footer {
  padding: var(--space-md) var(--space-lg) var(--space-lg);
  border-top: 1px solid var(--color-border);
}

.drawer-enter-active,
.drawer-leave-active {
  transition: opacity var(--duration-normal) var(--ease-out);
}

.drawer-enter-active .drawer-panel,
.drawer-leave-active .drawer-panel {
  transition: transform var(--duration-normal) var(--ease-out);
}

.drawer-enter-from,
.drawer-leave-to {
  opacity: 0;
}

.drawer-enter-from .drawer-panel,
.drawer-leave-to .drawer-panel {
  transform: translateY(100%);
}

@media (min-width: 640px) {
  .drawer-enter-from .drawer-panel,
  .drawer-leave-to .drawer-panel {
    transform: translateY(16px) scale(0.98);
  }
}
</style>
