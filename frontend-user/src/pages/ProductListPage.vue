<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import http from '../services/http'

const products = ref<any[]>([])

onMounted(async () => {
  const { data } = await http.get('/products')
  products.value = data.data
})
</script>

<template>
  <h2>AI 服务</h2>
  <div class="grid">
    <RouterLink v-for="p in products" :key="p.id" :to="`/products/${p.id}`" class="item">
      <h3>{{ p.name }}</h3>
      <p>¥{{ p.salePrice }} / {{ p.periodDays }}天</p>
    </RouterLink>
  </div>
</template>

<style scoped>
.grid { display: grid; gap: 12px; }
.item { border: 1px solid #eee; border-radius: 8px; padding: 16px; text-decoration: none; color: inherit; }
</style>
