<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, RouterLink } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import BaseButton from '../components/ui/BaseButton.vue'
import BaseInput from '../components/ui/BaseInput.vue'
import BaseCard from '../components/ui/BaseCard.vue'

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
    const res = await auth.sendCodeEmail(email.value)
    devCode.value = res.data.data.devCode ?? ''
  } finally {
    sending.value = false
  }
}

async function submit() {
  submitting.value = true
  try {
    await auth.registerEmail(email.value, code.value)
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
        <p>使用邮箱验证码创建账户</p>
      </header>

      <form class="auth-form" @submit.prevent="submit">
        <BaseInput v-model="email" label="邮箱" type="email" placeholder="your@email.com" />
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

      <p class="auth-footer">
        已有账户？
        <RouterLink to="/login">去登录</RouterLink>
      </p>
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

.auth-footer {
  margin-top: var(--space-lg);
  text-align: center;
  font-size: 0.9375rem;
  color: var(--color-text-muted);
}
</style>
