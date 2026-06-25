<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  createPayoutAccount,
  listPayoutAccounts,
  updatePayoutAccount,
  type PayoutAccount,
} from '../services/payoutApi'

const accounts = ref<PayoutAccount[]>([])
const loading = ref(true)
const dialogVisible = ref(false)
const editMode = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)

const form = ref({
  accountName: '',
  bankName: '',
  accountNo: '',
  enabled: true,
})

const roles = computed<string[]>(() => {
  try {
    return JSON.parse(localStorage.getItem('adminRoles') ?? '[]')
  } catch {
    return []
  }
})

const canManage = computed(() =>
  roles.value.includes('SUPER_ADMIN') || roles.value.includes('FINANCE'),
)

async function load() {
  loading.value = true
  try {
    const { data } = await listPayoutAccounts(!canManage.value)
    accounts.value = data.data
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editMode.value = false
  editingId.value = null
  form.value = { accountName: '', bankName: '', accountNo: '', enabled: true }
  dialogVisible.value = true
}

function openEdit(row: PayoutAccount) {
  editMode.value = true
  editingId.value = row.id
  form.value = {
    accountName: row.accountName,
    bankName: row.bankName,
    accountNo: '',
    enabled: row.enabled,
  }
  dialogVisible.value = true
}

async function submit() {
  if (!form.value.accountName || !form.value.bankName) {
    ElMessage.warning('请填写户名和开户行')
    return
  }
  if (!editMode.value && !form.value.accountNo) {
    ElMessage.warning('请填写银行账号')
    return
  }
  saving.value = true
  try {
    if (editMode.value && editingId.value != null) {
      const body: Record<string, unknown> = {
        accountName: form.value.accountName,
        bankName: form.value.bankName,
        enabled: form.value.enabled,
      }
      if (form.value.accountNo) body.accountNo = form.value.accountNo
      await updatePayoutAccount(editingId.value, body)
      ElMessage.success('账户已更新')
    } else {
      await createPayoutAccount({
        accountName: form.value.accountName,
        bankName: form.value.bankName,
        accountNo: form.value.accountNo,
      })
      ElMessage.success('账户已创建')
    }
    dialogVisible.value = false
    await load()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message ?? '保存失败，请确认当前账号具备财务或超管权限')
  } finally {
    saving.value = false
  }
}

async function toggleEnabled(row: PayoutAccount) {
  await updatePayoutAccount(row.id, { enabled: !row.enabled })
  ElMessage.success(row.enabled ? '已停用' : '已启用')
  await load()
}

onMounted(load)
</script>

<template>
  <div class="admin-page">
    <header class="admin-page__header">
      <div class="admin-page__header-text">
        <h2>收款账户</h2>
        <p>财务或超级管理员维护平台提现收款账户；创建提现时从启用账户中选择</p>
      </div>
      <el-button v-if="canManage" type="primary" @click="openCreate">新增账户</el-button>
    </header>

    <el-alert
      v-if="!canManage"
      type="info"
      show-icon
      :closable="false"
      title="当前账号仅可查看启用的收款账户。如需新增或编辑，请使用财务或超级管理员账号登录。"
      style="margin-bottom: 16px"
    />

    <el-table v-loading="loading" :data="accounts" stripe>
      <el-table-column prop="accountName" label="户名" width="140" />
      <el-table-column prop="bankName" label="开户行" min-width="160" />
      <el-table-column prop="accountLast4" label="账号尾号" width="100">
        <template #default="{ row }">****{{ row.accountLast4 }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
            {{ row.enabled ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column v-if="canManage" label="操作" width="180">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link @click="toggleEnabled(row)">
            {{ row.enabled ? '停用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editMode ? '编辑收款账户' : '新增收款账户'" width="480px">
      <el-form label-width="88px">
        <el-form-item label="户名" required>
          <el-input v-model="form.accountName" />
        </el-form-item>
        <el-form-item label="开户行" required>
          <el-input v-model="form.bankName" />
        </el-form-item>
        <el-form-item :label="editMode ? '新账号' : '银行账号'" :required="!editMode">
          <el-input v-model="form.accountNo" :placeholder="editMode ? '留空则不修改' : ''" />
        </el-form-item>
        <el-form-item v-if="editMode" label="启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
