<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import MarkdownEditor from '../components/MarkdownEditor.vue'
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
        <p>支持 Markdown 语法、图片与视频上传，内容展示在用户「确认订购」页</p>
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

    <el-dialog
      v-model="dialogVisible"
      :title="`编辑教程 · ${editing?.serviceType}`"
      width="760px"
      destroy-on-close
      top="5vh"
    >
      <el-form label-position="top">
        <el-form-item label="显示名称" required>
          <el-input v-model="form.displayName" placeholder="如 ChatGPT" />
        </el-form-item>
        <el-form-item>
          <MarkdownEditor
            v-model="form.accountTutorial"
            label="如何获取 AI 账号（Markdown）"
            :rows="8"
            placeholder="支持 **加粗**、列表、链接；可上传截图"
          />
        </el-form-item>
        <el-form-item>
          <MarkdownEditor
            v-model="form.tokenTutorial"
            label="如何获取 Session Token（Markdown）"
            :rows="10"
            placeholder="说明获取 Token 的步骤，建议配合截图"
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
