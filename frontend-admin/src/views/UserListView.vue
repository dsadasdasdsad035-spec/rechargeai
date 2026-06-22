<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../services/http'
import { USER_STATUS_LABEL, userStatusType } from '../utils/statusLabels'

const users = ref<any[]>([])
const loading = ref(true)

async function load() {
  loading.value = true
  try {
    const { data } = await http.get('/users')
    users.value = data.data
  } finally {
    loading.value = false
  }
}

async function toggleStatus(row: any) {
  const nextStatus = row.status === 'NORMAL' ? 'DISABLED' : 'NORMAL'
  const action = nextStatus === 'DISABLED' ? '封禁' : '解封'
  await ElMessageBox.confirm(`确定要${action}用户 ${row.phone || row.userNo} 吗？`, '确认操作', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
  await http.post(`/users/${row.id}/status`, { status: nextStatus })
  ElMessage.success(`已${action}`)
  load()
}

onMounted(load)
</script>

<template>
  <div class="admin-page">
    <header class="admin-page__header">
      <div class="admin-page__header-text">
        <h2>用户管理</h2>
        <p>查看注册用户，支持封禁与解封</p>
      </div>
    </header>

    <el-table v-loading="loading" :data="users" stripe style="width: 100%">
      <el-table-column prop="userNo" label="用户编号" min-width="140" show-overflow-tooltip />
      <el-table-column prop="phone" label="手机" width="140" />
      <el-table-column prop="nickname" label="昵称" min-width="100" />
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="userStatusType(row.status)" size="small">
            {{ USER_STATUS_LABEL[row.status] ?? row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="88" fixed="right">
        <template #default="{ row }">
          <el-button
            size="small"
            link
            :type="row.status === 'NORMAL' ? 'danger' : 'primary'"
            @click="toggleStatus(row)"
          >
            {{ row.status === 'NORMAL' ? '封禁' : '解封' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>
