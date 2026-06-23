<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter, RouterLink } from 'vue-router'
import BaseCard from '../components/ui/BaseCard.vue'
import http from '../services/http'

const route = useRoute()
const router = useRouter()
const payInfo = ref<Record<string, string>>({})
const loading = ref(true)
const error = ref('')
const polling = ref(false)
let pollTimer: ReturnType<typeof setInterval> | null = null

const isImageQr = () => /^https?:\/\//.test(payInfo.value.codeUrl || '')

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
  polling.value = false
}

async function checkPaid() {
  const { data } = await http.get(`/orders/${route.params.orderNo}`)
  const detail = data.data
  if (detail.paymentStatus === 'PAID' || detail.orderStatus === 'PAID') {
    stopPolling()
    router.push('/transaction-record')
  }
}

function startPolling() {
  polling.value = true
  pollTimer = setInterval(() => {
    checkPaid().catch(() => {
      // 轮询失败时静默重试
    })
  }, 3000)
}

onMounted(async () => {
  try {
    const { data } = await http.post(`/orders/${route.params.orderNo}/pay`, { channel: 'XUNHUPAY' })
    payInfo.value = data.data
    await checkPaid()
    if (payInfo.value.paymentUrl || payInfo.value.codeUrl) {
      startPolling()
    }
  } catch (e: any) {
    error.value = e.response?.data?.message ?? '发起支付失败，请稍后重试'
  } finally {
    loading.value = false
  }
})

onUnmounted(stopPolling)
</script>

<template>
  <div class="page">
    <header class="page-header">
      <h2>支付订单</h2>
      <p class="order-no">{{ route.params.orderNo }}</p>
    </header>

    <BaseCard v-if="error" class="pay-card pay-card--error">
      <p>{{ error }}</p>
      <RouterLink to="/transaction-record" class="pay-link">返回交易记录</RouterLink>
    </BaseCard>

    <BaseCard v-else-if="!loading" class="pay-card">
      <div v-if="payInfo.codeUrl && isImageQr()" class="pay-qr">
        <p class="pay-qr__label">请使用微信或支付宝扫码支付</p>
        <img class="pay-qr__image" :src="payInfo.codeUrl" alt="支付二维码" />
      </div>

      <div v-else-if="payInfo.codeUrl" class="pay-qr">
        <p class="pay-qr__label">支付链接</p>
        <p class="pay-qr__url">{{ payInfo.codeUrl }}</p>
      </div>

      <div class="pay-actions">
        <a
          v-if="payInfo.paymentUrl"
          class="pay-open-link"
          :href="payInfo.paymentUrl"
          target="_blank"
          rel="noreferrer"
        >
          打开支付页面
        </a>
        <p v-if="polling" class="pay-status">等待支付结果…</p>
        <RouterLink to="/transaction-record" class="pay-link">
          稍后查看交易记录
        </RouterLink>
      </div>
    </BaseCard>

    <div v-else class="skeleton-pay" aria-busy="true" />
  </div>
</template>

<style scoped>
.order-no {
  font-size: 0.8125rem;
  color: var(--color-text-subtle);
  word-break: break-all;
}

.pay-card {
  max-width: 480px;
}

.pay-card--error p {
  color: var(--color-danger);
  margin-bottom: var(--space-md);
}

.pay-qr {
  margin-bottom: var(--space-lg);
  padding: var(--space-lg);
  background: var(--color-primary-subtle);
  border-radius: var(--radius-sm);
  text-align: center;
}

.pay-qr__label {
  font-weight: 500;
  color: var(--color-text-heading);
  margin-bottom: var(--space-sm);
}

.pay-qr__url {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
  word-break: break-all;
}

.pay-qr__image {
  display: block;
  width: min(220px, 100%);
  aspect-ratio: 1;
  margin: 0 auto;
  border-radius: var(--radius-sm);
  background: var(--color-surface);
}

.pay-actions {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
  align-items: center;
}

.pay-open-link {
  display: inline-flex;
  min-height: 44px;
  width: 100%;
  align-items: center;
  justify-content: center;
  padding: 0 var(--space-lg);
  border-radius: var(--radius-sm);
  background: var(--color-primary);
  color: oklch(0.98 0.01 165);
  font-weight: 500;
  text-decoration: none;
}

.pay-status {
  font-size: 0.875rem;
  color: var(--color-text-muted);
}

.pay-link {
  font-size: 0.9375rem;
  color: var(--color-text-muted);
}

.skeleton-pay {
  height: 200px;
  max-width: 480px;
  border-radius: var(--radius-md);
  background: var(--color-neutral-bg);
}
</style>
