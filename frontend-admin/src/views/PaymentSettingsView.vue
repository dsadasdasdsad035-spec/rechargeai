<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getPaymentSetting, updatePaymentSetting, type PaymentSetting } from '../services/settingsApi'

const loading = ref(true)
const saving = ref(false)
const setting = ref<PaymentSetting | null>(null)
const usdToCnyRate = ref(7.25)

const preview = computed(() => {
  const amount = 25.99 * Number(usdToCnyRate.value || 0)
  return amount.toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })
})

function formatUpdatedAt(value?: string | null) {
  if (!value) return '尚未记录'
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

async function load() {
  loading.value = true
  try {
    const { data } = await getPaymentSetting()
    setting.value = data.data
    usdToCnyRate.value = Number(data.data.usdToCnyRate)
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message ?? '加载支付设置失败')
  } finally {
    loading.value = false
  }
}

async function save() {
  if (!usdToCnyRate.value || usdToCnyRate.value <= 0) {
    ElMessage.warning('请填写大于 0 的汇率')
    return
  }
  saving.value = true
  try {
    const { data } = await updatePaymentSetting({
      usdToCnyRate: Number(usdToCnyRate.value),
    })
    setting.value = data.data
    usdToCnyRate.value = Number(data.data.usdToCnyRate)
    ElMessage.success('支付汇率已保存')
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message ?? '保存支付设置失败')
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
        <h2>支付设置</h2>
        <p>维护美元订单发起人民币支付时使用的汇率；已生成支付单会锁定当时汇率</p>
      </div>
    </header>

    <el-card v-loading="loading" shadow="never" class="setting-card">
      <el-form label-position="top">
        <el-form-item label="美元兑人民币支付汇率" required>
          <el-input-number
            v-model="usdToCnyRate"
            :min="0.000001"
            :precision="6"
            :step="0.01"
            controls-position="right"
            style="width: 240px"
          />
          <p class="form-tip">例如 7.250000 表示 1 USD = 7.250000 CNY。</p>
        </el-form-item>

        <div class="preview-box">
          <div class="preview-box__label">金额预览</div>
          <div class="preview-box__value">$25.99 → ¥{{ preview }}</div>
          <div class="preview-box__meta">最后更新：{{ formatUpdatedAt(setting?.updatedAt) }}</div>
        </div>

        <el-form-item>
          <el-button type="primary" :loading="saving" @click="save">保存汇率</el-button>
          <el-button @click="load">重新加载</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.setting-card {
  max-width: 720px;
}

.form-tip {
  width: 100%;
  margin: 8px 0 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.preview-box {
  margin: 8px 0 24px;
  padding: 16px;
  border: 1px solid var(--wild-border);
  border-radius: 8px;
  background: var(--wild-bg);
}

.preview-box__label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-bottom: 6px;
}

.preview-box__value {
  font-size: 22px;
  font-weight: 600;
  color: var(--wild-text-heading);
}

.preview-box__meta {
  margin-top: 8px;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}
</style>
