<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  addTaskNote,
  assignTask,
  getFulfillmentTask,
  markTaskFailed,
  markTaskSuccess,
  waitUserTask,
  type FulfillmentTaskDetail,
} from '../services/fulfillmentApi'
import {
  FULFILLMENT_STATUS_LABEL,
  ORDER_STATUS_LABEL,
  fulfillmentStatusType,
  orderStatusType,
  canManageFulfillment,
} from '../utils/statusLabels'

const route = useRoute()
const router = useRouter()
const taskNo = computed(() => route.params.taskNo as string)
const detail = ref<FulfillmentTaskDetail | null>(null)
const loading = ref(true)
const noteContent = ref('')
const failureReason = ref('')
const successRemark = ref('')
const subscriptionStart = ref('')
const subscriptionEnd = ref('')
const waitUserInstruction = ref('')

const writable = computed(() => canManageFulfillment())
const isTerminal = computed(() =>
  detail.value ? ['SUCCESS', 'FAILED', 'CANCELLED'].includes(detail.value.status) : false,
)

async function load() {
  loading.value = true
  try {
    const { data } = await getFulfillmentTask(taskNo.value)
    detail.value = data.data
    if (data.data.subscriptionStart) {
      subscriptionStart.value = data.data.subscriptionStart.slice(0, 16)
    }
    if (data.data.subscriptionEnd) {
      subscriptionEnd.value = data.data.subscriptionEnd.slice(0, 16)
    }
  } finally {
    loading.value = false
  }
}

async function assignToSelf() {
  const adminId = Number(localStorage.getItem('adminId'))
  if (!adminId) {
    ElMessage.warning('无法获取当前管理员 ID，请重新登录')
    return
  }
  await assignTask(taskNo.value, adminId)
  ElMessage.success('已分派给自己')
  await load()
}

async function submitNote() {
  if (!noteContent.value.trim()) {
    ElMessage.warning('请输入备注内容')
    return
  }
  await addTaskNote(taskNo.value, noteContent.value.trim())
  noteContent.value = ''
  ElMessage.success('备注已保存')
  await load()
}

async function submitWaitUser() {
  await waitUserTask(taskNo.value, waitUserInstruction.value.trim() || undefined)
  waitUserInstruction.value = ''
  ElMessage.success('已标记等待用户')
  await load()
}

async function submitSuccess() {
  if (!subscriptionStart.value || !subscriptionEnd.value) {
    ElMessage.warning('请填写订阅起止时间')
    return
  }
  await markTaskSuccess(taskNo.value, {
    subscriptionStart: new Date(subscriptionStart.value).toISOString(),
    subscriptionEnd: new Date(subscriptionEnd.value).toISOString(),
    remark: successRemark.value.trim() || undefined,
  })
  ElMessage.success('履约已标记成功')
  await load()
}

async function submitFailed() {
  if (!failureReason.value.trim()) {
    ElMessage.warning('请填写失败原因')
    return
  }
  await ElMessageBox.confirm('确认标记履约失败？用户将可申请退款。', '确认操作', {
    type: 'warning',
  })
  await markTaskFailed(taskNo.value, { failureReason: failureReason.value.trim() })
  failureReason.value = ''
  ElMessage.success('已标记履约失败')
  await load()
}

onMounted(load)
</script>

<template>
  <div v-loading="loading" class="admin-page">
    <header class="admin-page__header">
      <div class="admin-page__header-text">
        <el-button link type="primary" @click="router.push('/fulfillment')">← 返回列表</el-button>
        <h2>履约任务 {{ taskNo }}</h2>
        <p v-if="detail">订单 {{ detail.orderNo }} · {{ detail.productName }}</p>
      </div>
    </header>

    <template v-if="detail">
      <el-descriptions :column="2" border class="detail-block">
        <el-descriptions-item label="任务状态">
          <el-tag :type="fulfillmentStatusType(detail.status)" size="small">
            {{ FULFILLMENT_STATUS_LABEL[detail.status] ?? detail.status }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="订单状态">
          <el-tag :type="orderStatusType(detail.orderStatus)" size="small">
            {{ ORDER_STATUS_LABEL[detail.orderStatus] ?? detail.orderStatus }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="金额">¥{{ detail.amount }} {{ detail.currency }}</el-descriptions-item>
        <el-descriptions-item label="处理人">{{ detail.assigneeName ?? '未分派' }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.subscriptionStart" label="订阅开始">
          {{ new Date(detail.subscriptionStart).toLocaleString('zh-CN') }}
        </el-descriptions-item>
        <el-descriptions-item v-if="detail.subscriptionEnd" label="订阅结束">
          {{ new Date(detail.subscriptionEnd).toLocaleString('zh-CN') }}
        </el-descriptions-item>
        <el-descriptions-item v-if="detail.failureReason" label="失败原因" :span="2">
          {{ detail.failureReason }}
        </el-descriptions-item>
      </el-descriptions>

      <section v-if="writable && !isTerminal" class="action-panel">
        <h3>处理操作</h3>
        <div class="action-row">
          <el-button type="primary" @click="assignToSelf">分派给自己</el-button>
          <el-button @click="submitWaitUser">标记等待用户</el-button>
        </div>

        <div class="action-form">
          <label>等待用户指引（可选）</label>
          <el-input v-model="waitUserInstruction" type="textarea" :rows="2" placeholder="告知用户需在官方页面完成的操作" />
        </div>

        <div class="action-form">
          <label>处理备注（用户可见）</label>
          <el-input v-model="noteContent" type="textarea" :rows="2" placeholder="填写处理进展或说明" />
          <el-button type="primary" plain @click="submitNote">保存备注</el-button>
        </div>

        <div class="action-form success-form">
          <h4>标记成功</h4>
          <div class="datetime-row">
            <div>
              <label>订阅开始</label>
              <el-date-picker
                v-model="subscriptionStart"
                type="datetime"
                placeholder="选择开始时间"
                value-format="YYYY-MM-DDTHH:mm:ss"
                style="width: 100%"
              />
            </div>
            <div>
              <label>订阅结束</label>
              <el-date-picker
                v-model="subscriptionEnd"
                type="datetime"
                placeholder="选择结束时间"
                value-format="YYYY-MM-DDTHH:mm:ss"
                style="width: 100%"
              />
            </div>
          </div>
          <el-input v-model="successRemark" placeholder="成功说明（可选）" />
          <el-button type="success" @click="submitSuccess">标记履约成功</el-button>
        </div>

        <div class="action-form fail-form">
          <h4>标记失败</h4>
          <el-input v-model="failureReason" type="textarea" :rows="2" placeholder="失败原因（必填，用户可见）" />
          <el-button type="danger" @click="submitFailed">标记履约失败</el-button>
        </div>
      </section>

      <p v-else-if="!writable" class="readonly-hint">当前角色仅可查看履约任务，不可执行写操作。</p>

      <section class="log-section">
        <h3>处理日志</h3>
        <el-timeline v-if="detail.logs.length">
          <el-timeline-item
            v-for="(log, idx) in detail.logs"
            :key="idx"
            :timestamp="new Date(log.createdAt).toLocaleString('zh-CN')"
            placement="top"
          >
            <el-tag size="small" type="info">{{ log.logType }}</el-tag>
            <span v-if="!log.userVisible" class="internal-tag">内部</span>
            <p>{{ log.content }}</p>
            <p v-if="log.operatorName" class="log-meta">操作人：{{ log.operatorName }}</p>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="暂无日志" />
      </section>
    </template>
  </div>
</template>

<style scoped>
.detail-block {
  margin-bottom: 24px;
}

.action-panel {
  background: var(--wild-surface);
  border: 1px solid var(--wild-border);
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 24px;
}

.action-panel h3,
.log-section h3 {
  margin: 0 0 16px;
  font-size: 16px;
}

.action-row {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.action-form {
  margin-bottom: 20px;
}

.action-form label {
  display: block;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-bottom: 6px;
}

.action-form .el-button {
  margin-top: 8px;
}

.datetime-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 12px;
}

.success-form,
.fail-form {
  padding-top: 16px;
  border-top: 1px dashed var(--wild-border);
}

.success-form h4,
.fail-form h4 {
  margin: 0 0 12px;
  font-size: 14px;
}

.readonly-hint {
  color: var(--el-text-color-secondary);
  margin-bottom: 24px;
}

.log-section {
  margin-top: 8px;
}

.internal-tag {
  margin-left: 8px;
  font-size: 12px;
  color: var(--el-color-warning);
}

.log-meta {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin: 4px 0 0;
}

@media (max-width: 768px) {
  .datetime-row {
    grid-template-columns: 1fr;
  }
}
</style>
