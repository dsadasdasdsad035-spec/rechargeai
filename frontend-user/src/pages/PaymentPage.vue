<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import http from '../services/http'

const route = useRoute()
const router = useRouter()
const payInfo = ref<Record<string, string>>({})

onMounted(async () => {
  const { data } = await http.post(`/orders/${route.params.orderNo}/pay`, { channel: 'XUNHUPAY' })
  payInfo.value = data.data
})

async function mockPay() {
  const url = payInfo.value.mockNotifyUrl
  if (url) {
    await axios.post(url)
  } else {
    await axios.post('/api/payments/mock/notify', null, {
      params: { paymentNo: payInfo.value.paymentNo, tradeNo: payInfo.value.mockTradeNo },
    })
  }
  router.push('/transaction-record')
}
</script>

<template>
  <div class="card">
    <h2>支付订单 {{ route.params.orderNo }}</h2>
    <p v-if="payInfo.codeUrl">支付链接：{{ payInfo.codeUrl }}</p>
    <button class="primary" @click="mockPay">模拟支付成功（开发环境）</button>
    <router-link to="/transaction-record">稍后查看交易记录</router-link>
  </div>
</template>

<style scoped>
.card { display: flex; flex-direction: column; gap: 12px; }
.primary { background: #52c41a; color: #fff; border: none; padding: 10px; border-radius: 6px; }
</style>
