<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, RouterLink } from 'vue-router'
import BaseButton from '../components/ui/BaseButton.vue'
import BaseCard from '../components/ui/BaseCard.vue'
import http from '../services/http'

const route = useRoute()
const product = ref<any>(null)
const loading = ref(true)

onMounted(async () => {
  const { data } = await http.get(`/products/${route.params.id}`)
  product.value = data.data
  loading.value = false
})
</script>

<template>
  <div class="page">
    <div v-if="loading" class="skeleton-detail" aria-busy="true" />

    <template v-else-if="product">
      <header class="page-header">
        <h2>{{ product.name }}</h2>
        <p class="price">¥{{ product.salePrice }}<span class="price__unit">/ {{ product.periodDays }} 天</span></p>
      </header>

      <BaseCard class="info-card">
        <dl class="info-list">
          <div class="info-row">
            <dt>预计处理</dt>
            <dd>约 {{ product.estimatedHours }} 小时</dd>
          </div>
          <div v-if="product.refundPolicyText" class="info-row">
            <dt>退款政策</dt>
            <dd>{{ product.refundPolicyText }}</dd>
          </div>
        </dl>
      </BaseCard>

      <div v-if="product.complianceNotice" class="notice" role="note">
        {{ product.complianceNotice }}
      </div>

      <div class="actions">
        <RouterLink :to="`/checkout/${product.id}`">
          <BaseButton size="lg" block>立即订购</BaseButton>
        </RouterLink>
      </div>
    </template>
  </div>
</template>

<style scoped>
.price {
  margin-top: var(--space-sm);
  font-family: var(--font-serif);
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--color-primary);
}

.price__unit {
  font-family: var(--font-sans);
  font-size: 0.9375rem;
  font-weight: 400;
  color: var(--color-text-muted);
}

.info-card {
  margin-bottom: var(--space-lg);
}

.info-list {
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.info-row {
  display: flex;
  justify-content: space-between;
  gap: var(--space-md);
}

.info-row dt {
  font-size: 0.875rem;
  color: var(--color-text-muted);
  font-weight: 500;
}

.info-row dd {
  margin: 0;
  text-align: right;
  color: var(--color-text-heading);
  font-weight: 500;
}

.notice {
  padding: var(--space-md);
  background: var(--color-warning-bg);
  border-radius: var(--radius-sm);
  color: var(--color-warning);
  font-size: 0.9375rem;
  line-height: 1.5;
  margin-bottom: var(--space-lg);
}

.actions {
  margin-top: var(--space-xl);
}

.actions a {
  display: block;
  text-decoration: none;
}

.skeleton-detail {
  height: 280px;
  border-radius: var(--radius-md);
  background: var(--color-neutral-bg);
}
</style>
