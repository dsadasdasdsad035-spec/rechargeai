<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted } from 'vue'
import StatusBadge from './ui/StatusBadge.vue'
import BaseButton from './ui/BaseButton.vue'
import {
  ORDER_STATUS_LABEL,
  PAYMENT_STATUS_LABEL,
  orderStatusTone,
  paymentStatusTone,
} from '../utils/statusLabels'

const props = defineProps<{
  order: {
    orderNo: string
    productName: string
    amount: number
    targetAccountMasked: string
    orderStatus: string
    paymentStatus: string
  } | null
}>()

const emit = defineEmits<{ close: [] }>()

const visible = ref(false)

watch(
  () => props.order,
  (val) => {
    visible.value = !!val
  },
)

function close() {
  visible.value = false
  emit('close')
}

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape' && visible.value) close()
}

onMounted(() => document.addEventListener('keydown', onKeydown))
onUnmounted(() => document.removeEventListener('keydown', onKeydown))
</script>

<template>
  <Teleport to="body">
    <Transition name="drawer">
      <div v-if="visible && order" class="drawer-root" role="dialog" aria-modal="true" aria-label="订单详情">
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
              <span class="detail-value">{{ order.productName }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">金额</span>
              <span class="detail-value detail-value--price">¥{{ order.amount }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">AI 账号</span>
              <span class="detail-value">{{ order.targetAccountMasked }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">订单号</span>
              <span class="detail-value detail-value--mono">{{ order.orderNo }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">状态</span>
              <span class="detail-badges">
                <StatusBadge
                  :label="ORDER_STATUS_LABEL[order.orderStatus] ?? order.orderStatus"
                  :tone="orderStatusTone(order.orderStatus)"
                />
                <StatusBadge
                  :label="PAYMENT_STATUS_LABEL[order.paymentStatus] ?? order.paymentStatus"
                  :tone="paymentStatusTone(order.paymentStatus)"
                />
              </span>
            </div>
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
  background: oklch(0.15 0.02 85 / 0.45);
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
  font-family: var(--font-serif);
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

.drawer-footer {
  padding: var(--space-md) var(--space-lg) var(--space-lg);
  border-top: 1px solid var(--color-border);
}

/* 入场动效 */
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
