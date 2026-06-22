<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { listOrders, getOrderDetail } from '../services/orderApi'
import OrderDetailDrawer from '../components/OrderDetailDrawer.vue'
import StatusBadge from '../components/ui/StatusBadge.vue'
import BaseButton from '../components/ui/BaseButton.vue'
import {
  ORDER_STATUS_LABEL,
  PAYMENT_STATUS_LABEL,
  orderStatusTone,
  paymentStatusTone,
} from '../utils/statusLabels'

interface OrderItem {
  orderNo: string
  productName: string
  amount: number
  orderStatus: string
  paymentStatus: string
  targetAccountMasked?: string
}

const orders = ref<OrderItem[]>([])
const selected = ref<{
  orderNo: string
  productName: string
  amount: number
  targetAccountMasked: string
  orderStatus: string
  paymentStatus: string
} | null>(null)
const orderStatus = ref('')
const paymentStatus = ref('')
const loading = ref(true)

async function load() {
  loading.value = true
  try {
    const { data } = await listOrders({
      pageNo: 1,
      pageSize: 50,
      orderStatus: orderStatus.value || undefined,
      paymentStatus: paymentStatus.value || undefined,
    })
    orders.value = data.data.items
  } finally {
    loading.value = false
  }
}

async function showDetail(orderNo: string) {
  const { data } = await getOrderDetail(orderNo)
  selected.value = data.data
}

onMounted(load)
</script>

<template>
  <div class="page">
    <header class="page-header">
      <h2>交易记录</h2>
      <p>查看您的订购与支付状态</p>
    </header>

    <div class="filters">
      <label class="filter">
        <span class="filter__label">订单状态</span>
        <select v-model="orderStatus" class="filter__select" @change="load">
          <option value="">全部</option>
          <option value="WAIT_PAY">待支付</option>
          <option value="PAID">已支付</option>
          <option value="CLOSED">已关闭</option>
        </select>
      </label>
      <label class="filter">
        <span class="filter__label">支付状态</span>
        <select v-model="paymentStatus" class="filter__select" @change="load">
          <option value="">全部</option>
          <option value="UNPAID">未支付</option>
          <option value="PAID">已支付</option>
        </select>
      </label>
    </div>

    <div v-if="loading" class="skeleton-list" aria-busy="true">
      <div v-for="i in 4" :key="i" class="skeleton-item" />
    </div>

    <div v-else-if="orders.length === 0" class="empty">
      <div class="empty__icon" aria-hidden="true">📋</div>
      <p class="empty__title">暂无订单</p>
      <p class="empty__desc">完成首次订购后，记录将显示在这里</p>
      <RouterLink to="/products">
        <BaseButton>去订购服务</BaseButton>
      </RouterLink>
    </div>

    <ul v-else class="order-list" role="list">
      <li v-for="o in orders" :key="o.orderNo">
        <button type="button" class="order-item" @click="showDetail(o.orderNo)">
          <div class="order-item__main">
            <span class="order-item__name">{{ o.productName }}</span>
            <span class="order-item__amount">¥{{ o.amount }}</span>
          </div>
          <div class="order-item__meta">
            <span class="order-item__no">{{ o.orderNo }}</span>
            <span class="order-item__badges">
              <StatusBadge
                :label="ORDER_STATUS_LABEL[o.orderStatus] ?? o.orderStatus"
                :tone="orderStatusTone(o.orderStatus)"
              />
              <StatusBadge
                :label="PAYMENT_STATUS_LABEL[o.paymentStatus] ?? o.paymentStatus"
                :tone="paymentStatusTone(o.paymentStatus)"
              />
            </span>
          </div>
        </button>
      </li>
    </ul>

    <OrderDetailDrawer :order="selected" @close="selected = null" />
  </div>
</template>

<style scoped>
.filters {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-md);
  margin-bottom: var(--space-lg);
}

.filter {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
  flex: 1;
  min-width: 140px;
}

.filter__label {
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--color-text-muted);
}

.filter__select {
  min-height: 44px;
  padding: 0 var(--space-md);
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-sm);
  background: var(--color-surface-raised);
  color: var(--color-text-heading);
  cursor: pointer;
}

.filter__select:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px var(--color-primary-muted);
}

.order-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

.order-item {
  width: 100%;
  text-align: left;
  padding: var(--space-md) var(--space-lg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface-raised);
  cursor: pointer;
  transition:
    border-color var(--duration-fast) var(--ease-out),
    box-shadow var(--duration-fast) var(--ease-out);
}

.order-item:hover {
  border-color: var(--color-border-strong);
  box-shadow: var(--shadow-sm);
}

.order-item:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}

.order-item__main {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--space-md);
  margin-bottom: var(--space-sm);
}

.order-item__name {
  font-weight: 600;
  color: var(--color-text-heading);
}

.order-item__amount {
  font-family: var(--font-serif);
  font-weight: 600;
  color: var(--color-primary);
  flex-shrink: 0;
}

.order-item__meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--space-sm);
  flex-wrap: wrap;
}

.order-item__no {
  font-size: 0.75rem;
  color: var(--color-text-subtle);
}

.order-item__badges {
  display: flex;
  gap: var(--space-xs);
}

.empty {
  text-align: center;
  padding: var(--space-2xl) var(--space-md);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-md);
}

.empty__icon {
  font-size: 2.5rem;
  opacity: 0.6;
}

.empty__title {
  font-family: var(--font-serif);
  font-size: 1.125rem;
  color: var(--color-text-heading);
}

.empty__desc {
  color: var(--color-text-muted);
  font-size: 0.9375rem;
  margin-bottom: var(--space-sm);
}

.skeleton-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

.skeleton-item {
  height: 72px;
  border-radius: var(--radius-md);
  background: var(--color-neutral-bg);
  animation: pulse 1.4s ease-in-out infinite;
}

@keyframes pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}
</style>
