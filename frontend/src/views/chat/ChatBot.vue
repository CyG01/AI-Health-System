<template>
  <div class="chatbot-container">
    <!-- 浮动按钮 -->
    <div class="chatbot-float-btn" @click="openChat" v-if="!visible">
      <el-icon :size="24"><ChatDotRound /></el-icon>
      <span class="btn-text">AI咨询</span>
    </div>

    <!-- 聊天窗口 -->
    <transition name="chat-slide">
      <div v-if="visible" class="chatbot-window glass-card">
        <div class="chatbot-header">
          <div class="header-left">
            <span class="header-dot"></span>
            <span>AI健康顾问</span>
            <el-tag v-if="streaming" size="small" type="warning" effect="dark">回复中</el-tag>
          </div>
          <div class="header-right">
            <el-button text :icon="Plus" @click="handleNewSession" title="新对话" />
            <el-button text :icon="Close" @click="closeChat" title="关闭" />
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
              <el-button text size="small" type="danger" @click.stop="handleDeleteSession(s.id)">
                <el-icon :size="14"><Delete /></el-icon>
              </el-button>
            </div>
          </div>
        </transition>

        <!-- 消息列表 -->
        <div class="chatbot-messages" ref="messagesRef">
          <div v-if="messages.length === 0 && !streaming" class="welcome-tip">
            <el-icon :size="40" color="#58a6ff"><ChatDotRound /></el-icon>
            <p>你好！我是AI健康顾问</p>
            <p class="sub-tip">可以问我任何关于健康、运动、饮食的问题</p>
            <div class="quick-questions">
              <el-tag
                v-for="q in quickQuestions"
                :key="q"
                class="quick-tag"
                @click="sendQuick(q)"
              >{{ q }}</el-tag>
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
            <div class="message-bubble">
              <div class="message-text" v-html="formatContent(msg.content)"></div>
            </div>
          </div>

          <div v-if="streaming" class="message-row assistant">
            <div class="message-avatar">AI</div>
            <div class="message-bubble streaming">
              <div class="message-text">{{ streamingText }}<span class="cursor-blink">|</span></div>
            </div>
          </div>
        </div>

        <!-- 输入区 -->
        <div class="chatbot-input">
          <el-input
            v-model="inputText"
            placeholder="输入健康问题..."
            @keyup.enter="handleSend"
            :disabled="streaming"
            clearable
            resize="none"
            :autosize="{ minRows: 1, maxRows: 3 }"
            type="textarea"
          />
          <el-button
            type="primary"
            :icon="Promotion"
            :loading="streaming"
            :disabled="!inputText.trim() || streaming"
            @click="handleSend"
          >发送</el-button>
        </div>

        <!-- 医疗免责声明（始终显示） -->
        <div class="chatbot-disclaimer">
          本建议由AI生成，仅供参考，不构成医疗诊断或处方。如有健康问题，请及时咨询专业医生。
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, nextTick, watch, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ChatDotRound, Delete, Plus, Close, Promotion } from '@element-plus/icons-vue'
import { createSession, getSessionList, getMessages, deleteSession, sendMessage } from '@/api/chat'
import { sanitizeHtml } from '@/utils/sanitize'

const visible = ref(false)
const showSessionList = ref(false)
const streaming = ref(false)
const inputText = ref('')
const streamingText = ref('')
const currentSessionId = ref(null)
const sessions = ref([])
const messages = ref([])
const messagesRef = ref(null)

const quickQuestions = [
  '减脂期应该怎么吃？',
  '我适合什么运动？',
  '如何提高睡眠质量？',
  '每天需要喝多少水？',
  '运动后肌肉酸痛怎么办？'
]

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
    const res = await getSessionList()
    sessions.value = res.data || []
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
    const res = await createSession()
    currentSessionId.value = res.data.id
    messages.value = []
    showSessionList.value = false
    sessions.value.unshift(res.data)
  } catch {
    // handled by interceptor
  }
}

async function loadMessages() {
  if (!currentSessionId.value) return
  try {
    const res = await getMessages(currentSessionId.value)
    messages.value = res.data || []
    await nextTick()
    scrollToBottom()
  } catch {
    // handled by interceptor
  }
}

function selectSession(id) {
  currentSessionId.value = id
  showSessionList.value = false
  loadMessages()
}

async function handleDeleteSession(id) {
  try {
    await ElMessageBox.confirm('确定删除该对话吗？', '提示', { type: 'warning' })
    await deleteSession(id)
    sessions.value = sessions.value.filter(s => s.id !== id)
    if (currentSessionId.value === id) {
      if (sessions.value.length > 0) {
        currentSessionId.value = sessions.value[0].id
        await loadMessages()
      } else {
        await handleNewSession()
      }
    }
    ElMessage.success('已删除')
  } catch {
    // cancelled
  }
}

function sendQuick(q) {
  inputText.value = q
  handleSend()
}

function handleSend() {
  const text = inputText.value.trim()
  if (!text || streaming.value || !currentSessionId.value) return

  // 添加用户消息到列表
  messages.value.push({ role: 'user', content: text })
  inputText.value = ''
  streaming.value = true
  streamingText.value = ''

  nextTick(() => scrollToBottom())

  const currentSession = currentSessionId.value

  try {
    sendMessage({ sessionId: currentSession, content: text })
      .onMessage((delta) => {
        if (delta === '[DONE]') {
          // AI回复完成
          messages.value.push({ role: 'assistant', content: streamingText.value })
          streaming.value = false
          streamingText.value = ''
          nextTick(() => scrollToBottom())
          // 刷新会话列表以更新标题
          getSessionList().then(res => { sessions.value = res.data || [] })
        } else if (delta === '[ERROR]') {
          streaming.value = false
          streamingText.value = ''
          ElMessage.error('AI回复失败')
        } else {
          streamingText.value += delta
          nextTick(() => scrollToBottom())
        }
      })
      .onError((err) => {
        streaming.value = false
        streamingText.value = ''
        ElMessage.error(err?.message || '发送失败')
      })
  } catch {
    streaming.value = false
    streamingText.value = ''
    ElMessage.error('发送失败')
  }
}

function formatContent(text) {
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
  background: linear-gradient(135deg, #58a6ff, #7c3aed);
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #fff;
  box-shadow: 0 4px 20px rgba(88, 166, 255, 0.4);
  transition: transform 0.2s, box-shadow 0.2s;
}

.chatbot-float-btn:hover {
  transform: scale(1.08);
  box-shadow: 0 6px 28px rgba(88, 166, 255, 0.55);
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
  border-radius: 14px;
  border: 1px solid #30363d;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-slide-enter-active { transition: all 0.3s ease-out; }
.chat-slide-enter-from { opacity: 0; transform: translateY(20px) scale(0.95); }

.chatbot-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #21262d;
  background: #161b22;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #e6edf3;
}

.header-dot {
  width: 8px;
  height: 8px;
  background: #3fb950;
  border-radius: 50%;
}

.session-list {
  max-height: 200px;
  overflow-y: auto;
  border-bottom: 1px solid #21262d;
  padding: 8px;
}

.session-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  border-radius: 8px;
  cursor: pointer;
  color: #8b949e;
  font-size: 13px;
}

.session-item:hover, .session-item.active {
  background: rgba(88, 166, 255, 0.1);
  color: #e6edf3;
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
}

.welcome-tip {
  text-align: center;
  color: #8b949e;
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
  background: rgba(88, 166, 255, 0.1) !important;
  border-color: rgba(88, 166, 255, 0.25) !important;
  color: #58a6ff !important;
  font-size: 12px !important;
}

.quick-tag:hover {
  background: rgba(88, 166, 255, 0.2) !important;
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
  background: linear-gradient(135deg, #7c3aed, #58a6ff);
}

.assistant .message-avatar {
  background: linear-gradient(135deg, #3fb950, #58a6ff);
}

.message-bubble {
  background: #21262d;
  border-radius: 12px;
  padding: 10px 14px;
  border: 1px solid #30363d;
}

.message-row.user .message-bubble {
  background: linear-gradient(135deg, rgba(124, 58, 237, 0.2), rgba(88, 166, 255, 0.15));
  border-color: rgba(88, 166, 255, 0.3);
}

.message-row.assistant .message-bubble {
  background: #161b22;
  border-color: #30363d;
}

.message-bubble.streaming {
  border-color: rgba(88, 166, 255, 0.4);
}

.message-text {
  color: #e6edf3;
  font-size: 13px;
  line-height: 1.6;
  word-break: break-word;
}

.cursor-blink {
  animation: blink 1s infinite;
  color: #58a6ff;
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

.chatbot-input {
  padding: 12px 14px;
  border-top: 1px solid #21262d;
  background: #161b22;
  display: flex;
  gap: 10px;
  align-items: flex-end;
}

.chatbot-input :deep(.el-textarea__inner) {
  background: #0d1117;
  border-color: #30363d;
  color: #e6edf3;
  font-size: 13px;
}

.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

/* 医疗免责声明 */
.chatbot-disclaimer {
  padding: 8px 14px;
  background: rgba(210, 153, 34, 0.08);
  border-top: 1px solid rgba(210, 153, 34, 0.25);
  font-size: 11px;
  color: #8b949e;
  text-align: center;
  line-height: 1.5;
}
</style>