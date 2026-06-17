<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const phone = ref('')
const code = ref('')
const auth = useAuthStore()
const router = useRouter()
const devCode = ref('')

async function sendCode() {
  const res = await auth.sendCode(phone.value)
  devCode.value = res.data.data.devCode
}

async function submit() {
  await auth.loginPhone(phone.value, code.value)
  router.push('/products')
}
</script>

<template>
  <div class="card">
    <h2>登录</h2>
    <input v-model="phone" placeholder="手机号" />
    <div class="row">
      <input v-model="code" placeholder="验证码" />
      <button @click="sendCode">获取验证码</button>
    </div>
    <p v-if="devCode" class="hint">开发验证码：{{ devCode }}</p>
    <button class="primary" @click="submit">登录</button>
    <p><router-link to="/register">去注册</router-link></p>
  </div>
</template>

<style scoped>
.card { max-width: 360px; margin: 40px auto; display: flex; flex-direction: column; gap: 12px; }
input { padding: 10px; border: 1px solid #ddd; border-radius: 6px; }
.row { display: flex; gap: 8px; }
.primary { background: #1677ff; color: #fff; border: none; padding: 10px; border-radius: 6px; }
.hint { color: #888; font-size: 12px; }
</style>
