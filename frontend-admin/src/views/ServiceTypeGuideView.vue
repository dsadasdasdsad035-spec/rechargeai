<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listServiceTypes, updateServiceType, type ServiceTypeGuide } from '../services/serviceTypeApi'

const items = ref<ServiceTypeGuide[]>([])
const loading = ref(true)
const dialogVisible = ref(false)
const saving = ref(false)
const editing = ref<ServiceTypeGuide | null>(null)

const form = ref({
  displayName: '',
  accountTutorial: '',
  tokenTutorial: '',
})

async function load() {
  loading.value = true
  try {
    const { data } = await listServiceTypes()
    items.value = data.data
  } finally {
    loading.value = false
  }
}

function openEdit(row: ServiceTypeGuide) {
  editing.value = row
  form.value = {
    displayName: row.displayName,
    accountTutorial: row.accountTutorial ?? '',
    tokenTutorial: row.tokenTutorial ?? '',
  }
  dialogVisible.value = true
}

async function save() {
  if (!editing.value) return
  if (!form.value.displayName.trim()) {
    ElMessage.warning('请填写显示名称')
    return
  }
  saving.value = true
  try {
    await updateServiceType(editing.value.serviceType, {
      displayName: form.value.displayName.trim(),
      accountTutorial: form.value.accountTutorial.trim(),
      tokenTutorial: form.value.tokenTutorial.trim(),
    })
    ElMessage.success('教程已保存')
    dialogVisible.value = false
    await load()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message ?? '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="admin-page">
    <header class="admin-page__header">
      <div class="admin-page__header-text">
        <h2>服务类型教程</h2>
        <p>按服务类型配置「确认订购」页展示的 AI 账号与 Token 获取教程</p>
      </div>
    </header>

    <el-table v-loading="loading" :data="items" stripe style="width: 100%">
      <el-table-column prop="serviceType" label="类型编码" width="120" />
      <el-table-column prop="displayName" label="显示名称" width="140" />
      <el-table-column label="账号教程" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">{{ row.accountTutorial || '—' }}</template>
      </el-table-column>
      <el-table-column label="Token 教程" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">{{ row.tokenTutorial || '—' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="88" fixed="right">
        <template #default="{ row }">
          <el-button size="small" link type="primary" @click="openEdit(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="`编辑教程 · ${editing?.serviceType}`" width="640px" destroy-on-close>
      <el-form label-position="top">
        <el-form-item label="显示名称" required>
          <el-input v-model="form.displayName" placeholder="如 ChatGPT" />
        </el-form-item>
        <el-form-item label="如何获取 AI 账号">
          <el-input
            v-model="form.accountTutorial"
            type="textarea"
            :rows="6"
            placeholder="支持多行文本，将展示在用户「确认订购」页"
          />
        </el-form-item>
        <el-form-item label="如何获取 Session Token">
          <el-input
            v-model="form.tokenTutorial"
            type="textarea"
            :rows="8"
            placeholder="说明如何从浏览器或官方渠道获取 Token"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
