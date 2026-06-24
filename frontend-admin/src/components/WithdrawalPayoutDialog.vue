<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { confirmPayout } from '../services/withdrawalApi'
import type { WithdrawalSummary } from '../services/withdrawalApi'

const props = defineProps<{
  visible: boolean
  withdrawal: WithdrawalSummary | null
}>()

const emit = defineEmits<{
  'update:visible': [boolean]
  confirmed: []
}>()

const saving = ref(false)
const form = ref({
  actualAmount: 0,
  paidAt: '',
  externalVoucherNo: '',
  varianceReason: '',
})

watch(
  () => props.withdrawal,
  (w) => {
    if (w) {
      form.value = {
        actualAmount: Number(w.amount),
        paidAt: new Date().toISOString().slice(0, 16),
        externalVoucherNo: '',
        varianceReason: '',
      }
    }
  },
  { immediate: true },
)

const showVariance = () => {
  if (!props.withdrawal) return false
  return form.value.actualAmount !== Number(props.withdrawal.amount)
}

async function submit() {
  if (!props.withdrawal) return
  if (!form.value.externalVoucherNo.trim()) {
    ElMessage.warning('请填写外部凭证号')
    return
  }
  if (showVariance() && !form.value.varianceReason.trim()) {
    ElMessage.warning('金额不一致时需填写差额原因')
    return
  }
  saving.value = true
  try {
    await confirmPayout(props.withdrawal.withdrawalNo, {
      actualAmount: form.value.actualAmount,
      paidAt: new Date(form.value.paidAt).toISOString(),
      externalVoucherNo: form.value.externalVoucherNo.trim(),
      varianceReason: form.value.varianceReason.trim() || undefined,
    })
    ElMessage.success('打款已确认')
    emit('update:visible', false)
    emit('confirmed')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="确认打款"
    width="480px"
    @update:model-value="emit('update:visible', $event)"
  >
    <p v-if="withdrawal" class="hint">
      提现单 {{ withdrawal.withdrawalNo }} · 申请 ¥{{ Number(withdrawal.amount).toFixed(2) }}
    </p>
    <el-form label-width="100px">
      <el-form-item label="实际金额" required>
        <el-input-number v-model="form.actualAmount" :min="0.01" :precision="2" style="width: 100%" />
      </el-form-item>
      <el-form-item label="打款时间" required>
        <el-input v-model="form.paidAt" type="datetime-local" />
      </el-form-item>
      <el-form-item label="外部凭证号" required>
        <el-input v-model="form.externalVoucherNo" placeholder="银行流水号等" />
      </el-form-item>
      <el-form-item v-if="showVariance()" label="差额原因" required>
        <el-input v-model="form.varianceReason" type="textarea" :rows="2" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="emit('update:visible', false)">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">确认打款</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.hint {
  margin: 0 0 16px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
</style>
