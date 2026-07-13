<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { listLedger, type LedgerEntry } from '../services/financeApi'
import { formatMoney, formatSignedMoney } from '../utils/money'

const entries = ref<LedgerEntry[]>([])
const loading = ref(true)
const entryType = ref('')
const total = ref(0)
const pageNo = ref(1)
const pageSize = 20

const typeLabels: Record<string, string> = {
  SETTLE: '订单结算',
  REFUND: '退款扣账',
  WITHDRAW_FREEZE: '提现冻结',
  WITHDRAW_RELEASE: '冻结释放',
  WITHDRAW_COMPLETE: '打款完成',
}

async function load() {
  loading.value = true
  try {
    const { data } = await listLedger({
      pageNo: pageNo.value,
      pageSize,
      entryType: entryType.value || undefined,
    })
    entries.value = data.data.items
    total.value = data.data.total
  } finally {
    loading.value = false
  }
}

function onPageChange(p: number) {
  pageNo.value = p
  load()
}

onMounted(load)
</script>

<template>
  <div class="admin-page">
    <header class="admin-page__header">
      <div class="admin-page__header-text">
        <h2>账本流水</h2>
        <p>平台资金变动明细，支持按类型筛选</p>
      </div>
    </header>

    <div class="admin-page__filters">
      <el-select v-model="entryType" placeholder="流水类型" clearable style="width: 180px">
        <el-option label="订单结算" value="SETTLE" />
        <el-option label="退款扣账" value="REFUND" />
        <el-option label="提现冻结" value="WITHDRAW_FREEZE" />
        <el-option label="冻结释放" value="WITHDRAW_RELEASE" />
        <el-option label="打款完成" value="WITHDRAW_COMPLETE" />
      </el-select>
      <el-button type="primary" @click="() => { pageNo = 1; load() }">查询</el-button>
    </div>

    <el-table v-loading="loading" :data="entries" stripe style="width: 100%">
      <el-table-column label="时间" width="170">
        <template #default="{ row }">
          {{ new Date(row.createdAt).toLocaleString('zh-CN') }}
        </template>
      </el-table-column>
      <el-table-column label="类型" width="120">
        <template #default="{ row }">
          {{ typeLabels[row.entryType] || row.entryType }}
        </template>
      </el-table-column>
      <el-table-column label="金额" width="120" align="right">
        <template #default="{ row }">
          <span :class="row.amount >= 0 ? 'amount-plus' : 'amount-minus'">
            {{ formatSignedMoney(row.amount, 'CNY') }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="可用余额" width="120" align="right">
        <template #default="{ row }">{{ formatMoney(row.balanceAfter, 'CNY') }}</template>
      </el-table-column>
      <el-table-column prop="refType" label="来源类型" width="100" />
      <el-table-column prop="refId" label="来源单号" min-width="160" show-overflow-tooltip />
      <el-table-column prop="entryNo" label="流水号" min-width="180" show-overflow-tooltip />
    </el-table>

    <div class="pager">
      <el-pagination
        background
        layout="total, prev, pager, next"
        :total="total"
        :page-size="pageSize"
        :current-page="pageNo"
        @current-change="onPageChange"
      />
    </div>
  </div>
</template>

<style scoped>
.admin-page__filters {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}

.amount-plus {
  color: var(--el-color-success);
}

.amount-minus {
  color: var(--el-color-danger);
}

.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
