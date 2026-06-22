<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter, RouterLink } from 'vue-router'
import axios from 'axios'
import BaseButton from '../components/ui/BaseButton.vue'
import BaseCard from '../components/ui/BaseCard.vue'
import http from '../services/http'

const route = useRoute()
const router = useRouter()
const payInfo = ref<Record<string, string>>({})
const paying = ref(false)
const loading = ref(true)

onMounted(async () => {
  const { data } = await http.post(`/orders/${route.params.orderNo}/pay`, { channel: 'XUNHUPAY' })
  payInfo.value = data.data
  loading.value = false
})

const isImageQr = () => /^https?:\/\//.test(payInfo.value.codeUrl || '')

async function mockPay() {
  paying.value = true
  try {
    const url = payInfo.value.mockNotifyUrl
    if (url) {
      await axios.post(url)
    } else {
      await axios.post('/api/payments/mock/notify', null, {
        params: { paymentNo: payInfo.value.paymentNo, tradeNo: payInfo.value.mockTradeNo },
      })
    }
    router.push('/transaction-record')
  } finally {
    paying.value = false
  }
}
</script>

<template>
  <div class="page">
    <header class="page-header">
      <h2>支付订单</h2>
      <p class="order-no">{{ route.params.orderNo }}</p>
    </header>

    <BaseCard v-if="!loading" class="pay-card">
      <div v-if="payInfo.codeUrl" class="pay-qr">
        <p class="pay-qr__label">请扫码完成支付</p>
        <img v-if="isImageQr()" class="pay-qr__image" :src="payInfo.codeUrl" alt="支付二维码" />
        <p v-else class="pay-qr__url">{{ payInfo.codeUrl }}</p>
      </div>

      <div class="pay-actions">
        <a v-if="payInfo.paymentUrl" class="pay-open-link" :href="payInfo.paymentUrl" target="_blank" rel="noreferrer">
          打开支付页面
        </a>
        <BaseButton v-if="payInfo.mockNotifyUrl || payInfo.mockTradeNo" variant="success" block :disabled="paying" @click="mockPay">
          {{ paying ? '处理中…' : '模拟支付成功（开发环境）' }}
        </BaseButton>
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
