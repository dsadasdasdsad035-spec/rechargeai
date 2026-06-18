<script setup lang="ts">
import { onMounted, ref } from 'vue'
import http from '../services/http'

const products = ref<any[]>([])

async function load() {
  const { data } = await http.get('/products')
  products.value = data.data
}

async function toggleShelf(row: any) {
  const status = row.status === 'ON_SHELF' ? 'OFF_SHELF' : 'ON_SHELF'
  await http.post(`/products/${row.id}/shelf`, { status })
  load()
}

onMounted(load)
</script>

<template>
  <h2>产品管理</h2>
  <el-table :data="products" style="width: 100%">
    <el-table-column prop="name" label="名称" />
    <el-table-column prop="salePrice" label="售价" />
    <el-table-column prop="status" label="状态" />
    <el-table-column label="操作">
      <template #default="{ row }">
        <el-button size="small" @click="toggleShelf(row)">
          {{ row.status === 'ON_SHELF' ? '下架' : '上架' }}
        </el-button>
      </template>
    </el-table-column>
  </el-table>
</template>
