<script setup lang="ts">
import { onMounted, ref } from 'vue'
import BaseCard from '../components/ui/BaseCard.vue'
import http from '../services/http'
import { formatMoney } from '../utils/money'

interface Product {
  id: number
  name: string
  salePrice: number
  currency: string
  periodDays: number
  estimatedHours?: number
}

const products = ref<Product[]>([])
const loading = ref(true)
const error = ref('')

onMounted(async () => {
  try {
    const { data } = await http.get('/products')
    products.value = data.data
  } catch {
    error.value = '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="page">
    <header class="page-header">
      <h2>AI 订阅服务</h2>
      <p>选择您需要的 AI 服务，我们将协助完成订阅流程</p>
    </header>

    <div v-if="loading" class="skeleton-grid" aria-busy="true" aria-label="加载中">
      <div v-for="i in 3" :key="i" class="skeleton-card" />
    </div>

    <p v-else-if="error" class="message message--error">{{ error }}</p>

    <div v-else-if="products.length === 0" class="empty">
      <p class="empty__title">暂无可用服务</p>
      <p class="empty__desc">请稍后再来查看</p>
    </div>

    <div v-else class="product-grid">
      <BaseCard
        v-for="p in products"
        :key="p.id"
        tag="router-link"
        :to="`/products/${p.id}`"
        interactive
        class="product-card"
      >
        <div class="product-card__top">
          <h3 class="product-card__name">{{ p.name }}</h3>
          <span v-if="p.estimatedHours" class="product-card__eta">约 {{ p.estimatedHours }}h</span>
        </div>
        <div class="product-card__price">
          <span class="product-card__amount">{{ formatMoney(p.salePrice, p.currency) }}</span>
          <span class="product-card__period">/ {{ p.periodDays }} 天</span>
        </div>
        <span class="product-card__cta">查看详情 →</span>
      </BaseCard>
    </div>
  </div>
</template>

<style scoped>
.product-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: var(--space-md);
}

@media (min-width: 480px) {
  .product-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (min-width: 768px) {
  .product-grid {
    grid-template-columns: repeat(3, 1fr);
    gap: var(--space-lg);
  }
}

.product-card {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
  min-height: 140px;
}

.product-card__top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: var(--space-sm);
}

.product-card__name {
  flex: 1;
}

.product-card__eta {
  flex-shrink: 0;
  font-size: 0.75rem;
  color: var(--color-text-muted);
  background: var(--color-neutral-bg);
  padding: 2px 8px;
  border-radius: var(--radius-full);
}

.product-card__price {
  display: flex;
  align-items: baseline;
  gap: var(--space-xs);
}

.product-card__amount {
  font-family: var(--font-display);
  font-size: 1.5rem;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: var(--color-primary);
  letter-spacing: -0.02em;
}

.product-card__period {
  font-size: 0.875rem;
  color: var(--color-text-muted);
}

.product-card__cta {
  margin-top: auto;
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--color-text-muted);
  transition: color var(--duration-fast) var(--ease-out);
}

.product-card:hover .product-card__cta {
  color: var(--color-primary);
}

.skeleton-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: var(--space-md);
}

@media (min-width: 480px) {
  .skeleton-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

.skeleton-card {
  height: 140px;
  border-radius: var(--radius-md);
  background: linear-gradient(
    90deg,
    var(--color-neutral-bg) 25%,
    var(--color-surface) 50%,
    var(--color-neutral-bg) 75%
  );
  background-size: 200% 100%;
  animation: shimmer 1.4s ease-in-out infinite;
}

@keyframes shimmer {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

.empty {
  text-align: center;
  padding: var(--space-2xl) var(--space-md);
}

.empty__title {
  font-family: var(--font-display);
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--color-text-heading);
  margin-bottom: var(--space-sm);
}

.empty__desc {
  color: var(--color-text-muted);
  font-size: 0.9375rem;
}

.message {
  padding: var(--space-md);
  border-radius: var(--radius-sm);
  font-size: 0.9375rem;
}

.message--error {
  background: var(--color-danger-bg);
  color: var(--color-danger);
}
</style>
