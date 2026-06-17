<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const phone = ref('')
const code = ref('')
const auth = useAuthStore()
const router = useRouter()

async function submit() {
  await auth.registerPhone(phone.value, code.value)
  router.push('/products')
}
</script>

<template>
  <div class="card">
    <h2>注册</h2>
    <input v-model="phone" placeholder="手机号" />
    <input v-model="code" placeholder="验证码（开发环境可用 123456）" />
    <button class="primary" @click="submit">注册</button>
  </div>
</template>

<style scoped>
.card { max-width: 360px; margin: 40px auto; display: flex; flex-direction: column; gap: 12px; }
input { padding: 10px; border: 1px solid #ddd; border-radius: 6px; }
.primary { background: #1677ff; color: #fff; border: none; padding: 10px; border-radius: 6px; }
</style>
