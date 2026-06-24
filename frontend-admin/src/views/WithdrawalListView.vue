<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import WithdrawalPayoutDialog from '../components/WithdrawalPayoutDialog.vue'
import { listPayoutAccounts, type PayoutAccount } from '../services/payoutApi'
import {
  approveWithdrawal,
  cancelWithdrawal,
  createWithdrawal,
  exportWithdrawals,
  listWithdrawals,
  rejectWithdrawal,
  type WithdrawalSummary,
} from '../services/withdrawalApi'

const items = ref<WithdrawalSummary[]>([])
const accounts = ref<PayoutAccount[]>([])
const loading = ref(true)
const status = ref('')
const total = ref(0)
const pageNo = ref(1)
const pageSize = 20

const createVisible = ref(false)
const payoutVisible = ref(false)
const payoutTarget = ref<WithdrawalSummary | null>(null)
const saving = ref(false)

const createForm = ref({
  amount: 100,
  payoutAccountId: null as number | null,
  remark: '',
})

const roles = computed<string[]>(() => {
  try {
    return JSON.parse(localStorage.getItem('adminRoles') ?? '[]')
  } catch {
    return []
  }
})

const isSuperAdmin = computed(() => roles.value.includes('SUPER_ADMIN'))
const isFinance = computed(() => roles.value.includes('FINANCE') || isSuperAdmin.value)
const adminId = computed(() => Number(localStorage.getItem('adminId') || 0))

const STATUS_LABEL: Record<string, string> = {
  PENDING_APPROVAL: '待审批',
  APPROVED: '待打款',
  REJECTED: '已驳回',
  CANCELLED: '已取消',
  COMPLETED: '已完成',
  PAYOUT_TIMEOUT: '打款超时',
}

async function load() {
  loading.value = true
  try {
    const { data } = await listWithdrawals({
      pageNo: pageNo.value,
      pageSize,
      status: status.value || undefined,
    })
    items.value = data.data.items
    total.value = data.data.total
  } finally {
    loading.value = false
  }
}

async function loadAccounts() {
  const { data } = await listPayoutAccounts(true)
  accounts.value = data.data
  if (accounts.value.length && !createForm.value.payoutAccountId) {
    createForm.value.payoutAccountId = accounts.value[0].id
  }
}

function openCreate() {
  createForm.value = { amount: 100, payoutAccountId: accounts.value[0]?.id ?? null, remark: '' }
  createVisible.value = true
}

async function submitCreate() {
  if (!createForm.value.payoutAccountId) {
    ElMessage.warning('请选择收款账户')
    return
  }
  saving.value = true
  try {
    await createWithdrawal({
      amount: createForm.value.amount,
      payoutAccountId: createForm.value.payoutAccountId,
      remark: createForm.value.remark || undefined,
    })
    createVisible.value = false
    ElMessage.success('提现申请已提交')
    await load()
  } finally {
    saving.value = false
  }
}

async function onApprove(row: WithdrawalSummary) {
  await ElMessageBox.confirm(`确认审批通过 ${row.withdrawalNo}？`, '审批')
  await approveWithdrawal(row.withdrawalNo)
  ElMessage.success('已通过')
  await load()
}

async function onReject(row: WithdrawalSummary) {
  const { value } = await ElMessageBox.prompt('请填写驳回原因', '驳回提现', {
    inputPattern: /.+/,
    inputErrorMessage: '原因不能为空',
  })
  await rejectWithdrawal(row.withdrawalNo, value)
  ElMessage.success('已驳回')
  await load()
}

async function onCancel(row: WithdrawalSummary) {
  await ElMessageBox.confirm('确认取消该提现申请？冻结金额将释放', '取消')
  await cancelWithdrawal(row.withdrawalNo)
  ElMessage.success('已取消')
  await load()
}

function openPayout(row: WithdrawalSummary) {
  payoutTarget.value = row
  payoutVisible.value = true
}

async function onExport() {
  const { data } = await exportWithdrawals(status.value || undefined)
  const url = URL.createObjectURL(data as Blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'withdrawals.csv'
  a.click()
  URL.revokeObjectURL(url)
}

function canApprove(row: WithdrawalSummary) {
  return isSuperAdmin.value && row.status === 'PENDING_APPROVAL'
}

function canCancel(row: WithdrawalSummary) {
  return row.status === 'PENDING_APPROVAL'
    && (isSuperAdmin.value || row.applicantAdminId === adminId.value)
}

function canPayout(row: WithdrawalSummary) {
  return isFinance.value && (row.status === 'APPROVED' || row.status === 'PAYOUT_TIMEOUT')
}

onMounted(async () => {
  await Promise.all([load(), loadAccounts()])
})
</script>

<template>
  <div class="admin-page">
    <header class="admin-page__header">
      <div class="admin-page__header-text">
        <h2>提现管理</h2>
        <p>财务发起申请 → 超管审批 → 财务确认打款</p>
      </div>
      <div class="header-actions">
        <el-button @click="onExport">导出 CSV</el-button>
        <el-button v-if="isFinance" type="primary" @click="openCreate">发起提现</el-button>
      </div>
    </header>

    <div class="admin-page__filters">
      <el-select v-model="status" placeholder="状态" clearable style="width: 140px">
        <el-option v-for="(label, key) in STATUS_LABEL" :key="key" :label="label" :value="key" />
      </el-select>
      <el-button type="primary" @click="() => { pageNo = 1; load() }">查询</el-button>
    </div>

    <el-table v-loading="loading" :data="items" stripe>
      <el-table-column prop="withdrawalNo" label="提现单号" min-width="180" show-overflow-tooltip />
      <el-table-column label="金额" width="100" align="right">
        <template #default="{ row }">¥{{ Number(row.amount).toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag size="small">{{ STATUS_LABEL[row.status] || row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="payoutAccountSummary" label="收款账户" min-width="200" show-overflow-tooltip />
      <el-table-column prop="applicantName" label="申请人" width="100" />
      <el-table-column label="申请时间" width="170">
        <template #default="{ row }">
          {{ new Date(row.appliedAt).toLocaleString('zh-CN') }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button v-if="canApprove(row)" link type="primary" @click="onApprove(row)">通过</el-button>
          <el-button v-if="canApprove(row)" link type="danger" @click="onReject(row)">驳回</el-button>
          <el-button v-if="canCancel(row)" link @click="onCancel(row)">取消</el-button>
          <el-button v-if="canPayout(row)" link type="success" @click="openPayout(row)">确认打款</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        background
        layout="total, prev, pager, next"
        :total="total"
        :page-size="pageSize"
        :current-page="pageNo"
        @current-change="(p: number) => { pageNo = p; load() }"
      />
    </div>

    <el-dialog v-model="createVisible" title="发起提现" width="480px">
      <el-form label-width="88px">
        <el-form-item label="金额" required>
          <el-input-number v-model="createForm.amount" :min="100" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="收款账户" required>
          <el-select v-model="createForm.payoutAccountId" style="width: 100%" placeholder="选择账户">
            <el-option
              v-for="a in accounts"
              :key="a.id"
              :label="`${a.bankName} ${a.accountName} ****${a.accountLast4}`"
              :value="a.id"
            />
          </el-select>
          <p v-if="!accounts.length" class="warn">暂无启用收款账户，请联系超管配置</p>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="createForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" :disabled="!accounts.length" @click="submitCreate">
          提交
        </el-button>
      </template>
    </el-dialog>

    <WithdrawalPayoutDialog
      v-model:visible="payoutVisible"
      :withdrawal="payoutTarget"
      @confirmed="load"
    />
  </div>
</template>

<style scoped>
.header-actions {
  display: flex;
  gap: 8px;
}

.admin-page__filters {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.warn {
  margin: 8px 0 0;
  font-size: 12px;
  color: var(--el-color-warning);
}
</style>
