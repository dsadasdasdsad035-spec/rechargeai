<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BaseButton from '../components/ui/BaseButton.vue'
import BaseInput from '../components/ui/BaseInput.vue'
import BaseCard from '../components/ui/BaseCard.vue'
import MarkdownContent from '../components/MarkdownContent.vue'
import http from '../services/http'
import { getApiErrorMessage } from '../utils/apiError'
import { formatMoney } from '../utils/money'

interface OrderField {
  key: string
  label: string
  type?: string
  required?: boolean
  placeholder?: string
}

interface ServiceTypeGuide {
  serviceType: string
  displayName: string
  accountTutorial?: string | null
  tokenTutorial?: string | null
}

const route = useRoute()
const router = useRouter()
const product = ref<any>(null)
const fieldValues = ref<Record<string, string>>({})
const submitting = ref(false)
const submitError = ref('')

const orderFields = computed<OrderField[]>(() => {
  if (!product.value?.requiredFieldsJson) return []
  try {
    return JSON.parse(product.value.requiredFieldsJson)
  } catch {
    return []
  }
})

const guide = computed<ServiceTypeGuide | null>(() => product.value?.serviceTypeGuide ?? null)

onMounted(async () => {
  const { data } = await http.get(`/products/${route.params.id}`)
  product.value = data.data
  const initial: Record<string, string> = {}
  try {
    const fields = JSON.parse(data.data.requiredFieldsJson) as OrderField[]
    for (const field of fields) {
      initial[field.key] = ''
    }
  } catch {
    initial.target_account = ''
    initial.account_token = ''
  }
  fieldValues.value = initial
})

async function submit() {
  submitting.value = true
  submitError.value = ''
  try {
    const { data } = await http.post('/orders', {
      productId: Number(route.params.id),
      fields: fieldValues.value,
    })
    router.push(`/payment/${data.data.orderNo}`)
  } catch (e: unknown) {
    submitError.value = getApiErrorMessage(e, '提交订单失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page">
    <template v-if="product">
      <header class="page-header">
        <h2>确认订购</h2>
        <p>{{ product.name }} · {{ formatMoney(product.salePrice, product.currency) }}</p>
      </header>

      <BaseCard v-if="guide && (guide.accountTutorial || guide.tokenTutorial)" class="guide-card">
        <h3 class="guide-title">{{ guide.displayName }} · 获取凭证教程</h3>
        <section v-if="guide.accountTutorial" class="guide-section">
          <h4>如何获取 AI 账号</h4>
          <MarkdownContent :source="guide.accountTutorial" />
        </section>
        <section v-if="guide.tokenTutorial" class="guide-section">
          <h4>如何获取 Session Token</h4>
          <MarkdownContent :source="guide.tokenTutorial" />
        </section>
      </BaseCard>

      <BaseCard class="checkout-card">
        <form class="checkout-form" @submit.prevent="submit">
          <BaseInput
            v-for="field in orderFields"
            :key="field.key"
            v-model="fieldValues[field.key]"
            :label="field.required === false ? `${field.label}（选填）` : field.label"
            :type="field.type === 'email' ? 'email' : 'text'"
            :required="field.required !== false"
            :placeholder="field.placeholder ?? (field.required === false ? `选填：${field.label}` : `请填写${field.label}`)"
          />
          <p class="hint" role="note">请勿填写第三方服务登录密码；Session Token 将加密保存</p>
          <p v-if="submitError" class="submit-error" role="alert">{{ submitError }}</p>
          <BaseButton type="submit" block :disabled="submitting">
            {{ submitting ? '提交中…' : '提交订单' }}
          </BaseButton>
        </form>
      </BaseCard>
    </template>
  </div>
</template>

<style scoped>
.guide-card {
  max-width: 480px;
  margin-bottom: var(--space-md);
}

.guide-title {
  margin: 0 0 var(--space-md);
  font-size: 1rem;
  font-weight: 600;
  color: var(--color-text-heading);
}

.guide-section + .guide-section {
  margin-top: var(--space-md);
}

.guide-section h4 {
  margin: 0 0 var(--space-xs);
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-primary);
}

.checkout-card {
  max-width: 480px;
}

.checkout-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.hint {
  font-size: 0.8125rem;
  color: var(--color-warning);
  background: var(--color-warning-bg);
  padding: var(--space-sm) var(--space-md);
  border-radius: var(--radius-sm);
}

.submit-error {
  margin: 0;
  font-size: 0.875rem;
  color: var(--color-danger, #c0392b);
  background: var(--color-danger-bg, #fdecea);
  padding: var(--space-sm) var(--space-md);
  border-radius: var(--radius-sm);
}
</style>
