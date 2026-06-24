<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createAdmin,
  listAdmins,
  listRoles,
  setAdminRoles,
  updateAdminStatus,
  type AdminAccount,
  type AdminRole,
} from '../services/roleApi'

const admins = ref<AdminAccount[]>([])
const roles = ref<AdminRole[]>([])
const loading = ref(true)
const dialogVisible = ref(false)
const roleDialogVisible = ref(false)
const saving = ref(false)
const selectedAdmin = ref<AdminAccount | null>(null)

const form = ref({
  username: '',
  password: '',
  displayName: '',
  roleCodes: [] as string[],
})

const roleSelection = ref<string[]>([])

const isSuperAdmin = computed(() => {
  try {
    const r: string[] = JSON.parse(localStorage.getItem('adminRoles') ?? '[]')
    return r.includes('SUPER_ADMIN')
  } catch {
    return false
  }
})

const ROLE_LABEL: Record<string, string> = {
  SUPER_ADMIN: '超级管理员',
  OPS_ADMIN: '运营管理员',
  CUSTOMER_SERVICE: '客服',
  FINANCE: '财务',
  AUDIT_READONLY: '只读审计',
}

async function load() {
  loading.value = true
  try {
    const [adminsRes, rolesRes] = await Promise.all([listAdmins(), listRoles()])
    admins.value = adminsRes.data.data
    roles.value = rolesRes.data.data
  } finally {
    loading.value = false
  }
}

function openCreate() {
  form.value = { username: '', password: '', displayName: '', roleCodes: ['AUDIT_READONLY'] }
  dialogVisible.value = true
}

async function submitCreate() {
  if (!form.value.username || !form.value.password) {
    ElMessage.warning('请填写用户名和密码')
    return
  }
  saving.value = true
  try {
    await createAdmin(form.value)
    dialogVisible.value = false
    ElMessage.success('管理员已创建')
    await load()
  } finally {
    saving.value = false
  }
}

function openRoles(row: AdminAccount) {
  selectedAdmin.value = row
  roleSelection.value = [...row.roleCodes]
  roleDialogVisible.value = true
}

async function submitRoles() {
  if (!selectedAdmin.value || roleSelection.value.length === 0) {
    ElMessage.warning('至少选择一个角色')
    return
  }
  saving.value = true
  try {
    await setAdminRoles(selectedAdmin.value.id, roleSelection.value)
    roleDialogVisible.value = false
    ElMessage.success('角色已更新')
    await load()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row: AdminAccount) {
  const next = row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  const action = next === 'DISABLED' ? '禁用' : '启用'
  await ElMessageBox.confirm(`确定${action}管理员 ${row.username}？`, '确认', { type: 'warning' })
  await updateAdminStatus(row.id, next)
  ElMessage.success(`已${action}`)
  await load()
}

onMounted(load)
</script>

<template>
  <div class="admin-page">
    <header class="admin-page__header">
      <div class="admin-page__header-text">
        <h2>角色与管理员</h2>
        <p>维护后台账号及 RBAC 角色分配（仅超级管理员可写）</p>
      </div>
      <div v-if="isSuperAdmin" class="admin-page__actions">
        <el-button type="primary" @click="openCreate">新建管理员</el-button>
      </div>
    </header>

    <el-table v-loading="loading" :data="admins" stripe style="width: 100%">
      <el-table-column prop="username" label="用户名" width="140" />
      <el-table-column prop="displayName" label="显示名" width="140" />
      <el-table-column label="角色" min-width="200">
        <template #default="{ row }">
          <el-tag v-for="code in row.roleCodes" :key="code" size="small" style="margin-right: 4px">
            {{ ROLE_LABEL[code] ?? code }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" size="small">
            {{ row.status === 'ACTIVE' ? '正常' : '已禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column v-if="isSuperAdmin" label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button size="small" link type="primary" @click="openRoles(row)">分配角色</el-button>
          <el-button
            size="small"
            link
            :type="row.status === 'ACTIVE' ? 'danger' : 'primary'"
            @click="toggleStatus(row)"
          >
            {{ row.status === 'ACTIVE' ? '禁用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" title="新建管理员" width="420px">
      <el-form label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="显示名">
          <el-input v-model="form.displayName" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleCodes" multiple style="width: 100%">
            <el-option
              v-for="r in roles"
              :key="r.roleCode"
              :label="r.roleName"
              :value="r.roleCode"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitCreate">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="roleDialogVisible" title="分配角色" width="420px">
      <el-select v-model="roleSelection" multiple style="width: 100%">
        <el-option v-for="r in roles" :key="r.roleCode" :label="r.roleName" :value="r.roleCode" />
      </el-select>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitRoles">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
