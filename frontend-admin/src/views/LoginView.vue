<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '../services/http'

const username = ref('admin')
const password = ref('changeme')
const router = useRouter()

async function login() {
  const { data } = await http.post('/auth/login', { username: username.value, password: password.value })
  localStorage.setItem('adminToken', data.data.accessToken)
  router.push('/orders')
}
</script>

<template>
  <div class="login">
    <el-card style="width: 360px">
      <h2>管理端登录</h2>
      <el-input v-model="username" placeholder="用户名" style="margin-bottom: 12px" />
      <el-input v-model="password" type="password" placeholder="密码" style="margin-bottom: 12px" />
      <el-button type="primary" @click="login" style="width: 100%">登录</el-button>
    </el-card>
  </div>
</template>

<style scoped>
.login { display: flex; justify-content: center; align-items: center; min-height: 100vh; }
</style>
