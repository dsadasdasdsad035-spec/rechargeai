<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import http from '../services/http'

const router = useRouter()
const submitting = ref(false)
const defaultRequiredFieldsJson = JSON.stringify(
  [
    { key: 'target_account', label: 'AI 账号', type: 'text', required: false },
    { key: 'account_token', label: 'Session Token', type: 'text', required: true },
  ],
  null,
  2,
)

const form = ref({
  productCode: '',
  name: '',
  serviceType: 'GENERAL',
  officialPrice: undefined as number | undefined,
  salePrice: 0,
  periodDays: 30,
  currency: 'CNY',
  status: 'OFF_SHELF',
  estimatedHours: undefined as number | undefined,
  sortOrder: 0,
  refundPolicyText: '',
  complianceNotice: '',
  requiredFieldsJson: defaultRequiredFieldsJson,
})

async function submit() {
  if (!form.value.name.trim()) {
    ElMessage.warning('请填写产品名称')
    return
  }
  try {
    const parsed = JSON.parse(form.value.requiredFieldsJson)
    if (!Array.isArray(parsed)) {
      throw new Error('字段配置必须是数组')
    }
    if (parsed.some((field) => String(field?.type ?? '').toLowerCase() === 'password')) {
      ElMessage.warning('下单字段类型不能使用 password，请改用 text')
      return
    }
  } catch {
    ElMessage.warning('下单字段配置不是合法 JSON 数组')
    return
  }
  submitting.value = true
  try {
    await http.post('/products', {
      productCode: form.value.productCode.trim() || undefined,
      name: form.value.name.trim(),
      serviceType: form.value.serviceType,
      officialPrice: form.value.officialPrice ?? null,
      salePrice: form.value.salePrice,
      periodDays: form.value.periodDays,
      currency: form.value.currency,
      status: form.value.status,
      estimatedHours: form.value.estimatedHours ?? null,
      sortOrder: form.value.sortOrder,
      refundPolicyText: form.value.refundPolicyText.trim() || null,
      complianceNotice: form.value.complianceNotice.trim() || null,
      requiredFieldsJson: form.value.requiredFieldsJson.trim(),
    })
    ElMessage.success('产品已创建')
    router.push('/products')
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message ?? '创建失败，请稍后重试')
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
        <p>填写产品基本信息、交付规则与用户下单字段</p>
      </div>
    </header>

    <el-card shadow="never" style="max-width: 880px">
      <el-form label-width="100px" label-position="top">
        <el-row :gutter="16">
          <el-col :xs="24" :md="12">
            <el-form-item label="名称" required>
              <el-input v-model="form.name" placeholder="如 ChatGPT Plus 月度" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="产品编码">
              <el-input v-model="form.productCode" placeholder="留空自动生成" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="服务类型">
              <el-select v-model="form.serviceType" style="width: 100%">
                <el-option label="ChatGPT" value="CHATGPT" />
                <el-option label="X Premium+" value="X_PREMIUM_PLUS" />
                <el-option label="Gemini Pro / Ultra" value="GEMINI_PRO_ULTRA" />
                <el-option label="Claude" value="CLAUDE" />
                <el-option label="Claude API" value="CLAUDE_API" />
                <el-option label="通用" value="GENERAL" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="状态">
              <el-select v-model="form.status" style="width: 100%">
                <el-option label="下架" value="OFF_SHELF" />
                <el-option label="上架" value="ON_SHELF" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="8">
            <el-form-item label="售价（元）" required>
              <el-input-number v-model="form.salePrice" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="8">
            <el-form-item label="官方参考价">
              <el-input-number
                v-model="form.officialPrice"
                :min="0"
                :precision="2"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="8">
            <el-form-item label="币种">
              <el-select v-model="form.currency" style="width: 100%">
                <el-option label="人民币 CNY" value="CNY" />
                <el-option label="美元 USD" value="USD" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="8">
            <el-form-item label="周期（天）" required>
              <el-input-number v-model="form.periodDays" :min="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="8">
            <el-form-item label="预计处理（小时）">
              <el-input-number v-model="form.estimatedHours" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="8">
            <el-form-item label="排序">
              <el-input-number v-model="form.sortOrder" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="退款政策">
          <el-input
            v-model="form.refundPolicyText"
            type="textarea"
            :rows="3"
            placeholder="向用户展示的退款政策"
          />
        </el-form-item>
        <el-form-item label="合规提示">
          <el-input
            v-model="form.complianceNotice"
            type="textarea"
            :rows="3"
            placeholder="向用户展示的合规说明"
          />
        </el-form-item>
        <el-form-item label="下单字段配置 JSON" required>
          <el-input
            v-model="form.requiredFieldsJson"
            type="textarea"
            :rows="8"
            placeholder="配置用户下单时需要填写的字段"
          />
          <p class="form-tip">Session Token 使用 type: "text"；AI 账号默认选填。</p>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="submit">保存产品</el-button>
          <el-button @click="router.push('/products')">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.form-tip {
  margin: 8px 0 0;
  font-size: 12px;
  line-height: 1.5;
  color: var(--el-text-color-secondary);
}
</style>
