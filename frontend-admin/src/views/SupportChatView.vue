<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import {
  createSupportSocket,
  listSupportMessages,
  listSupportSessions,
  sendSupportMessage,
  type SupportEvent,
  type SupportMessage,
  type SupportSession,
} from '../services/supportApi'

const sessions = ref<SupportSession[]>([])
const messages = ref<SupportMessage[]>([])
const activeSessionNo = ref('')
const input = ref('')
const loading = ref(true)
const messageLoading = ref(false)
const sending = ref(false)
const socketReady = ref(false)
const listEl = ref<HTMLElement | null>(null)
let socket: WebSocket | null = null
let reconnectTimer: number | undefined

const activeSession = computed(() => sessions.value.find((item) => item.sessionNo === activeSessionNo.value) ?? null)

async function loadSessions(keepActive = true) {
  const { data } = await listSupportSessions()
  sessions.value = data.data.items
  if (!keepActive || !sessions.value.some((item) => item.sessionNo === activeSessionNo.value)) {
    activeSessionNo.value = sessions.value[0]?.sessionNo ?? ''
  }
}

async function selectSession(sessionNo: string) {
  activeSessionNo.value = sessionNo
  await loadMessages()
}

async function loadMessages() {
  if (!activeSessionNo.value) {
    messages.value = []
    return
  }
  messageLoading.value = true
  try {
    const { data } = await listSupportMessages(activeSessionNo.value)
    messages.value = data.data.items
    await loadSessions(true)
    await scrollBottom()
  } finally {
    messageLoading.value = false
  }
}

function connectSocket() {
  if (socket && socket.readyState !== WebSocket.CLOSED) return
  socket = createSupportSocket(handleSocketEvent, () => {
    socketReady.value = false
    window.clearTimeout(reconnectTimer)
    reconnectTimer = window.setTimeout(connectSocket, 2000)
  })
  if (!socket) return
  socket.addEventListener('open', () => {
    socketReady.value = true
  })
}

async function handleSocketEvent(event: SupportEvent) {
  if (event.type === 'SESSION_CREATED') {
    await loadSessions(true)
    return
  }

  if (event.type === 'MESSAGE' && event.message && event.session) {
    upsertSession(event.session)
    if (!activeSessionNo.value) {
      activeSessionNo.value = event.session.sessionNo
    }
    if (event.message.sessionNo === activeSessionNo.value) {
      appendMessage(event.message)
    }
  }
}

function upsertSession(session: SupportSession) {
  const index = sessions.value.findIndex((item) => item.sessionNo === session.sessionNo)
  if (index >= 0) {
    sessions.value[index] = session
  } else {
    sessions.value.unshift(session)
  }
  sessions.value = [...sessions.value].sort((a, b) => {
    const left = a.lastMessageAt ?? a.updatedAt
    const right = b.lastMessageAt ?? b.updatedAt
    return new Date(right).getTime() - new Date(left).getTime()
  })
}

function appendMessage(message: SupportMessage) {
  if (!messages.value.some((item) => item.id === message.id)) {
    messages.value.push(message)
    scrollBottom()
  }
}

async function send() {
  const content = input.value.trim()
  if (!content || sending.value || !activeSessionNo.value) return
  sending.value = true
  input.value = ''
  try {
    if (socket?.readyState === WebSocket.OPEN) {
      socket.send(JSON.stringify({ type: 'SEND_MESSAGE', sessionNo: activeSessionNo.value, content }))
    } else {
      const { data } = await sendSupportMessage(activeSessionNo.value, content)
      appendMessage(data.data)
    }
  } finally {
    sending.value = false
  }
}

async function scrollBottom() {
  await nextTick()
  if (listEl.value) {
    listEl.value.scrollTop = listEl.value.scrollHeight
  }
}

onMounted(async () => {
  try {
    await loadSessions(false)
    await loadMessages()
    connectSocket()
  } finally {
    loading.value = false
  }
})

onBeforeUnmount(() => {
  window.clearTimeout(reconnectTimer)
  socket?.close()
})
</script>

<template>
  <div class="admin-page support-page">
    <header class="admin-page__header">
      <div class="admin-page__header-text">
        <h2>在线客服</h2>
        <p>处理用户实时咨询并保留站内沟通记录</p>
      </div>
      <el-tag :type="socketReady ? 'success' : 'warning'" effect="plain">
        {{ socketReady ? '实时连接中' : '正在连接' }}
      </el-tag>
    </header>

    <div class="support-shell">
      <aside class="support-sessions">
        <div class="support-sessions__header">
          <strong>会话</strong>
          <el-button size="small" @click="loadSessions(true)">刷新</el-button>
        </div>
        <el-scrollbar class="support-sessions__list" v-loading="loading">
          <button
            v-for="session in sessions"
            :key="session.sessionNo"
            type="button"
            class="support-session"
            :class="{ 'support-session--active': session.sessionNo === activeSessionNo }"
            @click="selectSession(session.sessionNo)"
          >
            <span class="support-session__top">
              <strong>{{ session.subject }}</strong>
              <el-badge v-if="session.unreadAdminCount > 0" :value="session.unreadAdminCount" />
            </span>
            <span class="support-session__meta">用户 ID：{{ session.userId }}</span>
            <span class="support-session__message">{{ session.lastMessage ?? '暂无消息' }}</span>
          </button>
          <el-empty v-if="!loading && sessions.length === 0" description="暂无客服会话" />
        </el-scrollbar>
      </aside>

      <section class="support-chat">
        <header class="support-chat__header">
          <div v-if="activeSession">
            <strong>{{ activeSession.subject }}</strong>
            <span>{{ activeSession.sessionNo }}</span>
          </div>
          <span v-else>请选择会话</span>
        </header>

        <div ref="listEl" v-loading="messageLoading" class="support-chat__messages">
          <template v-if="messages.length > 0">
            <div
              v-for="message in messages"
              :key="message.id"
              class="support-message"
              :class="{ 'support-message--mine': message.senderType === 'ADMIN' }"
            >
              <div class="support-message__sender">{{ message.senderType === 'ADMIN' ? '客服' : '用户' }}</div>
              <div class="support-message__bubble">{{ message.content }}</div>
              <time>{{ new Date(message.createdAt).toLocaleString('zh-CN') }}</time>
            </div>
          </template>
          <el-empty v-else-if="activeSessionNo" description="暂无消息" />
          <el-empty v-else description="暂无选中会话" />
        </div>

        <form class="support-chat__form" @submit.prevent="send">
          <el-input
            v-model="input"
            type="textarea"
            :rows="3"
            maxlength="1000"
            show-word-limit
            resize="none"
            placeholder="输入回复内容"
            :disabled="!activeSessionNo"
            @keydown.enter.exact.prevent="send"
          />
          <el-button type="primary" native-type="submit" :disabled="!input.trim() || !activeSessionNo" :loading="sending">
            发送
          </el-button>
        </form>
      </section>
    </div>
  </div>
</template>

<style scoped>
.support-page {
  min-height: calc(100vh - 48px);
}

.support-shell {
  height: calc(100vh - 126px);
  min-height: 560px;
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  background: var(--wild-surface);
  border: 1px solid var(--wild-border);
  border-radius: 8px;
  overflow: hidden;
}

.support-sessions {
  min-width: 0;
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--wild-border);
  background: #fbfaf7;
}

.support-sessions__header {
  min-height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 14px;
  border-bottom: 1px solid var(--wild-border);
}

.support-sessions__list {
  flex: 1;
}

.support-session {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px 14px;
  border: none;
  border-bottom: 1px solid var(--wild-border);
  background: transparent;
  text-align: left;
  color: var(--wild-text);
}

.support-session:hover,
.support-session--active {
  background: var(--wild-primary-light);
}

.support-session__top {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  color: var(--wild-text-heading);
}

.support-session__meta,
.support-session__message {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.support-session__message {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.support-chat {
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.support-chat__header {
  min-height: 56px;
  display: flex;
  align-items: center;
  padding: 0 18px;
  border-bottom: 1px solid var(--wild-border);
}

.support-chat__header strong,
.support-chat__header span {
  display: block;
}

.support-chat__header span {
  margin-top: 2px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.support-chat__messages {
  flex: 1;
  min-height: 0;
  padding: 18px;
  overflow-y: auto;
  background: #f7f5f0;
}

.support-message {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  margin-bottom: 14px;
}

.support-message--mine {
  align-items: flex-end;
}

.support-message__sender,
.support-message time {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.support-message__bubble {
  max-width: min(72%, 640px);
  margin: 4px 0;
  padding: 10px 12px;
  border: 1px solid var(--wild-border);
  border-radius: 6px;
  background: #fff;
  color: var(--wild-text-heading);
  white-space: pre-wrap;
  word-break: break-word;
}

.support-message--mine .support-message__bubble {
  background: var(--wild-primary);
  border-color: var(--wild-primary);
  color: #fff;
}

.support-chat__form {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 92px;
  gap: 12px;
  padding: 14px;
  border-top: 1px solid var(--wild-border);
}

@media (max-width: 900px) {
  .support-shell {
    height: auto;
    min-height: 0;
    grid-template-columns: 1fr;
  }

  .support-sessions {
    max-height: 280px;
    border-right: none;
    border-bottom: 1px solid var(--wild-border);
  }

  .support-chat__messages {
    height: 420px;
  }
}
</style>
