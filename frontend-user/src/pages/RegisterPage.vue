<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import BaseButton from '../components/ui/BaseButton.vue'
import BaseInput from '../components/ui/BaseInput.vue'
import BaseCard from '../components/ui/BaseCard.vue'

type RegisterMode = 'phone' | 'email'

const mode = ref<RegisterMode>('email')
const phone = ref('')
const email = ref('')
const code = ref('')
const devCode = ref('')
const auth = useAuthStore()
const router = useRouter()
const sending = ref(false)
const submitting = ref(false)

async function sendCode() {
  sending.value = true
  devCode.value = ''
  try {
    const res =
      mode.value === 'email'
        ? await auth.sendCodeEmail(email.value)
        : await auth.sendCodePhone(phone.value)
    devCode.value = res.data.data.devCode ?? ''
  } finally {
    sending.value = false
  }
}

async function submit() {
  submitting.value = true
  try {
    if (mode.value === 'email') {
      await auth.registerEmail(email.value, code.value)
    } else {
      await auth.registerPhone(phone.value, code.value)
    }
    router.push('/products')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page auth-page">
    <BaseCard class="auth-card">
      <header class="auth-header">
        <h2>注册</h2>
        <p>创建账户以订购 AI 订阅服务</p>
      </header>

      <div class="mode-tabs" role="tablist">
        <button
          type="button"
          class="mode-tab"
          :class="{ 'mode-tab--active': mode === 'email' }"
          @click="mode = 'email'"
        >
          邮箱注册
        </button>
        <button
          type="button"
          class="mode-tab"
          :class="{ 'mode-tab--active': mode === 'phone' }"
          @click="mode = 'phone'"
        >
          手机号注册
        </button>
      </div>

      <form class="auth-form" @submit.prevent="submit">
        <BaseInput
          v-if="mode === 'email'"
          v-model="email"
          label="邮箱"
          type="email"
          placeholder="your@email.com"
        />
        <BaseInput
          v-else
          v-model="phone"
          label="手机号"
          type="tel"
          placeholder="请输入手机号"
        />
        <div class="code-row">
          <BaseInput v-model="code" label="验证码" placeholder="请输入验证码" />
          <BaseButton type="button" variant="secondary" :disabled="sending" @click="sendCode">
            {{ sending ? '发送中…' : '获取验证码' }}
          </BaseButton>
        </div>
        <p v-if="devCode" class="hint">开发验证码：{{ devCode }}</p>
        <BaseButton type="submit" block :disabled="submitting">
          {{ submitting ? '注册中…' : '注册' }}
        </BaseButton>
      </form>
    </BaseCard>
  </div>
</template>

<style scoped>
.auth-page {
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding-top: var(--space-2xl);
}

.auth-card {
  width: 100%;
  max-width: 400px;
}

.auth-header {
  margin-bottom: var(--space-lg);
}

.auth-header p {
  margin-top: var(--space-sm);
  color: var(--color-text-muted);
  font-size: 0.9375rem;
}

.mode-tabs {
  display: flex;
  gap: var(--space-xs);
  margin-bottom: var(--space-lg);
  padding: 4px;
  background: var(--color-neutral-bg);
  border-radius: var(--radius-sm);
}

.mode-tab {
  flex: 1;
  min-height: 40px;
  border: none;
  background: transparent;
  border-radius: var(--radius-sm);
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--color-text-muted);
  cursor: pointer;
  transition: background var(--duration-fast) var(--ease-out);
}

.mode-tab--active {
  background: var(--color-surface-raised);
  color: var(--color-primary);
  box-shadow: var(--shadow-sm);
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.code-row {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

@media (min-width: 400px) {
  .code-row {
    flex-direction: row;
    align-items: flex-end;
  }

  .code-row :deep(.field) {
    flex: 1;
  }
}

.hint {
  font-size: 0.8125rem;
  color: var(--color-text-subtle);
}
</style>
