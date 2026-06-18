<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import http from '../services/http'

const route = useRoute()
const router = useRouter()
const product = ref<any>(null)
const targetAccount = ref('')

onMounted(async () => {
  const { data } = await http.get(`/products/${route.params.id}`)
  product.value = data.data
})

async function submit() {
  const { data } = await http.post('/orders', {
    productId: Number(route.params.id),
    fields: { target_account: targetAccount.value },
  })
  router.push(`/payment/${data.data.orderNo}`)
}
</script>

<template>
  <div v-if="product" class="card">
    <h2>确认订购 — {{ product.name }}</h2>
    <p>金额：¥{{ product.salePrice }}</p>
    <label>AI 账号邮箱</label>
    <input v-model="targetAccount" type="email" placeholder="your@email.com" />
    <p class="hint">请勿填写第三方服务密码</p>
    <button class="primary" @click="submit">提交订单</button>
  </div>
</template>

<style scoped>
.card { display: flex; flex-direction: column; gap: 12px; max-width: 480px; }
input { padding: 10px; border: 1px solid #ddd; border-radius: 6px; }
.primary { background: #1677ff; color: #fff; border: none; padding: 10px; border-radius: 6px; }
.hint { color: #888; font-size: 12px; }
</style>
