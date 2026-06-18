<script setup lang="ts">
import { onMounted, ref } from 'vue'
import http from '../services/http'

const users = ref<any[]>([])

async function load() {
  const { data } = await http.get('/users')
  users.value = data.data
}

async function toggleStatus(row: any) {
  const status = row.status === 'NORMAL' ? 'DISABLED' : 'NORMAL'
  await http.post(`/users/${row.id}/status`, { status })
  load()
}

onMounted(load)
</script>

<template>
  <h2>用户管理</h2>
  <el-table :data="users" style="width: 100%">
    <el-table-column prop="userNo" label="用户编号" />
    <el-table-column prop="phone" label="手机" />
    <el-table-column prop="nickname" label="昵称" />
    <el-table-column prop="status" label="状态" />
    <el-table-column label="操作">
      <template #default="{ row }">
        <el-button size="small" @click="toggleStatus(row)">
          {{ row.status === 'NORMAL' ? '封禁' : '解封' }}
        </el-button>
      </template>
    </el-table-column>
  </el-table>
</template>
