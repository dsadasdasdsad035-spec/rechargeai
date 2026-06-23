<script setup lang="ts">
import { ref } from 'vue'
import BaseButton from './ui/BaseButton.vue'
import { submitFulfillmentSupplement } from '../services/fulfillmentApi'

const props = defineProps<{
  orderNo: string
}>()

const emit = defineEmits<{ submitted: [] }>()

const content = ref('')
const loading = ref(false)
const error = ref('')

async function submit() {
  if (!content.value.trim()) {
    error.value = '请填写补充说明'
    return
  }
  loading.value = true
  error.value = ''
  try {
    await submitFulfillmentSupplement(props.orderNo, content.value.trim())
    content.value = ''
    emit('submitted')
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } } }
    error.value = err.response?.data?.message ?? '提交失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="supplement-form">
    <label class="supplement-label" for="fulfillment-content">补充资料说明</label>
    <textarea
      id="fulfillment-content"
      v-model="content"
      class="supplement-input"
      rows="4"
      placeholder="请描述您已完成的操作或需要客服知晓的信息"
    />
    <p v-if="error" class="supplement-error">{{ error }}</p>
    <BaseButton variant="primary" block :disabled="loading" @click="submit">
      {{ loading ? '提交中…' : '提交补充资料' }}
    </BaseButton>
  </div>
</template>

<style scoped>
.supplement-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

.supplement-label {
  font-size: 0.875rem;
  color: var(--color-text-muted);
}

.supplement-input {
  width: 100%;
  padding: var(--space-sm) var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font: inherit;
  resize: vertical;
  background: var(--color-surface);
  color: var(--color-text);
}

.supplement-error {
  font-size: 0.8125rem;
  color: var(--color-danger);
  margin: 0;
}
</style>
