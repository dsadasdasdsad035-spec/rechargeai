<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, RouterLink } from 'vue-router'
import http from '../services/http'

const route = useRoute()
const product = ref<any>(null)

onMounted(async () => {
  const { data } = await http.get(`/products/${route.params.id}`)
  product.value = data.data
})
</script>

<template>
  <div v-if="product">
    <h2>{{ product.name }}</h2>
    <p>售价：¥{{ product.salePrice }}</p>
    <p>预计处理：{{ product.estimatedHours }} 小时</p>
    <p>{{ product.refundPolicyText }}</p>
    <p class="notice">{{ product.complianceNotice }}</p>
    <RouterLink :to="`/checkout/${product.id}`" class="btn">立即订购</RouterLink>
  </div>
</template>

<style scoped>
.notice { background: #fff7e6; padding: 12px; border-radius: 6px; color: #ad6800; }
.btn { display: inline-block; margin-top: 16px; background: #1677ff; color: #fff; padding: 10px 16px; border-radius: 6px; text-decoration: none; }
</style>
