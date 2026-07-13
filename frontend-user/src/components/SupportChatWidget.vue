<script setup lang="ts">
import { nextTick, onBeforeUnmount, ref } from 'vue'
import {
  createSupportSession,
  createSupportSocket,
  listSupportMessages,
  listSupportSessions,
  sendSupportMessage,
  type SupportEvent,
  type SupportMessage,
  type SupportSession,
} from '../services/supportApi'

const open = ref(false)
const loading = ref(false)
const sending = ref(false)
const input = ref('')
const activeSession = ref<SupportSession | null>(null)
const messages = ref<SupportMessage[]>([])
const listEl = ref<HTMLElement | null>(null)
const socketReady = ref(false)
let socket: WebSocket | null = null
let reconnectTimer: number | undefined

async function toggle() {
  open.value = !open.value
  if (open.value) {
    await ensureSession()
    connectSocket()
  }
}

async function ensureSession() {
  if (activeSession.value) return
  loading.value = true
  try {
    const { data } = await listSupportSessions()
    activeSession.value = data.data.items.find((item) => item.status === 'OPEN') ?? data.data.items[0] ?? null
    if (!activeSession.value) {
      const created = await createSupportSession('在线咨询')
      activeSession.value = created.data.data
    }
    await loadMessages()
  } finally {
    loading.value = false
  }
}

async function loadMessages() {
  if (!activeSession.value) return
  const { data } = await listSupportMessages(activeSession.value.sessionNo)
  messages.value = data.data.items
  await scrollBottom()
}

function connectSocket() {
  if (socket && socket.readyState !== WebSocket.CLOSED) return
  socket = createSupportSocket(handleSocketEvent, () => {
    socketReady.value = false
    if (open.value) {
      window.clearTimeout(reconnectTimer)
      reconnectTimer = window.setTimeout(connectSocket, 2000)
    }
  })
  if (!socket) return
  socket.addEventListener('open', () => {
    socketReady.value = true
  })
}

function handleSocketEvent(event: SupportEvent) {
  if (event.type === 'MESSAGE' && event.message && event.message.sessionNo === activeSession.value?.sessionNo) {
    appendMessage(event.message)
  }
}

function appendMessage(message: SupportMessage) {
  if (!messages.value.some((item) => item.id === message.id)) {
    messages.value.push(message)
    scrollBottom()
  }
}

async function send() {
  const content = input.value.trim()
  if (!content || sending.value) return
  await ensureSession()
  if (!activeSession.value) return
  sending.value = true
  input.value = ''
  try {
    if (socket?.readyState === WebSocket.OPEN) {
      socket.send(JSON.stringify({ type: 'SEND_MESSAGE', sessionNo: activeSession.value.sessionNo, content }))
    } else {
      const { data } = await sendSupportMessage(activeSession.value.sessionNo, content)
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

onBeforeUnmount(() => {
  window.clearTimeout(reconnectTimer)
  socket?.close()
})
</script>

<template>
  <div class="support-widget" :class="{ 'support-widget--open': open }">
    <section v-if="open" class="support-panel" aria-label="在线客服">
      <header class="support-panel__header">
        <div>
          <strong>在线客服</strong>
          <span>{{ socketReady ? '实时在线' : '正在连接' }}</span>
        </div>
        <button type="button" class="support-panel__close" aria-label="关闭客服窗口" @click="open = false">×</button>
      </header>

      <div ref="listEl" class="support-panel__messages">
        <div v-if="loading" class="support-panel__state">正在加载...</div>
        <template v-else>
          <div v-if="messages.length === 0" class="support-panel__state">请直接输入您的问题</div>
          <div
            v-for="message in messages"
            :key="message.id"
            class="support-message"
            :class="{ 'support-message--mine': message.senderType === 'USER' }"
          >
            <div class="support-message__bubble">{{ message.content }}</div>
            <time class="support-message__time">{{ new Date(message.createdAt).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }) }}</time>
          </div>
        </template>
      </div>

      <form class="support-panel__form" @submit.prevent="send">
        <textarea
          v-model="input"
          rows="2"
          maxlength="1000"
          placeholder="输入消息"
          @keydown.enter.exact.prevent="send"
        />
        <button type="submit" :disabled="!input.trim() || sending">发送</button>
      </form>
    </section>

    <button type="button" class="support-fab" @click="toggle">
      <span>客服</span>
    </button>
  </div>
</template>

<style scoped>
.support-widget {
  position: fixed;
  right: max(18px, env(safe-area-inset-right));
  bottom: max(18px, calc(env(safe-area-inset-bottom) + 18px));
  z-index: 120;
}

.support-panel {
  width: min(360px, calc(100vw - 32px));
  height: min(540px, calc(100dvh - 96px));
  margin-bottom: 12px;
  display: flex;
  flex-direction: column;
  background: var(--color-surface-raised);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg);
  overflow: hidden;
}

.support-panel__header {
  min-height: 58px;
  padding: 12px 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  background: var(--color-primary);
  color: #fff;
}

.support-panel__header strong,
.support-panel__header span {
  display: block;
}

.support-panel__header strong {
  font-size: 0.95rem;
}

.support-panel__header span {
  margin-top: 2px;
  font-size: 0.75rem;
  opacity: 0.82;
}

.support-panel__close {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: var(--radius-sm);
  background: rgb(255 255 255 / 0.12);
  color: #fff;
  font-size: 1.4rem;
  line-height: 1;
}

.support-panel__messages {
  flex: 1;
  padding: 14px;
  overflow-y: auto;
  background: var(--color-primary-subtle);
}

.support-panel__state {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-muted);
  font-size: 0.9rem;
}

.support-message {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  margin-bottom: 12px;
}

.support-message--mine {
  align-items: flex-end;
}

.support-message__bubble {
  max-width: 82%;
  padding: 9px 11px;
  border-radius: var(--radius-sm);
  background: var(--color-surface-raised);
  border: 1px solid var(--color-border);
  color: var(--color-text-heading);
  font-size: 0.9rem;
  word-break: break-word;
  white-space: pre-wrap;
}

.support-message--mine .support-message__bubble {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: #fff;
}

.support-message__time {
  margin-top: 3px;
  font-size: 0.72rem;
  color: var(--color-text-subtle);
}

.support-panel__form {
  display: flex;
  gap: 8px;
  padding: 10px;
  border-top: 1px solid var(--color-border);
  background: var(--color-surface);
}

.support-panel__form textarea {
  flex: 1;
  min-width: 0;
  resize: none;
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-sm);
  padding: 8px 10px;
  color: var(--color-text-heading);
}

.support-panel__form textarea:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px var(--color-primary-muted);
}

.support-panel__form button,
.support-fab {
  border: none;
  border-radius: var(--radius-sm);
  background: var(--color-primary);
  color: #fff;
  font-weight: 600;
}

.support-panel__form button {
  width: 60px;
}

.support-panel__form button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.support-fab {
  width: 58px;
  height: 44px;
  box-shadow: var(--shadow-md);
}

@media (max-width: 520px) {
  .support-widget {
    right: 12px;
    bottom: calc(env(safe-area-inset-bottom) + 12px);
  }

  .support-panel {
    width: calc(100vw - 24px);
    height: min(520px, calc(100dvh - 86px));
  }
}
</style>
