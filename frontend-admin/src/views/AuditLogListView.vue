<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { listAuditLogs, type AuditLog } from '../services/auditApi'

const logs = ref<AuditLog[]>([])
const loading = ref(true)
const operationType = ref('')
const targetType = ref('')
const targetId = ref('')

async function load() {
  loading.value = true
  try {
    const { data } = await listAuditLogs({
      pageNo: 1,
      pageSize: 50,
      operationType: operationType.value || undefined,
      targetType: targetType.value || undefined,
      targetId: targetId.value || undefined,
    })
    logs.value = data.data.items
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="admin-page">
    <header class="admin-page__header">
      <div class="admin-page__header-text">
        <h2>审计日志</h2>
        <p>追溯管理员敏感操作（强制改状态、退款、封禁等）</p>
      </div>
    </header>

    <div class="admin-page__filters">
      <el-input v-model="operationType" placeholder="操作类型" clearable style="width: 160px" />
      <el-input v-model="targetType" placeholder="对象类型" clearable style="width: 140px" />
      <el-input v-model="targetId" placeholder="对象 ID" clearable style="width: 160px" />
      <el-button type="primary" @click="load">查询</el-button>
    </div>

    <el-table v-loading="loading" :data="logs" stripe style="width: 100%">
      <el-table-column label="时间" width="170">
        <template #default="{ row }">
          {{ new Date(row.createdAt).toLocaleString('zh-CN') }}
        </template>
      </el-table-column>
      <el-table-column prop="operatorName" label="操作人" width="120" />
      <el-table-column prop="operationType" label="操作类型" width="160" />
      <el-table-column prop="targetType" label="对象类型" width="120" />
      <el-table-column prop="targetId" label="对象 ID" width="140" show-overflow-tooltip />
      <el-table-column prop="beforeValue" label="变更前" min-width="140" show-overflow-tooltip />
      <el-table-column prop="afterValue" label="变更后" min-width="140" show-overflow-tooltip />
      <el-table-column prop="reason" label="原因" min-width="120" show-overflow-tooltip />
    </el-table>
  </div>
</template>

<style scoped>
.admin-page__filters {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}
</style>
