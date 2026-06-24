<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../services/http'

const username = ref('')
const password = ref('')
const loading = ref(false)
const router = useRouter()

async function login() {
  if (!username.value || !password.value) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const { data } = await http.post('/auth/login', {
      username: username.value,
      password: password.value,
    })
    localStorage.setItem('adminToken', data.data.accessToken)
    localStorage.setItem('adminId', String(data.data.adminId))
    localStorage.setItem('adminRoles', JSON.stringify(data.data.roles ?? []))
    ElMessage.success('登录成功')
    router.push('/orders')
  } catch {
    ElMessage.error('登录失败，请检查账号密码')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <el-card class="login-card" shadow="never">
      <div class="login-brand">
        <span class="login-brand__mark">R</span>
        <div>
          <div class="login-brand__title">RechargeAi 管理端</div>
          <div class="login-brand__sub">订阅助手运营后台</div>
        </div>
      </div>

      <form class="login-form" @submit.prevent="login">
        <div class="login-form__field">
          <label for="username">用户名</label>
          <el-input id="username" v-model="username" placeholder="请输入用户名" size="large" />
        </div>
        <div class="login-form__field">
          <label for="password">密码</label>
          <el-input
            id="password"
            v-model="password"
            type="password"
            placeholder="请输入密码"
            size="large"
            show-password
            @keyup.enter="login"
          />
        </div>
        <el-button type="primary" size="large" :loading="loading" native-type="submit" style="width: 100%">
          登录
        </el-button>
      </form>
    </el-card>
  </div>
</template>
