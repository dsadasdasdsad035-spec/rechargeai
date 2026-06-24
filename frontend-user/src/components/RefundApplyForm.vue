<script setup lang="ts">
import { ref } from 'vue'
import BaseButton from './ui/BaseButton.vue'
import { applyRefund } from '../services/refundApi'

const props = defineProps<{
  orderNo: string
  amount: number
}>()

const emit = defineEmits<{ submitted: [] }>()

const reason = ref('')
const loading = ref(false)
const error = ref('')

async function submit() {
  if (!reason.value.trim()) {
    error.value = '请填写退款原因'
    return
  }
  loading.value = true
  error.value = ''
  try {
    await applyRefund(props.orderNo, reason.value.trim())
    reason.value = ''
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
  <div class="refund-form">
    <p class="refund-hint">
      履约失败订单可申请<strong>全额退款</strong>（¥{{ amount }}），提交后进入人工审核。
    </p>
    <label class="refund-label" for="refund-reason">退款原因</label>
    <textarea
      id="refund-reason"
      v-model="reason"
      class="refund-input"
      rows="4"
      placeholder="请简要说明申请退款的原因"
    />
    <p v-if="error" class="refund-error">{{ error }}</p>
    <BaseButton variant="primary" block :disabled="loading" @click="submit">
      {{ loading ? '提交中…' : '提交退款申请' }}
    </BaseButton>
  </div>
</template>

<style scoped>
.refund-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

.refund-hint {
  margin: 0;
  font-size: 0.8125rem;
  color: var(--color-text-muted);
  line-height: 1.5;
}

.refund-label {
  font-size: 0.875rem;
  color: var(--color-text-muted);
}

.refund-input {
  width: 100%;
  padding: var(--space-sm) var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font: inherit;
  resize: vertical;
  background: var(--color-surface);
  color: var(--color-text);
}

.refund-error {
  font-size: 0.8125rem;
  color: var(--color-danger);
  margin: 0;
}
</style>
