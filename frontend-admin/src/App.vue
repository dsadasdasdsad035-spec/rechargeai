<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Document, Goods, User, SwitchButton } from '@element-plus/icons-vue'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

const route = useRoute()
const router = useRouter()

const activeMenu = computed(() => {
  if (route.path.startsWith('/products')) return '/products'
  return route.path
})

function logout() {
  localStorage.removeItem('adminToken')
  router.push('/login')
}
</script>

<template>
  <el-config-provider :locale="zhCn">
    <el-container v-if="route.path !== '/login'" class="layout">
      <el-aside width="220px" class="aside">
        <div class="brand">
          <span class="brand__mark">R</span>
          <div>
            <div class="brand__name">RechargeAi</div>
            <div class="brand__sub">管理后台</div>
          </div>
        </div>

        <el-menu
          router
          :default-active="activeMenu"
          class="side-menu"
        >
          <el-menu-item index="/orders">
            <el-icon><Document /></el-icon>
            <span>订单（只读）</span>
          </el-menu-item>
          <el-menu-item index="/products">
            <el-icon><Goods /></el-icon>
            <span>产品管理</span>
          </el-menu-item>
          <el-menu-item index="/users">
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
        </el-menu>

        <div class="aside-footer">
          <el-button class="logout-btn" text @click="logout">
            <el-icon><SwitchButton /></el-icon>
            退出登录
          </el-button>
        </div>
      </el-aside>

      <el-container class="main-wrap">
        <el-main class="main">
          <RouterView />
        </el-main>
      </el-container>
    </el-container>

    <RouterView v-else />
  </el-config-provider>
</template>

<style scoped>
.layout {
  min-height: 100vh;
}

.aside {
  display: flex;
  flex-direction: column;
  background: var(--wild-surface);
  border-right: 1px solid var(--wild-border);
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px 16px;
  border-bottom: 1px solid var(--wild-border);
}

.brand__mark {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--wild-primary);
  color: #fff;
  font-family: 'Noto Serif SC', serif;
  font-weight: 600;
  border-radius: 8px;
  flex-shrink: 0;
}

.brand__name {
  font-family: 'Noto Serif SC', serif;
  font-weight: 600;
  font-size: 15px;
  color: var(--wild-text-heading);
}

.brand__sub {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 2px;
}

.side-menu {
  flex: 1;
  border-right: none;
  padding: 8px;
  background: transparent;
}

.side-menu :deep(.el-menu-item) {
  border-radius: 6px;
  margin-bottom: 4px;
  height: 44px;
}

.side-menu :deep(.el-menu-item.is-active) {
  background: var(--el-color-primary-light-9);
  color: var(--wild-primary);
  font-weight: 500;
}

.aside-footer {
  padding: 12px 16px 16px;
  border-top: 1px solid var(--wild-border);
}

.logout-btn {
  width: 100%;
  justify-content: flex-start;
  color: var(--el-text-color-secondary);
}

.logout-btn:hover {
  color: var(--el-color-danger);
}

.main-wrap {
  background: var(--wild-bg);
}

.main {
  padding: 24px 28px;
  max-width: 1200px;
}

@media (max-width: 768px) {
  .layout {
    flex-direction: column;
  }

  .aside {
    width: 100% !important;
    border-right: none;
    border-bottom: 1px solid var(--wild-border);
  }

  .side-menu {
    display: flex;
    padding: 0 8px 8px;
  }

  .side-menu :deep(.el-menu-item) {
    flex: 1;
    justify-content: center;
    padding: 0 8px !important;
  }

  .side-menu :deep(.el-menu-item span) {
    display: none;
  }

  .aside-footer {
    display: none;
  }

  .main {
    padding: 16px;
  }
}
</style>
