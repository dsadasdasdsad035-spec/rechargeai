<script setup lang="ts">
import { RouterView } from 'vue-router'
</script>

<template>
  <el-container v-if="$route.path !== '/login'" class="layout">
    <el-aside width="200px" class="aside">
      <div class="title">WildAI 管理端</div>
      <el-menu router :default-active="$route.path">
        <el-menu-item index="/orders">订单（只读）</el-menu-item>
        <el-menu-item index="/products">产品管理</el-menu-item>
        <el-menu-item index="/users">用户管理</el-menu-item>
      </el-menu>
    </el-aside>
    <el-main><RouterView /></el-main>
  </el-container>
  <RouterView v-else />
</template>

<style scoped>
.layout { min-height: 100vh; }
.aside { border-right: 1px solid #eee; }
.title { padding: 16px; font-weight: 700; }
</style>
