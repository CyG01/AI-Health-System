<template>
  <div class="copilot-wrapper">
    <!-- 悬浮 AI 按钮 -->
    <div v-if="!appStore.copilotOpen" class="copilot-fab" @click="handleOpen">
      <div class="fab-inner">
        <svg class="fab-icon" viewBox="0 0 24 24" fill="currentColor" width="24" height="24"><path d="M7.5 5.6L10 7 8.6 4.5 10 2 7.5 3.4 5 2l1.4 2.5L5 7zm12 9.8L17 14l1.4 2.5L17 19l2.5-1.4L22 19l-1.4-2.5L22 14zM22 2l-2.5 1.4L17 2l1.4 2.5L17 7l2.5-1.4L22 7l-1.4-2.5zm-7.63 5.29a.996.996 0 0 0-1.41 0L1.29 18.96c-.39.39-.39 1.02 0 1.41l2.34 2.34c.39.39 1.02.39 1.41 0L16.7 11.05c.39-.39.39-1.02 0-1.41l-2.33-2.35z"/></svg>
        <span class="fab-label">AI助手</span>
      </div>
      <div class="fab-pulse" />
    </div>

    <!-- 底部抽屉 -->
    <transition name="drawer-slide">
      <div v-if="appStore.copilotOpen" class="copilot-drawer">
        <!-- 头部 -->
        <div class="drawer-header">
          <div class="handle-bar" />
          <div class="header-content">
            <div class="header-left">
              <span class="header-dot" :class="{ active: !streaming }"></span>
              <span class="header-title">AI 智能助手</span>
              <NTag v-if="streaming" size="small" type="warning">回复中</NTag>
            </div>
            <div class="header-right">
              <div v-if="contextInfo" class="context-badge">
                <NTag size="small" type="info">{{ contextInfo.label }}</NTag>
                <NTag v-if="contextInfo.entityName" size="small" type="warning">{{ contextInfo.entityName }}</NTag>
              </div>
              <NButton text @click="handleNewSession" title="新对话">
                <template #icon><svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16"><path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/></svg></template>
              </NButton>
              <NButton text @click="handleClose" title="关闭">
                <template #icon><svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16"><path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/></svg></template>
              </NButton>
            </div>
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
              <NButton text size="tiny" type="error" @click.stop="handleDeleteSession(s.id)">
                <template #icon><svg viewBox="0 0 24 24" fill="currentColor" width="14" height="14"><path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"/></svg></template>
              </NButton>
            </div>
          </div>
        </transition>

        <!-- 快速操作建议 -->
        <div v-if="quickActions.length > 0 && messages.length === 0" class="quick-actions">
          <NButton
            v-for="action in quickActions"
            :key="action.label"
            size="small"
            secondary
            @click="handleQuickAction(action)"
          >
            {{ action.label }}
          </NButton>
        </div>

        <!-- 消息区 -->
        <div class="drawer-messages" ref="messagesRef">
          <div v-if="messages.length === 0 && !streaming" class="welcome-tip">
            <svg viewBox="0 0 24 24" fill="currentColor" width="36" height="36" style="color: #58a6ff"><path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H5.17L4 17.17V4h16v12zM7 9h2v2H7zm4 0h2v2h-2zm4 0h2v2h-2z"/></svg>
            <p>你好！我是AI智能助手</p>
            <p class="sub-tip">{{ contextInfo ? `当前页面：${contextInfo.label}` : '可以问我任何关于健康、运动、饮食的问题' }}</p>
            <div v-if="!contextInfo" class="quick-questions">
              <NTag
                v-for="q in defaultQuestions"
                :key="q"
                class="quick-tag"
                :bordered="false"
                @click="sendQuick(q)"
                style="cursor: pointer"
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

              <!-- SDUI plan_card 固化按钮 -->
              <ErrorBoundary
                v-if="msg.sdui?.type === 'plan_card'"
                fallbackTitle="计划卡片渲染异常"
                fallbackMessage="AI 返回的计划数据格式有误"
              >
                <div class="sdui-plan-card">
                  <div class="sdui-plan-header">
                    <h4>{{ msg.sdui.planName }}</h4>
                    <NButton size="small" type="primary" @click="handleApplyPlan(msg.sdui!)">
                      固化到我的计划
                    </NButton>
                  </div>
                  <div class="sdui-plan-preview">
                    <span>{{ msg.sdui.durationDays }}天 · {{ msg.sdui.planType }}</span>
                    <span>{{ msg.sdui.totalExercises }}个动作</span>
                  </div>
                </div>
              </ErrorBoundary>

              <!-- AI 回复操作按钮 -->
              <div v-if="msg.role === 'assistant' && msg.content && !msg.sdui" class="message-actions">
                <NButton text size="small" @click="handleRegenerate(msg)" title="重新生成">重新生成</NButton>
                <NButton text size="small" @click="handleFeedback(msg, 'useful')" title="有用">
                  <template #icon><svg viewBox="0 0 24 24" fill="currentColor" width="14" height="14" :style="{ color: msg.feedback === 'useful' ? '#3fb950' : '' }"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/></svg></template>
                </NButton>
                <NButton text size="small" @click="handleFeedback(msg, 'useless')" title="没用">
                  <template #icon><svg viewBox="0 0 24 24" fill="currentColor" width="14" height="14" :style="{ color: msg.feedback === 'useless' ? '#f85149' : '' }"><path d="M12 2C6.47 2 2 6.47 2 12s4.47 10 10 10 10-4.47 10-10S17.53 2 12 2zm5 13.59L15.59 17 12 13.41 8.41 17 7 15.59 10.59 12 7 8.41 8.41 7 12 10.59 15.59 7 17 8.41 13.41 12 17 15.59z"/></svg></template>
                </NButton>
              </div>
            </div>
          </div>

          <!-- 流式生成中 -->
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

          <!-- SSE 超时提示 -->
          <div v-if="sseTimedOut" class="message-row assistant">
            <div class="message-avatar">AI</div>
            <div class="message-bubble-wrapper">
              <NAlert type="warning" title="AI 响应超时">
                <p>可能原因：网络波动或服务繁忙</p>
                <NButton
                  size="small"
                  type="primary"
                  secondary
                  :disabled="sseRetryCount >= MAX_RETRY"
                  @click="handleRetrySend"
                  style="margin-top: 8px"
                >
                  重试{{ sseRetryCount > 0 ? ` (${sseRetryCount}/${MAX_RETRY})` : '' }}
                </NButton>
              </NAlert>
            </div>
          </div>
        </div>

        <!-- 输入区 -->
        <div class="drawer-input">
          <NInput
            v-model:value="inputText"
            :placeholder="inputPlaceholder"
            @keyup.enter="handleSend"
            :disabled="streaming"
            clearable
            type="textarea"
            :autosize="{ minRows: 1, maxRows: 3 }"
          />
          <NButton
            class="voice-btn"
            :class="{ recording }"
            circle
            @mousedown="startVoice"
            @mouseup="stopVoice"
            @mouseleave="stopVoice"
            title="按住说话"
          >
            <template #icon><svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16"><path d="M12 14c1.66 0 3-1.34 3-3V5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3zm-1-9c0-.55.45-1 1-1s1 .45 1 1v6c0 .55-.45 1-1 1s-1-.45-1-1V5zm6 6c0 2.76-2.24 5-5 5s-5-2.24-5-5H5c0 3.53 2.61 6.43 6 6.92V21h2v-3.08c3.39-.49 6-3.39 6-6.92h-2z"/></svg></template>
          </NButton>
          <NButton
            type="primary"
            :loading="streaming"
            :disabled="!inputText.trim() || streaming"
            @click="handleSend"
          >{{ streaming ? '' : '发送' }}</NButton>
        </div>

        <!-- 医疗免责声明 -->
        <div class="drawer-disclaimer">
          本建议由AI生成，仅供参考，不构成医疗诊断或处方。如有健康问题，请及时咨询专业医生。
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, onBeforeUnmount } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { NButton, NTag, NInput, NAlert, useMessage, useDialog } from 'naive-ui';
import ErrorBoundary from '@/components/ErrorBoundary.vue';
import { sanitizeHtml } from '@/utils/sanitize';
import { cacheChatMessages } from '@/utils/offlineCache';
import { createSSEStream } from '@/utils/sseClient';
import { localStg } from '@/utils/storage';
import { fetchCreateSession, fetchGetSessionList, fetchGetMessages, fetchDeleteSession } from '@/service/api';
import { fetchSolidifyPlan } from '@/service/api';

defineOptions({ name: 'GlobalCopilotDrawer' });

// Types
interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
  sdui?: SduiPlanCard;
  feedback?: 'useful' | 'useless' | null;
}

interface SduiPlanCard {
  type: 'plan_card';
  planName: string;
  planId?: number;
  version?: number;
  durationDays: number;
  planType: string;
  totalExercises: number;
}

interface ToolCall {
  action: string;
  dayIndex?: number;
  itemIndex?: number;
  newItem?: any;
  newItems?: any[];
  plan?: any;
  days?: any[];
}

interface ChatSession {
  id: string;
  title: string;
  [k: string]: any;
}

interface QuickAction {
  label: string;
  action: string;
}

interface ContextInfo {
  page: string;
  label: string;
  icon: string;
  entityName: string | null;
}

interface SSEStreamControl {
  promise: Promise<void>;
  abort: () => void;
  readonly receivedChars: number;
}

/** SSE 无响应超时时间（毫秒） */
const SSE_TIMEOUT_MS = 15_000;
/** 最大重试次数 */
const MAX_RETRY = 2;

const route = useRoute();
const router = useRouter();
const message = useMessage();
const dialog = useDialog();

// Stores - will be imported from new store paths
// For now, use reactive state as placeholders
const appStore = {
  copilotOpen: ref(false),
  copilotContext: ref<any>(null),
  openCopilot: (ctx?: any) => { appStore.copilotOpen.value = true; appStore.copilotContext.value = ctx; },
  closeCopilot: () => { appStore.copilotOpen.value = false; },
  emit: (event: string, data?: any) => { console.log('[EventBus]', event, data); }
};

const planStore = {
  currentPlan: ref<any>(null),
  currentPlanDays: ref<any[]>([]),
  setPlan: (plan: any, days: any[]) => { planStore.currentPlan.value = plan; planStore.currentPlanDays.value = days; },
  updateDayItem: (dayIndex: number, itemIndex: number, newItem: any) => { /* update logic */ },
  replaceDayItems: (dayIndex: number, newItems: any[]) => { /* replace logic */ }
};

// === State ===
const showSessionList = ref(false);
const streaming = ref(false);
const inputText = ref('');
const streamingText = ref('');
const progressChars = ref(0);
const recording = ref(false);
let currentSseAbort: SSEStreamControl | null = null;
const currentSessionId = ref<string | null>(null);
const sessions = ref<ChatSession[]>([]);
const messages = ref<ChatMessage[]>([]);
const messagesRef = ref<HTMLElement | null>(null);
const sseTimedOut = ref(false);
const sseRetryCount = ref(0);
let sseTimeoutTimer: ReturnType<typeof setTimeout> | null = null;
let lastRequestData: any = null;

// === Context awareness ===
const contextInfo = computed<ContextInfo | null>(() => {
  const ctx = appStore.copilotContext.value;
  if (ctx) return resolveContextDisplay(ctx);
  const path = route.path;
  if (path.startsWith('/plan/') && path !== '/plan/list' && path !== '/plan/generate') {
    return { page: 'planDetail', label: '计划详情', icon: 'DataLine', entityName: null };
  }
  if (path === '/food/record') return { page: 'foodRecord', label: '饮食记录', icon: 'Dish', entityName: null };
  if (path === '/checkin/calendar') return { page: 'calendar', label: '打卡日历', icon: 'Calendar', entityName: null };
  if (path === '/dashboard') return { page: 'dashboard', label: '健康看板', icon: 'DataLine', entityName: null };
  return null;
});

function resolveContextDisplay(ctx: any): ContextInfo {
  const displays: Record<string, { label: string; icon: string }> = {
    planDetail: { label: '计划详情', icon: 'DataLine' },
    foodRecord: { label: '饮食记录', icon: 'Dish' },
    calendar: { label: '打卡日历', icon: 'Calendar' },
    dashboard: { label: '健康看板', icon: 'DataLine' }
  };
  const d = displays[ctx.page] || { label: ctx.page, icon: 'User' };
  return { ...d, page: ctx.page, entityName: ctx.entityName || null };
}

// === Quick actions ===
const quickActions = computed<QuickAction[]>(() => {
  const ctx = contextInfo.value;
  if (!ctx) return [];
  switch (ctx.page) {
    case 'planDetail':
      return [
        { label: '帮我调整今天的训练', action: 'adjust_today' },
        { label: '把某个动作换掉', action: 'replace_item' },
        { label: '降低训练强度', action: 'reduce_intensity' },
        { label: '分析我的完成度', action: 'analyze_progress' }
      ];
    case 'foodRecord':
      return [
        { label: '帮我记录午餐', action: 'record_lunch' },
        { label: '推荐今天吃什么', action: 'recommend_food' },
        { label: '分析今日营养摄入', action: 'analyze_nutrition' }
      ];
    case 'dashboard':
      return [
        { label: '总结我的健康数据', action: 'summarize_data' },
        { label: '生成新的运动计划', action: 'generate_plan' }
      ];
    default:
      return [];
  }
});

const defaultQuestions = [
  '减脂期应该怎么吃？',
  '我适合什么运动？',
  '如何提高睡眠质量？',
  '每天需要喝多少水？'
];

const inputPlaceholder = computed(() => {
  const ctx = contextInfo.value;
  if (!ctx) return '描述你想做什么，如：如何提高睡眠质量...';
  switch (ctx.page) {
    case 'planDetail': return '描述你想调整的内容，如：把深蹲换成臀桥...';
    case 'foodRecord': return '一句话记录饮食，如：中午吃了一碗牛肉面...';
    default: return '描述你想做什么...';
  }
});

// === Copilot control ===
function handleOpen() {
  appStore.openCopilot(appStore.copilotContext.value);
  if (!currentSessionId.value) initChat();
}

function handleClose() {
  appStore.closeCopilot();
}

// === Session management ===
async function initChat() {
  try {
    const { data, error } = await fetchGetSessionList();
    if (error) return;
    sessions.value = (data as any[]) || [];
    if (sessions.value.length > 0) {
      currentSessionId.value = sessions.value[0].id;
      await loadMessages();
    } else {
      await handleNewSession();
    }
  } catch {
    // handled by interceptor
  }
}

async function handleNewSession() {
  try {
    const { data, error } = await fetchCreateSession();
    if (error || !data) return;
    currentSessionId.value = (data as any).id;
    messages.value = [];
    showSessionList.value = false;
    sessions.value.unshift(data as any);
  } catch {
    // handled by interceptor
  }
}

async function loadMessages() {
  if (!currentSessionId.value) return;
  try {
    const { data, error } = await fetchGetMessages(currentSessionId.value);
    if (error) return;
    messages.value = (data as any[]) || [];
    await nextTick();
    scrollToBottom();
  } catch {
    // handled by interceptor
  }
}

function selectSession(id: string) {
  currentSessionId.value = id;
  showSessionList.value = false;
  loadMessages();
}

async function handleDeleteSession(id: string) {
  dialog.warning({
    title: '提示',
    content: '确定删除该对话吗？',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await fetchDeleteSession(id);
        sessions.value = sessions.value.filter(s => s.id !== id);
        if (currentSessionId.value === id) {
          if (sessions.value.length > 0) {
            currentSessionId.value = sessions.value[0].id;
            await loadMessages();
          } else {
            await handleNewSession();
          }
        }
        message.success('已删除');
      } catch {
        // ignore
      }
    }
  });
}

// === Message sending ===
function sendQuick(q: string) {
  inputText.value = q;
  handleSend();
}

function handleQuickAction(action: QuickAction) {
  const actionPrompts: Record<string, string> = {
    adjust_today: '帮我根据今天的状态调整训练计划',
    replace_item: '我想把计划中的某个动作换成其他动作',
    reduce_intensity: '帮我降低今天训练的强度',
    analyze_progress: '分析一下我最近的计划完成情况',
    record_lunch: '帮我记录午餐',
    recommend_food: '根据我的健康数据推荐今天吃什么',
    analyze_nutrition: '分析一下我今天的营养摄入情况',
    summarize_data: '帮我总结一下最近的健康数据趋势',
    generate_plan: '帮我生成一个新的运动计划'
  };
  inputText.value = actionPrompts[action.action] || action.label;
  handleSend();
}

function handleSend() {
  const text = inputText.value.trim();
  if (!text || streaming.value || !currentSessionId.value) return;

  if (currentSseAbort) {
    currentSseAbort.abort();
    currentSseAbort = null;
  }

  messages.value.push({ role: 'user', content: text });
  cacheChatMessages(currentSessionId.value, messages.value as any[]);
  inputText.value = '';
  streaming.value = true;
  streamingText.value = '';
  progressChars.value = 0;
  sseTimedOut.value = false;
  sseRetryCount.value = 0;

  nextTick(() => scrollToBottom());

  const ctx = contextInfo.value;
  const requestData = {
    sessionId: currentSessionId.value,
    content: text,
    context: ctx ? {
      page: ctx.page,
      entityId: appStore.copilotContext.value?.entityId || null
    } : null
  };
  lastRequestData = requestData;
  startSseStream(requestData);
}

function startSseStream(requestData: any) {
  clearSseTimeout();
  resetSseTimeout();

  const stream = createSSEStream('/chat/send', requestData, {
    onMessage: (delta: string) => {
      resetSseTimeout();

      if (delta === '[DONE]') {
        clearSseTimeout();
        const toolCall = tryParseToolCall(streamingText.value);
        const sdui = tryParseSdui(streamingText.value);

        const assistantMsg: ChatMessage = {
          role: 'assistant',
          content: streamingText.value,
          sdui: sdui || undefined
        };
        messages.value.push(assistantMsg);

        if (toolCall) executeToolCall(toolCall);

        streaming.value = false;
        streamingText.value = '';
        progressChars.value = 0;
        sseTimedOut.value = false;
        sseRetryCount.value = 0;
        if (currentSessionId.value) {
          cacheChatMessages(currentSessionId.value, messages.value as any[]);
        }
        nextTick(() => scrollToBottom());
      } else if (delta === '[ERROR]') {
        clearSseTimeout();
        streaming.value = false;
        streamingText.value = '';
        progressChars.value = 0;
        message.error('AI回复失败');
      } else {
        streamingText.value += delta;
        progressChars.value = (progressChars.value || 0) + (delta ? delta.length : 0);
        nextTick(() => scrollToBottom());
      }
    },
    onProgress: (chars: number) => { progressChars.value = chars; },
    onDone: () => { clearSseTimeout(); },
    onError: (err: Error) => {
      clearSseTimeout();
      streaming.value = false;
      streamingText.value = '';
      progressChars.value = 0;
      message.error(err?.message || '发送失败');
    },
    onResume: (cursor: number) => {
      resetSseTimeout();
      message.info(`连接恢复中...已恢复 ${cursor} 字`);
    }
  });

  currentSseAbort = stream;
}

// === SSE timeout ===
function resetSseTimeout() {
  clearSseTimeout();
  sseTimeoutTimer = setTimeout(() => handleSseTimeout(), SSE_TIMEOUT_MS);
}

function clearSseTimeout() {
  if (sseTimeoutTimer) {
    clearTimeout(sseTimeoutTimer);
    sseTimeoutTimer = null;
  }
}

function handleSseTimeout() {
  if (currentSseAbort) {
    currentSseAbort.abort();
    currentSseAbort = null;
  }
  streaming.value = false;
  streamingText.value = '';
  progressChars.value = 0;
  sseTimedOut.value = true;
}

function handleRetrySend() {
  if (!lastRequestData || sseRetryCount.value >= MAX_RETRY) {
    message.warning('已达最大重试次数，请重新发送消息');
    sseTimedOut.value = false;
    return;
  }
  sseRetryCount.value++;
  sseTimedOut.value = false;
  streaming.value = true;
  streamingText.value = '';
  progressChars.value = 0;
  nextTick(() => scrollToBottom());
  startSseStream(lastRequestData);
}

// === Tool call parsing ===
function tryParseToolCall(text: string): ToolCall | null {
  try {
    const match = text.match(/\{[\s\S]*?"action"[\s\S]*?\}/);
    if (match) {
      const parsed = JSON.parse(match[0]);
      if (parsed.action) return parsed;
    }
  } catch {
    // not a tool_call
  }
  return null;
}

function tryParseSdui(text: string): SduiPlanCard | null {
  try {
    const match = text.match(/\{[\s\S]*?"type"\s*:\s*"plan_card"[\s\S]*?\}/);
    if (match) return JSON.parse(match[0]);
  } catch {
    // not SDUI
  }
  return null;
}

async function executeToolCall(toolCall: ToolCall) {
  switch (toolCall.action) {
    case 'replace_item':
      if (toolCall.dayIndex != null && toolCall.itemIndex != null && toolCall.newItem) {
        planStore.updateDayItem(toolCall.dayIndex, toolCall.itemIndex, toolCall.newItem);
        appStore.emit('plan:updated', { action: 'replace_item', ...toolCall });
        message.success('计划已更新');
      }
      break;
    case 'replace_day_items':
      if (toolCall.dayIndex != null && toolCall.newItems) {
        planStore.replaceDayItems(toolCall.dayIndex, toolCall.newItems);
        appStore.emit('plan:updated', { action: 'replace_day_items', ...toolCall });
        message.success('计划已更新');
      }
      break;
    case 'set_plan':
      if (toolCall.plan && toolCall.days) {
        planStore.setPlan(toolCall.plan, toolCall.days);
        appStore.emit('plan:updated', { action: 'set_plan' });
        message.success('计划已更新');
      }
      break;
  }
}

// === SDUI solidify ===
async function handleApplyPlan(sdui: SduiPlanCard) {
  dialog.info({
    title: '固化计划',
    content: '确定将此计划固化到正式计划中吗？',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const { data, error } = await fetchSolidifyPlan({ tempPlanId: sdui.planId, version: sdui.version || 1 });
        if (error) {
          message.error('固化失败');
          return;
        }
        message.success('计划已固化到我的计划中');
        router.push(`/plan/${(data as any)?.id || ''}`);
      } catch {
        message.error('固化失败');
      }
    }
  });
}

// === Message actions ===
function handleRegenerate(msg: ChatMessage) {
  if (streaming.value) return;
  const idx = messages.value.indexOf(msg);
  if (idx <= 0) return;
  let userMsg: ChatMessage | null = null;
  for (let i = idx - 1; i >= 0; i--) {
    if (messages.value[i].role === 'user') {
      userMsg = messages.value[i];
      break;
    }
  }
  if (!userMsg) return;
  messages.value.splice(idx, 1);
  inputText.value = userMsg.content;
  handleSend();
}

function handleFeedback(msg: ChatMessage, type: 'useful' | 'useless') {
  if (msg.feedback === type) {
    msg.feedback = null;
  } else {
    msg.feedback = type;
  }
  message.success(type === 'useful' ? '感谢反馈！' : '已记录反馈');
}

// === Voice input (stub) ===
function startVoice() {
  recording.value = true;
  message.info('语音功能开发中...');
}

function stopVoice() {
  recording.value = false;
}

// === Utilities ===
function formatContent(text: string | undefined): string {
  if (!text) return '';
  return sanitizeHtml(
    text
      .replace(/\*\*(.*?)\*\*/g, '<b>$1</b>')
      .replace(/\n/g, '<br>')
  );
}

function scrollToBottom() {
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight;
  }
}

// === Cleanup ===
onBeforeUnmount(() => {
  if (currentSseAbort) {
    currentSseAbort.abort();
    currentSseAbort = null;
  }
  clearSseTimeout();
});
</script>

<style scoped>
.copilot-wrapper {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 1000;
}

.copilot-fab {
  position: relative;
  cursor: pointer;
}

.fab-inner {
  width: 64px;
  height: 64px;
  background: linear-gradient(135deg, #58a6ff, #7c3aed);
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #fff;
  box-shadow: 0 4px 20px rgba(88, 166, 255, 0.4);
  transition: transform 0.2s, box-shadow 0.2s;
  position: relative;
  z-index: 2;
}

.fab-inner:hover {
  transform: scale(1.08);
  box-shadow: 0 6px 28px rgba(88, 166, 255, 0.55);
}

.fab-icon { width: 24px; height: 24px; }
.fab-label { font-size: 10px; margin-top: 1px; }

.fab-pulse {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: rgba(88, 166, 255, 0.3);
  transform: translate(-50%, -50%);
  animation: fab-pulse 2s infinite;
  z-index: 1;
}

@keyframes fab-pulse {
  0% { transform: translate(-50%, -50%) scale(1); opacity: 0.6; }
  100% { transform: translate(-50%, -50%) scale(1.6); opacity: 0; }
}

.copilot-drawer {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 65vh;
  max-height: 700px;
  background: var(--n-color, #0d1117);
  border-top: 1px solid var(--n-border-color, #30363d);
  border-radius: 16px 16px 0 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 -8px 40px rgba(0, 0, 0, 0.5);
  z-index: 1001;
}

.drawer-slide-enter-active { transition: all 0.35s cubic-bezier(0.16, 1, 0.3, 1); }
.drawer-slide-leave-active { transition: all 0.25s ease-in; }
.drawer-slide-enter-from,
.drawer-slide-leave-to { transform: translateY(100%); }

.drawer-header { flex-shrink: 0; }

.handle-bar {
  width: 40px;
  height: 4px;
  background: #30363d;
  border-radius: 2px;
  margin: 8px auto 4px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px 12px;
  border-bottom: 1px solid #21262d;
}

.header-left { display: flex; align-items: center; gap: 8px; }

.header-dot {
  width: 8px;
  height: 8px;
  background: #484f58;
  border-radius: 50%;
}

.header-dot.active { background: #3fb950; }

.header-title { font-weight: 600; font-size: 15px; color: #e6edf3; }

.header-right { display: flex; align-items: center; gap: 4px; }

.context-badge { display: flex; gap: 4px; margin-right: 8px; }

.session-list {
  max-height: 120px;
  overflow-y: auto;
  border-bottom: 1px solid #21262d;
  padding: 4px 0;
}

.session-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 16px;
  cursor: pointer;
  transition: background 0.15s;
}

.session-item:hover { background: rgba(255, 255, 255, 0.04); }
.session-item.active { background: rgba(88, 166, 255, 0.1); }

.session-title {
  font-size: 13px;
  color: #8b949e;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.quick-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 12px 16px;
}

.drawer-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.welcome-tip {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: #8b949e;
  text-align: center;
}

.welcome-tip p { margin: 8px 0 0; font-size: 15px; color: #e6edf3; }
.welcome-tip .sub-tip { font-size: 13px; color: #8b949e; }

.quick-questions { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 16px; justify-content: center; }

.message-row {
  display: flex;
  gap: 10px;
  max-width: 85%;
}

.message-row.user { align-self: flex-end; flex-direction: row-reverse; }
.message-row.assistant { align-self: flex-start; }

.message-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
  background: #21262d;
  color: #e6edf3;
}

.message-row.user .message-avatar { background: #58a6ff; color: #fff; }

.message-bubble-wrapper { flex: 1; min-width: 0; }

.message-bubble {
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}

.message-row.user .message-bubble {
  background: #58a6ff;
  color: #fff;
  border-bottom-right-radius: 4px;
}

.message-row.assistant .message-bubble {
  background: #161b22;
  color: #e6edf3;
  border: 1px solid #21262d;
  border-bottom-left-radius: 4px;
}

.message-actions {
  display: flex;
  gap: 4px;
  margin-top: 4px;
  opacity: 0;
  transition: opacity 0.2s;
}

.message-row.assistant:hover .message-actions { opacity: 1; }

.sdui-plan-card {
  margin-top: 8px;
  padding: 12px;
  background: rgba(88, 166, 255, 0.08);
  border: 1px solid rgba(88, 166, 255, 0.2);
  border-radius: 8px;
}

.sdui-plan-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.sdui-plan-header h4 { margin: 0; font-size: 14px; color: #e6edf3; }

.sdui-plan-preview {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #8b949e;
}

.streaming .message-text { opacity: 0.9; }

.cursor-blink {
  animation: blink 1s step-end infinite;
  color: #58a6ff;
}

@keyframes blink {
  50% { opacity: 0; }
}

.streaming-progress {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 6px;
  font-size: 12px;
  color: #8b949e;
}

.progress-dot {
  width: 6px;
  height: 6px;
  background: #58a6ff;
  border-radius: 50%;
  animation: pulse-dot 1.5s infinite;
}

@keyframes pulse-dot {
  0%, 100% { opacity: 0.4; }
  50% { opacity: 1; }
}

.drawer-input {
  display: flex;
  gap: 8px;
  padding: 12px 16px;
  border-top: 1px solid #21262d;
  align-items: flex-end;
}

.voice-btn.recording { color: #f85149; }

.drawer-disclaimer {
  text-align: center;
  font-size: 11px;
  color: #484f58;
  padding: 6px 16px 10px;
}

.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
