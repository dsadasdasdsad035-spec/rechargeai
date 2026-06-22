<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import http from '../services/http'

const router = useRouter()
const submitting = ref(false)

const form = ref({
  name: '',
  salePrice: 0,
  periodDays: 30,
  currency: 'CNY',
  complianceNotice: '',
})

async function submit() {
  if (!form.value.name.trim()) {
    ElMessage.warning('请填写产品名称')
    return
  }
  submitting.value = true
  try {
    await http.post('/products', form.value)
    ElMessage.success('产品已创建')
    router.push('/products')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="admin-page">
    <header class="admin-page__header">
      <div class="admin-page__header-text">
        <el-button text :icon="ArrowLeft" @click="router.push('/products')">返回列表</el-button>
        <h2 style="margin-top: 8px">新建产品</h2>
        <p>填写产品基本信息与合规提示</p>
      </div>
    </header>

    <el-card shadow="never" style="max-width: 560px">
      <el-form label-width="100px" label-position="top">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="如 ChatGPT Plus 月度" />
        </el-form-item>
        <el-form-item label="售价（元）">
          <el-input-number v-model="form.salePrice" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="周期（天）">
          <el-input-number v-model="form.periodDays" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="合规提示">
          <el-input
            v-model="form.complianceNotice"
            type="textarea"
            :rows="3"
            placeholder="向用户展示的合规说明"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="submit">保存产品</el-button>
          <el-button @click="router.push('/products')">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>
