<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getFinanceOverview, type FinanceOverview } from '../services/financeApi'

const router = useRouter()
const loading = ref(true)
const overview = ref<FinanceOverview | null>(null)

function fmt(n: number | undefined) {
  if (n == null) return '¥0.00'
  return `¥${Number(n).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

async function load() {
  loading.value = true
  try {
    const { data } = await getFinanceOverview()
    overview.value = data.data
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="admin-page">
    <header class="admin-page__header">
      <div class="admin-page__header-text">
        <h2>资金概览</h2>
        <p>已结算实收仅含履约成功（SUCCESS）订单，与运营统计口径一致</p>
      </div>
      <el-button type="primary" @click="router.push('/finance/ledger')">账本流水</el-button>
      <el-button @click="router.push('/finance/withdrawals')">提现管理</el-button>
      <el-button @click="router.push('/finance/payout-accounts')">收款账户</el-button>
    </header>

    <el-row v-loading="loading" :gutter="16" class="stat-row">
      <el-col :xs="24" :sm="12" :lg="8">
        <el-card shadow="never" class="stat-card">
          <div class="stat-card__label">已结算实收</div>
          <div class="stat-card__value">{{ fmt(overview?.settledRevenue) }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="8">
        <el-card shadow="never" class="stat-card stat-card--primary">
          <div class="stat-card__label">可提现余额</div>
          <div class="stat-card__value">{{ fmt(overview?.availableBalance) }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="8">
        <el-card shadow="never" class="stat-card">
          <div class="stat-card__label">累计已退款</div>
          <div class="stat-card__value">{{ fmt(overview?.totalRefunded) }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="8">
        <el-card shadow="never" class="stat-card">
          <div class="stat-card__label">提现冻结中</div>
          <div class="stat-card__value">{{ fmt(overview?.frozenForWithdrawal) }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="8">
        <el-card shadow="never" class="stat-card">
          <div class="stat-card__label">累计已提现</div>
          <div class="stat-card__value">{{ fmt(overview?.totalWithdrawn) }}</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.stat-row {
  margin-top: 8px;
}

.stat-card {
  margin-bottom: 16px;
  border-radius: 12px;
}

.stat-card--primary {
  border-color: var(--el-color-primary-light-5);
  background: linear-gradient(135deg, #f0f7ff 0%, #fff 100%);
}

.stat-card__label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-bottom: 8px;
}

.stat-card__value {
  font-size: 28px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}
</style>
