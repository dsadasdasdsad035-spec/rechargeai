<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ChatDotRound, Document, Goods, User, SwitchButton, List, Money, Key, Notebook, Reading, Wallet, Setting } from '@element-plus/icons-vue'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

const route = useRoute()
const router = useRouter()

const activeMenu = computed(() => {
  if (route.path.startsWith('/products')) return '/products'
  if (route.path.startsWith('/service-types')) return '/service-types'
  if (route.path.startsWith('/articles')) return '/articles'
  if (route.path.startsWith('/fulfillment')) return '/fulfillment'
  if (route.path.startsWith('/support')) return '/support'
  if (route.path.startsWith('/refunds')) return '/refunds'
  if (route.path.startsWith('/roles')) return '/roles'
  if (route.path.startsWith('/audit-logs')) return '/audit-logs'
  if (route.path.startsWith('/finance')) return '/finance'
  if (route.path.startsWith('/settings')) return '/settings'
  return route.path
})

function logout() {
  localStorage.removeItem('adminToken')
  localStorage.removeItem('adminId')
  localStorage.removeItem('adminRoles')
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
          <el-menu-item index="/fulfillment">
            <el-icon><List /></el-icon>
            <span>履约任务</span>
          </el-menu-item>
          <el-menu-item index="/support">
            <el-icon><ChatDotRound /></el-icon>
            <span>在线客服</span>
          </el-menu-item>
          <el-menu-item index="/refunds">
            <el-icon><Money /></el-icon>
            <span>退款管理</span>
          </el-menu-item>
          <el-sub-menu index="/finance">
            <template #title>
              <el-icon><Wallet /></el-icon>
              <span>资金管理</span>
            </template>
            <el-menu-item index="/finance">资金概览</el-menu-item>
            <el-menu-item index="/finance/ledger">账本流水</el-menu-item>
            <el-menu-item index="/finance/withdrawals">提现管理</el-menu-item>
            <el-menu-item index="/finance/payout-accounts">收款账户</el-menu-item>
            <el-menu-item index="/finance/settings">支付设置</el-menu-item>
          </el-sub-menu>
          <el-sub-menu index="/products">
            <template #title>
              <el-icon><Goods /></el-icon>
              <span>产品管理</span>
            </template>
            <el-menu-item index="/products">产品列表</el-menu-item>
            <el-menu-item index="/service-types">服务类型教程</el-menu-item>
          </el-sub-menu>
          <el-menu-item index="/articles">
            <el-icon><Reading /></el-icon>
            <span>文章管理</span>
          </el-menu-item>
          <el-menu-item index="/users">
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
          <el-menu-item index="/roles">
            <el-icon><Key /></el-icon>
            <span>角色管理</span>
          </el-menu-item>
          <el-menu-item index="/audit-logs">
            <el-icon><Notebook /></el-icon>
            <span>审计日志</span>
          </el-menu-item>
          <el-menu-item index="/settings">
            <el-icon><Setting /></el-icon>
            <span>系统配置</span>
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
