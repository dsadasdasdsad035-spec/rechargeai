<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getNotificationSetting,
  updateNotificationSetting,
  type NotificationSetting,
} from '../services/settingsApi'

const loading = ref(true)
const saving = ref(false)
const setting = ref<NotificationSetting | null>(null)
const form = reactive({
  supportOfflineNotifyEmail: '296629801@qq.com',
  orderPaidNotifyEmail: '296629801@qq.com',
  orderPaidNotifyEnabled: true,
})

function formatUpdatedAt(value?: string | null) {
  if (!value) return '尚未记录'
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

function applySetting(value: NotificationSetting) {
  setting.value = value
  form.supportOfflineNotifyEmail = value.supportOfflineNotifyEmail
  form.orderPaidNotifyEmail = value.orderPaidNotifyEmail
  form.orderPaidNotifyEnabled = value.orderPaidNotifyEnabled
}

function hasInvalidEmail(value: string) {
  return !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value.trim())
}

async function load() {
  loading.value = true
  try {
    const { data } = await getNotificationSetting()
    applySetting(data.data)
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message ?? '加载系统配置失败')
  } finally {
    loading.value = false
  }
}

async function save() {
  if (hasInvalidEmail(form.supportOfflineNotifyEmail)) {
    ElMessage.warning('请填写正确的离线客服提醒邮箱')
    return
  }
  if (hasInvalidEmail(form.orderPaidNotifyEmail)) {
    ElMessage.warning('请填写正确的订单支付完成提醒邮箱')
    return
  }
  saving.value = true
  try {
    const { data } = await updateNotificationSetting({
      supportOfflineNotifyEmail: form.supportOfflineNotifyEmail.trim(),
      orderPaidNotifyEmail: form.orderPaidNotifyEmail.trim(),
      orderPaidNotifyEnabled: form.orderPaidNotifyEnabled,
    })
    applySetting(data.data)
    ElMessage.success('系统配置已保存')
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message ?? '保存系统配置失败')
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
        <h2>系统配置</h2>
        <p>维护管理员邮件通知；邮件总开关仍由服务器 MAIL_ENABLED 控制</p>
      </div>
    </header>

    <el-card v-loading="loading" shadow="never" class="setting-card">
      <el-form label-position="top">
        <el-form-item label="离线客服提醒邮箱" required>
          <el-input v-model="form.supportOfflineNotifyEmail" maxlength="128" clearable />
          <p class="form-tip">后台在线客服无人在线时，用户发来客服消息会通知此邮箱。</p>
        </el-form-item>

        <el-divider />

        <el-form-item label="订单支付完成邮件提醒">
          <el-switch
            v-model="form.orderPaidNotifyEnabled"
            active-text="开启"
            inactive-text="关闭"
          />
        </el-form-item>

        <el-form-item label="订单支付完成提醒邮箱" required>
          <el-input v-model="form.orderPaidNotifyEmail" maxlength="128" clearable />
          <p class="form-tip">用户订单首次支付成功后会通知此邮箱。</p>
        </el-form-item>

        <div class="preview-box">
          <div class="preview-box__label">最后更新</div>
          <div class="preview-box__value">{{ formatUpdatedAt(setting?.updatedAt) }}</div>
        </div>

        <el-form-item>
          <el-button type="primary" :loading="saving" @click="save">保存配置</el-button>
          <el-button @click="load">重新加载</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.setting-card {
  max-width: 760px;
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
  font-size: 16px;
  font-weight: 600;
  color: var(--wild-text-heading);
}
</style>
