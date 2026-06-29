<template>
  <div class="chatbot-container">
    <!-- 浮动按钮 -->
    <div class="chatbot-float-btn" @click="openChat" v-if="!visible">
      <NIcon :size="24"><ChatbubbleEllipsesOutline /></NIcon>
      <span class="btn-text">{{ $t('chat.consult') || 'AI咨询' }}</span>
    </div>

    <!-- 聊天窗口 -->
    <transition name="chat-slide">
      <div v-if="visible" class="chatbot-window glass-card">
        <div class="chatbot-header">
          <div class="header-left">
            <span class="header-dot"></span>
            <span>{{ $t('chat.advisor') || 'AI健康顾问' }}</span>
            <NTag v-if="streaming" size="small" type="warning" :bordered="false">{{ $t('chat.replying') || '回复中' }}</NTag>
          </div>
          <div class="header-right">
            <NButton text @click="handleNewSession" :title="$t('chat.newSession') || '新对话'">
              <template #icon><NIcon><AddOutline /></NIcon></template>
            </NButton>
            <NButton text @click="closeChat" :title="$t('common.close') || '关闭'">
              <template #icon><NIcon><CloseOutline /></NIcon></template>
            </NButton>
          </div>
        </div>

        <!-- 会话列表 -->
        <transition name="fade">
          <div v-if="showSessionList" class="session-list">
            <div
              v-for="s in sessions"
              :key="s.id"
              class="session-item"
              :class="{ active: s.id === currentSessionId }"
              @click="selectSession(s.id)"
            >
              <span class="session-title">{{ s.title }}</span>
              <NButton text size="small" type="error" @click.stop="handleDeleteSession(s.id)">
                <template #icon><NIcon :size="14"><TrashOutline /></NIcon></template>
              </NButton>
            </div>
          </div>
        </transition>

        <!-- 消息列表 -->
        <div class="chatbot-messages" ref="messagesRef">
          <div v-if="messages.length === 0 && !streaming" class="welcome-tip">
            <NIcon :size="40" color="#4ECDC4"><ChatbubbleEllipsesOutline /></NIcon>
            <p>你好！我是AI健康顾问</p>
            <p class="sub-tip">可以问我任何关于健康、运动、饮食的问题</p>
            <div class="quick-questions">
              <NTag
                v-for="q in quickQuestions"
                :key="q"
                class="quick-tag"
                @click="sendQuick(q)"
                round
                size="small"
              >{{ q }}</NTag>
            </div>
          </div>

          <div
            v-for="(msg, idx) in messages"
            :key="idx"
            class="message-row"
            :class="msg.role"
          >
            <div class="message-avatar">
              {{ msg.role === 'user' ? '我' : 'AI' }}
            </div>
            <div class="message-bubble-wrapper">
              <div class="message-bubble">
                <div class="message-text" v-html="formatContent(msg.content)"></div>
              </div>
              <!-- AI 回复操作按钮 -->
              <div v-if="msg.role === 'assistant' && msg.content" class="message-actions">
                <NButton text size="small" @click="handleRegenerate(msg)" title="重新生成">
                  <template #icon><NIcon><RefreshOutline /></NIcon></template>
                  重新生成
                </NButton>
                <NButton text size="small" @click="handleFeedback(msg, 'useful')" title="有用">
                  <template #icon>
                    <NIcon :size="14" :color="msg.feedback === 'useful' ? '#C7F464' : ''"><CheckmarkCircleOutline /></NIcon>
                  </template>
                </NButton>
                <NButton text size="small" @click="handleFeedback(msg, 'useless')" title="没用">
                  <template #icon>
                    <NIcon :size="14" :color="msg.feedback === 'useless' ? '#FF6B6B' : ''"><CloseCircleOutline /></NIcon>
                  </template>
                </NButton>
                <NButton text size="small" @click="handleFeedback(msg, 'incorrect')" title="有误">
                  <template #icon>
                    <NIcon :size="14" :color="msg.feedback === 'incorrect' ? '#FFB800' : ''"><WarningOutline /></NIcon>
                  </template>
                </NButton>
              </div>
            </div>
          </div>

          <div v-if="streaming" class="message-row assistant">
            <div class="message-avatar">AI</div>
            <div class="message-bubble-wrapper">
              <div class="message-bubble streaming">
                <div class="message-text">{{ streamingText }}<span class="cursor-blink">|</span></div>
              </div>
              <div class="streaming-progress">
                <span class="progress-dot" />
                <span class="progress-text">AI 正在生成回复... {{ progressChars }} 字</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 输入区 -->
        <div class="chatbot-input">
          <div class="input-options">
            <NCheckbox
              v-model:checked="includeHealthContext"
              :disabled="streaming || healthContextLoading"
              size="small"
            >
              <span class="context-label">
                {{ healthContextLoading ? '加载健康档案中...' : '包含健康档案上下文' }}
              </span>
            </NCheckbox>
          </div>
          <div class="input-row">
            <NInput
              v-model:value="inputText"
              :placeholder="$t('chat.inputPlaceholder') || '输入健康问题...'"
              @keyup.enter="handleSend"
              :disabled="streaming || healthContextLoading"
              clearable
              :autosize="{ minRows: 1, maxRows: 3 }"
              type="textarea"
            />
            <NButton
              type="primary"
              :loading="streaming || healthContextLoading"
              :disabled="!inputText.trim() || streaming || healthContextLoading"
              @click="handleSend"
            >
              <template #icon><NIcon><SendOutline /></NIcon></template>
              {{ streaming ? ($t('chat.generating') || '生成中...') : ($t('chat.send') || '发送') }}
            </NButton>
          </div>
        </div>

        <!-- 医疗免责声明（始终显示） -->
        <div class="chatbot-disclaimer">
          本建议由AI生成，仅供参考，不构成医疗诊断或处方。如有健康问题，请及时咨询专业医生。
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onBeforeUnmount } from 'vue'
import {
  NButton, NIcon, NTag, NInput, NCheckbox,
  useMessage, useDialog
} from 'naive-ui'
import {
  ChatbubbleEllipsesOutline, TrashOutline, AddOutline, CloseOutline,
  SendOutline, CheckmarkCircleOutline, CloseCircleOutline,
  WarningOutline, RefreshOutline
} from '@vicons/ionicons5'
import { fetchCreateSession, fetchGetSessionList, fetchGetMessages, fetchDeleteSession, fetchGetLatestHealth } from '@/service/api'
import { SEND_WITH_CONTEXT_URL } from '@/service/api/chat'
import { createSSEStream } from '@/utils/sseClient'
import { sanitizeHtml } from '@/utils/sanitize'
import { isOnline, getCachedChatMessages, cacheChatMessages } from '@/utils/offlineCache'

interface ChatMessage {
  id?: number
  role: 'user' | 'assistant' | 'system'
  content: string
  createTime?: string
  feedback?: 'useful' | 'useless' | 'incorrect' | null
}

type Session = Api.Chat.Session;

interface SseAbortController {
  abort: () => void
}

interface SseCallbacks {
  onMessage: (delta: string) => void
  onProgress: (chars: number) => void
  onDone: () => void
  onError: (err: { message?: string } | null) => void
  onResume: (cursor: number) => void
}

defineOptions({ name: 'ChatBotPage' })

const message = useMessage()
const dialog = useDialog()

const visible = ref(false)
const showSessionList = ref(false)
const streaming = ref(false)
const inputText = ref('')
const streamingText = ref('')
const progressChars = ref(0)
let currentSseAbort: SseAbortController | null = null
const currentSessionId = ref<string | number | null>(null)
const sessions = ref<Session[]>([])
const messages = ref<ChatMessage[]>([])
const messagesRef = ref<HTMLElement | null>(null)

const quickQuestions = [
  '减脂期应该怎么吃？',
  '我适合什么运动？',
  '如何提高睡眠质量？',
  '每天需要喝多少水？',
  '运动后肌肉酸痛怎么办？'
]

/** Whether to include health profile context when sending messages */
const includeHealthContext = ref(false)
/** Loading state for fetching health profile before sending */
const healthContextLoading = ref(false)

function openChat() {
  visible.value = true
  if (!currentSessionId.value) {
    initChat()
  }
}

function closeChat() {
  visible.value = false
}

async function initChat() {
  try {
    const { data } = await fetchGetSessionList()
    sessions.value = data || []
    if (sessions.value.length > 0) {
      currentSessionId.value = sessions.value[0].id
      await loadMessages()
    } else {
      await handleNewSession()
    }
  } catch {
    // handled by interceptor
  }
}

async function handleNewSession() {
  try {
    const { data } = await fetchCreateSession()
    currentSessionId.value = data.id
    messages.value = []
    showSessionList.value = false
    sessions.value.unshift(data)
  } catch {
    // handled by interceptor
  }
}

async function loadMessages() {
  if (!currentSessionId.value) return
  try {
    const { data } = await fetchGetMessages(currentSessionId.value as string)
    messages.value = data || []
    await nextTick()
    scrollToBottom()
  } catch {
    // handled by interceptor
  }
}

function selectSession(id: string | number) {
  currentSessionId.value = id
  showSessionList.value = false
  loadMessages()
}

async function handleDeleteSession(id: string | number) {
  dialog.warning({
    title: '提示',
    content: '确定删除该对话吗？',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await fetchDeleteSession(id as string)
        sessions.value = sessions.value.filter(s => s.id !== id)
        if (currentSessionId.value === id) {
          if (sessions.value.length > 0) {
            currentSessionId.value = sessions.value[0].id
            await loadMessages()
          } else {
            await handleNewSession()
          }
        }
        message.success('已删除')
      } catch {
        // cancelled
      }
    }
  })
}

function sendQuick(q: string) {
  inputText.value = q
  handleSend()
}

async function handleSend() {
  const text = inputText.value.trim()
  if (!text || streaming.value || !currentSessionId.value) return

  // 取消上一次未完成的请求
  if (currentSseAbort) {
    currentSseAbort.abort()
    currentSseAbort = null
  }

  // 添加用户消息到列表并缓存
  messages.value.push({ role: 'user', content: text })
  cacheChatMessages(currentSessionId.value as string, messages.value)
  inputText.value = ''
  streaming.value = true
  streamingText.value = ''
  progressChars.value = 0

  nextTick(() => scrollToBottom())

  const currentSession = currentSessionId.value

  // Determine endpoint and request body based on context toggle
  let endpoint = '/chat/send'
  const requestBody: Record<string, any> = { sessionId: currentSession, content: text }

  if (includeHealthContext.value) {
    healthContextLoading.value = true
    try {
      const { data: healthData, error } = await fetchGetLatestHealth()
      if (!error && healthData) {
        endpoint = SEND_WITH_CONTEXT_URL
        requestBody.context = {
          page: 'health',
          entityId: null,
          healthData
        }
      }
    } catch {
      // Failed to fetch health data — fall back to plain /chat/send
      console.warn('[ChatBot] Failed to fetch health profile, sending without context')
    } finally {
      healthContextLoading.value = false
    }
  }

  const stream = createSSEStream(
    endpoint,
    requestBody,
    {
      onMessage: (delta: string) => {
        if (delta === '[DONE]') {
          // AI回复完成
          messages.value.push({ role: 'assistant', content: streamingText.value })
          streaming.value = false
          streamingText.value = ''
          progressChars.value = 0
          // 缓存完整的消息列表
          cacheChatMessages(currentSessionId.value as string, messages.value)
          nextTick(() => scrollToBottom())
          // 刷新会话列表以更新标题
          fetchGetSessionList().then(({ data }) => { sessions.value = data || [] })
        } else if (delta === '[ERROR]') {
          streaming.value = false
          streamingText.value = ''
          progressChars.value = 0
          message.error('AI回复失败')
        } else {
          streamingText.value += delta
          progressChars.value = (progressChars.value || 0) + (delta ? delta.length : 0)
          nextTick(() => scrollToBottom())
        }
      },
      onProgress: (chars: number) => {
        progressChars.value = chars
      },
      onDone: () => {
        // done via [DONE] message
      },
      onError: (err: { message?: string } | null) => {
        streaming.value = false
        streamingText.value = ''
        progressChars.value = 0
        message.error(err?.message || '发送失败')
      },
      onResume: (cursor: number) => {
        message.info(`连接恢复中...已恢复 ${cursor} 字`)
      }
    }
  )

  currentSseAbort = stream
}

function handleRegenerate(msg: ChatMessage) {
  if (streaming.value) return
  // 找到该AI消息之前最后一条用户消息
  const idx = messages.value.indexOf(msg)
  if (idx <= 0) return
  // 找到此AI回复对应的用户问题
  let userMsg: ChatMessage | null = null
  for (let i = idx - 1; i >= 0; i--) {
    if (messages.value[i].role === 'user') {
      userMsg = messages.value[i]
      break
    }
  }
  if (!userMsg) return
  // 移除当前AI回复
  messages.value.splice(idx, 1)
  // 重新发送用户消息
  inputText.value = userMsg.content
  handleSend()
}

function handleFeedback(msg: ChatMessage, type: 'useful' | 'useless' | 'incorrect') {
  // 切换反馈状态
  if (msg.feedback === type) {
    msg.feedback = null
  } else {
    msg.feedback = type
  }
  // 可扩展：发送反馈到后端
  // request({ url: '/chat/feedback', method: 'post', data: { sessionId, messageIndex, feedback: type } })
  message.success(type === 'useful' ? '感谢反馈！' : '已记录反馈')
}

function formatContent(text: string | undefined): string {
  if (!text) return ''
  return sanitizeHtml(text
    .replace(/\*\*(.*?)\*\*/g, '<b>$1</b>')
    .replace(/\n/g, '<br>'))
}

function scrollToBottom() {
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}

onBeforeUnmount(() => {
  if (currentSseAbort) {
    currentSseAbort.abort()
    currentSseAbort = null
  }
})
</script>

<style scoped>
.chatbot-container {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 1000;
}

.chatbot-float-btn {
  width: 64px;
  height: 64px;
  background: var(--gradient-warm);
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #fff;
  box-shadow: 0 4px 20px rgba(255, 107, 53, 0.4);
  transition: transform 0.2s, box-shadow 0.2s;
}

.chatbot-float-btn:hover {
  transform: scale(1.08);
  box-shadow: 0 6px 28px rgba(255, 107, 53, 0.55);
}

.btn-text {
  font-size: 10px;
  margin-top: 1px;
}

.chatbot-window {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 400px;
  height: 580px;
  border-radius: var(--radius-base);
  border: 1px solid var(--sport-border);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--sport-bg-surface);
}

.chat-slide-enter-active { transition: all 0.3s ease-out; }
.chat-slide-enter-from { opacity: 0; transform: translateY(20px) scale(0.95); }

.chatbot-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid var(--sport-border-subtle);
  background: var(--sport-bg-elevated);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: var(--sport-text-primary);
}

.header-dot {
  width: 8px;
  height: 8px;
  background: var(--sport-primary);
  border-radius: 50%;
}

.session-list {
  max-height: 200px;
  overflow-y: auto;
  border-bottom: 1px solid var(--sport-border-subtle);
  padding: 8px;
}

.session-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  color: var(--sport-text-secondary);
  font-size: 13px;
}

.session-item:hover, .session-item.active {
  background: var(--sport-primary-subtle);
  color: var(--sport-text-primary);
}

.session-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.chatbot-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  background: var(--sport-bg-surface);
}

.welcome-tip {
  text-align: center;
  color: var(--sport-text-secondary);
  margin-top: 60px;
}

.welcome-tip p { margin: 8px 0; font-size: 15px; }
.sub-tip { font-size: 13px !important; opacity: 0.7; }

.quick-questions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
  margin-top: 16px;
}

.quick-tag {
  cursor: pointer;
  background: var(--sport-primary-subtle) !important;
  border-color: rgba(255, 107, 53, 0.25) !important;
  color: var(--sport-primary) !important;
  font-size: 12px !important;
}

.quick-tag:hover {
  background: rgba(255, 107, 53, 0.2) !important;
}

.message-row {
  display: flex;
  gap: 10px;
  max-width: 90%;
}

.message-row.user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.message-row.assistant {
  align-self: flex-start;
}

.message-avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
  flex-shrink: 0;
  color: #fff;
}

.user .message-avatar {
  background: var(--gradient-warm);
}

.assistant .message-avatar {
  background: var(--gradient-cool);
}

.message-bubble {
  background: var(--sport-bg-elevated);
  border-radius: var(--radius-sm);
  padding: 10px 14px;
  border: 1px solid var(--sport-border);
}

.message-row.user .message-bubble {
  background: var(--gradient-warm);
  border-color: rgba(255, 107, 53, 0.3);
  color: #fff;
}

.message-row.user .message-bubble .message-text {
  color: #fff;
}

.message-row.assistant .message-bubble {
  background: var(--sport-bg-elevated);
  border-color: var(--sport-border);
}

.message-bubble.streaming {
  border-color: rgba(78, 205, 196, 0.4);
}

.message-text {
  color: var(--sport-text-primary);
  font-size: 13px;
  line-height: 1.6;
  word-break: break-word;
}

.cursor-blink {
  animation: blink 1s infinite;
  color: var(--sport-secondary);
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

/* 消息操作按钮 */
.message-bubble-wrapper {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.message-actions {
  display: flex;
  gap: 2px;
  padding-left: 4px;
}

/* 流式进度提示 */
.streaming-progress {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 8px;
  font-size: 12px;
  color: var(--sport-text-secondary);
}

.progress-dot {
  width: 6px;
  height: 6px;
  background: var(--sport-secondary);
  border-radius: 50%;
  animation: pulse 1.5s infinite;
}

.progress-text {
  font-size: 12px;
  color: var(--sport-text-secondary);
}

@keyframes pulse {
  0%, 100% { opacity: 0.3; transform: scale(0.8); }
  50% { opacity: 1; transform: scale(1); }
}

.chatbot-input {
  padding: 12px 14px;
  border-top: 1px solid var(--sport-border-subtle);
  background: var(--sport-bg-elevated);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.input-options {
  display: flex;
  align-items: center;
}

.context-label {
  font-size: 12px;
  color: var(--sport-text-secondary);
}

.input-row {
  display: flex;
  gap: 10px;
  align-items: flex-end;
}

.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

/* 医疗免责声明 */
.chatbot-disclaimer {
  padding: 8px 14px;
  background: rgba(255, 184, 0, 0.08);
  border-top: 1px solid rgba(255, 184, 0, 0.25);
  font-size: 11px;
  color: var(--sport-text-secondary);
  text-align: center;
  line-height: 1.5;
}
</style>
