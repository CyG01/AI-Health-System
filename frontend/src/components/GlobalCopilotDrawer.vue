<template>
  <div class="copilot-wrapper">
    <!-- 悬浮 AI 按钮 -->
    <div v-if="!appStore.copilotOpen" class="copilot-fab" @click="handleOpen" data-onboarding="copilot-fab">
      <div class="fab-inner">
        <svg class="fab-icon" viewBox="0 0 24 24" fill="currentColor" width="24" height="24"><path d="M7.5 5.6L10 7 8.6 4.5 10 2 7.5 3.4 5 2l1.4 2.5L5 7zm12 9.8L17 14l1.4 2.5L17 19l2.5-1.4L22 19l-1.4-2.5L22 14zM22 2l-2.5 1.4L17 2l1.4 2.5L17 7l2.5-1.4L22 7l-1.4-2.5zm-7.63 5.29a.996.996 0 0 0-1.41 0L1.29 18.96c-.39.39-.39 1.02 0 1.41l2.34 2.34c.39.39 1.02.39 1.41 0L16.7 11.05c.39-.39.39-1.02 0-1.41l-2.33-2.35z"/></svg>
        <span class="fab-label">AI助手</span>
      </div>
      <div class="fab-pulse" />
    </div>

    <!-- 底部抽屉 -->
    <transition name="drawer-slide">
      <div v-if="appStore.copilotOpen" class="copilot-drawer" :class="{ 'mobile-sheet': isMobile }">
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
              :class="{ active: String(s.id) === currentSessionId }"
              @click="selectSession(String(s.id))"
            >
              <span class="session-title">{{ s.title }}</span>
              <NButton text size="tiny" type="error" @click.stop="handleDeleteSession(String(s.id))">
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
        <div class="drawer-messages" ref="messagesRef" v-auto-animate>
          <div v-if="messages.length === 0 && !streaming" class="welcome-tip">
            <svg viewBox="0 0 24 24" fill="currentColor" width="36" height="36" style="color: var(--chart-sky)"><path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H5.17L4 17.17V4h16v12zM7 9h2v2H7zm4 0h2v2h-2zm4 0h2v2h-2z"/></svg>
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
                <img v-if="msg.image" :src="msg.image" class="msg-image" alt="用户上传图片" />
                <div class="message-text" v-html="formatContent(msg.content)"></div>
              </div>

              <!-- SDUI dynamic widget renderer -->
              <ErrorBoundary
                v-if="msg.sdui"
                fallbackTitle="组件渲染异常"
                fallbackMessage="AI 返回的数据格式有误"
              >
                <SduiRenderer :widget="msg.sdui" />
              </ErrorBoundary>

              <!-- Progressive SDUI: show partial widget during streaming -->
              <ErrorBoundary
                v-if="streaming && progressiveSdui && idx === messages.length - 1"
                fallbackTitle="图表加载中"
                fallbackMessage="数据正在生成..."
              >
                <SduiRenderer :widget="progressiveSdui" />
              </ErrorBoundary>

              <!-- AI 回复操作按钮 + RLHF 反馈 -->
              <div v-if="msg.role === 'assistant' && msg.content && !msg.sdui" class="message-actions">
                <NButton text size="small" @click="handleRegenerate(msg)" title="重新生成">重新生成</NButton>
                <RlhfFeedback
                  v-if="msg.messageId"
                  :messageId="msg.messageId"
                  :aiContent="msg.content"
                  @feedback="(payload) => handleRlhfFeedback(msg, payload)"
                />
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
          <!-- 图片预览 -->
          <div v-if="pendingImage" class="image-preview-bar">
            <img :src="pendingImage" alt="pending" />
            <NButton text size="tiny" @click="clearPendingImage" title="移除图片">
              <template #icon><svg viewBox="0 0 24 24" fill="currentColor" width="14" height="14"><path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/></svg></template>
            </NButton>
          </div>
          <input ref="fileInputRef" type="file" accept="image/*" capture="environment" style="display:none" @change="handleFileSelected" />
          <NButton
            class="photo-btn"
            circle
            @click="triggerFileInput"
            title="拍照/选图"
            data-onboarding="photo-btn"
          >
            <template #icon><svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16"><path d="M12 15.2a3.2 3.2 0 1 0 0-6.4 3.2 3.2 0 0 0 0 6.4z"/><path d="M9 2L7.17 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2h-3.17L15 2H9zm3 15c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5z"/></svg></template>
          </NButton>
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
            :disabled="(!inputText.trim() && !pendingImage) || streaming"
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
import { ref, computed, nextTick, onBeforeUnmount, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { NButton, NTag, NInput, NAlert, useMessage, useDialog } from 'naive-ui';
import { useMediaQuery } from '@vueuse/core';
import { vAutoAnimate } from '@formkit/auto-animate/vue';
import ErrorBoundary from '@/components/ErrorBoundary.vue';
import SduiRenderer from '@/components/SduiRenderer.vue';
import RlhfFeedback from '@/components/RlhfFeedback.vue';
import { sanitizeHtml } from '@/utils/sanitize';
import { cacheChatMessages } from '@/utils/offlineCache';
import { createSSEStream } from '@/utils/sseClient';
import { ProgressiveJsonParser } from '@/utils/progressiveJsonParser';
import { captureSduiError } from '@/utils/telemetry';
import { fetchCreateSession, fetchGetSessionList, fetchGetMessages, fetchDeleteSession } from '@/service/api';
import { fetchSolidifyPlan } from '@/service/api';
import { fetchGetLatestHealth } from '@/service/api';
import { SEND_WITH_CONTEXT_URL } from '@/service/api/chat';
import { useAppStore } from '@/store/modules/app';
import { usePlanStore } from '@/store/modules/plan';

defineOptions({ name: 'GlobalCopilotDrawer' });

// Types
interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
  sdui?: Sdui.Widget;
  feedback?: 'useful' | 'useless' | null;
  image?: string;
  messageId?: string | number;
}

/** Progressive SDUI parser for streaming rendering */
const progressiveParser = new ProgressiveJsonParser();
const progressiveSdui = ref<Sdui.Widget | null>(null);

/** @deprecated kept for backward compat in loadMessages */
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
  newItem?: Record<string, unknown>;
  newItems?: Array<Record<string, unknown>>;
  plan?: Api.Plan.AiPlan;
  days?: Array<Record<string, unknown>>;
}

interface ChatSession extends Api.Chat.Session {
  // Local extensions can be added here
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

// Stores
const appStore = useAppStore();
const planStore = usePlanStore();

// Mobile detection for responsive bottom sheet
const isMobile = useMediaQuery('(max-width: 640px)');

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
let lastRequestData: Record<string, unknown> | null = null;
let lastEndpoint: string = '/chat/send';

// === Image / multimodal ===
const fileInputRef = ref<HTMLInputElement | null>(null);
const pendingImage = ref<string | null>(null);

function triggerFileInput() {
  fileInputRef.value?.click();
}

function handleFileSelected(e: Event) {
  const input = e.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) return;
  if (file.size > 5 * 1024 * 1024) {
    message.warning('图片不能超过5MB');
    return;
  }
  const reader = new FileReader();
  reader.onload = () => {
    pendingImage.value = reader.result as string;
  };
  reader.readAsDataURL(file);
  input.value = ''; // reset so same file can be re-selected
}

function clearPendingImage() {
  pendingImage.value = null;
}

// === Context awareness ===
const contextInfo = computed<ContextInfo | null>(() => {
  const ctx = appStore.copilotContext.value;
  if (ctx) return resolveContextDisplay(ctx);
  const path = route.path;
  if (path.startsWith('/plan/') && path !== '/plan/list' && path !== '/plan/generate') {
    return { page: 'planDetail', label: '计划详情', icon: 'DataLine', entityName: null };
  }
  if (path === '/food/record') return { page: 'foodRecord', label: '饮食记录', icon: 'Dish', entityName: null };
  if (path.startsWith('/health')) return { page: 'health', label: '健康档案', icon: 'User', entityName: null };
  if (path.startsWith('/exercise')) return { page: 'exercise', label: '运动记录', icon: 'Fitness', entityName: null };
  if (path === '/checkin/calendar') return { page: 'calendar', label: '打卡日历', icon: 'Calendar', entityName: null };
  if (path === '/dashboard') return { page: 'dashboard', label: '健康看板', icon: 'DataLine', entityName: null };
  return null;
});

function resolveContextDisplay(ctx: Record<string, unknown>): ContextInfo {
  const displays: Record<string, { label: string; icon: string }> = {
    planDetail: { label: '计划详情', icon: 'DataLine' },
    foodRecord: { label: '饮食记录', icon: 'Dish' },
    health: { label: '健康档案', icon: 'User' },
    exercise: { label: '运动记录', icon: 'Fitness' },
    calendar: { label: '打卡日历', icon: 'Calendar' },
    dashboard: { label: '健康看板', icon: 'DataLine' }
  };
  const page = (ctx.page as string) || 'unknown';
  const d = displays[page] || { label: page, icon: 'User' };
  return { ...d, page, entityName: (ctx.entityName as string) || null };
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
    sessions.value = (data as ChatSession[]) || [];
    if (sessions.value.length > 0) {
      currentSessionId.value = String(sessions.value[0].id);
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
    const session = data as unknown as ChatSession;
    currentSessionId.value = String(session.id);
    messages.value = [];
    showSessionList.value = false;
    sessions.value.unshift(session);
  } catch {
    // handled by interceptor
  }
}

async function loadMessages() {
  if (!currentSessionId.value) return;
  try {
    const { data, error } = await fetchGetMessages(currentSessionId.value);
    if (error) return;
    const apiMessages = (data as Api.Chat.Message[]) || [];
    messages.value = apiMessages
      .filter(m => m.role === 'user' || m.role === 'assistant')
      .map(m => ({
        role: m.role as 'user' | 'assistant',
        content: m.content,
        sdui: m.planCard ? { type: 'plan_card' as const, planName: m.planCard.title, durationDays: 0, planType: '', totalExercises: 0 } : undefined
      }));
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
        sessions.value = sessions.value.filter(s => String(s.id) !== id);
        if (currentSessionId.value === id) {
          if (sessions.value.length > 0) {
            currentSessionId.value = String(sessions.value[0].id);
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

async function handleSend() {
  const text = inputText.value.trim();
  const image = pendingImage.value;
  if ((!text && !image) || streaming.value || !currentSessionId.value) return;

  if (currentSseAbort) {
    currentSseAbort.abort();
    currentSseAbort = null;
  }

  const userMsg: ChatMessage = { role: 'user', content: text || '[图片]', image: image || undefined };
  messages.value.push(userMsg);
  cacheChatMessages(currentSessionId.value, messages.value as unknown as Record<string, unknown>[]);
  inputText.value = '';
  pendingImage.value = null;
  streaming.value = true;
  streamingText.value = '';
  progressChars.value = 0;
  sseTimedOut.value = false;
  sseRetryCount.value = 0;

  nextTick(() => scrollToBottom());

  const ctx = contextInfo.value;
  let endpoint = '/chat/send';
  let contextPayload: Record<string, unknown> | null = null;

  // When opened from a specific page, automatically include that page's context
  if (ctx) {
    contextPayload = {
      page: ctx.page,
      entityId: appStore.copilotContext.value?.entityId ?? null
    };

    // Fetch rich context data depending on the current page
    try {
      switch (ctx.page) {
        case 'health':
        case 'dashboard': {
          const { data: healthData, error } = await fetchGetLatestHealth();
          if (!error && healthData) {
            contextPayload.healthData = healthData;
          }
          break;
        }
        case 'planDetail': {
          // Include current plan summary if available
          const plan = planStore.currentPlan.value;
          if (plan) {
            contextPayload.planData = {
              planName: plan.planName,
              planType: plan.planType,
              durationDays: plan.durationDays,
              days: planStore.currentPlanDays.value?.slice(0, 3).map(d => ({
                dayIndex: d.dayIndex,
                exercises: ((d.items as Array<Record<string, unknown>>) || [])
                  .map(i => (i.name || i.exerciseName) as string)
                  .join(', ')
              }))
            };
          }
          break;
        }
        case 'foodRecord': {
          // Include a note that user is on the food recording page
          contextPayload.pageHint = '用户正在记录饮食，请根据健康档案给出营养建议';
          break;
        }
        case 'calendar':
        case 'exercise': {
          contextPayload.pageHint = '用户正在查看运动/打卡记录';
          break;
        }
      }
    } catch {
      // Failed to fetch context data — still send with basic page context
      console.warn('[Copilot] Failed to fetch page context data, using basic context');
    }

    endpoint = SEND_WITH_CONTEXT_URL;
  }

  const requestData = {
    sessionId: currentSessionId.value,
    content: text || '[图片]',
    context: contextPayload,
    image: image || undefined
  };
  lastRequestData = requestData;
  lastEndpoint = endpoint;
  startSseStream(endpoint, requestData);
}

function startSseStream(endpoint: string, requestData: Record<string, unknown>) {
  clearSseTimeout();
  resetSseTimeout();

  const stream = createSSEStream(endpoint, requestData, {
    onMessage: (delta: string) => {
      resetSseTimeout();

      if (delta === '[DONE]') {
        clearSseTimeout();
        const toolCall = tryParseToolCall(streamingText.value);
        const sdui = tryParseSdui(streamingText.value);

        // Reset progressive parser
        progressiveParser.reset();
        progressiveSdui.value = null;

        const assistantMsg: ChatMessage = {
          role: 'assistant',
          content: streamingText.value,
          sdui: sdui || undefined,
          messageId: Date.now()
        };
        messages.value.push(assistantMsg);

        if (toolCall) executeToolCall(toolCall);

        streaming.value = false;
        streamingText.value = '';
        progressChars.value = 0;
        sseTimedOut.value = false;
        sseRetryCount.value = 0;
        if (currentSessionId.value) {
          cacheChatMessages(currentSessionId.value, messages.value as unknown as Record<string, unknown>[]);
        }
        nextTick(() => scrollToBottom());
      } else if (delta === '[ERROR]') {
        clearSseTimeout();
        streaming.value = false;
        streamingText.value = '';
        progressChars.value = 0;
        progressiveParser.reset();
        progressiveSdui.value = null;
        message.error('AI回复失败');
      } else {
        streamingText.value += delta;
        progressChars.value = (progressChars.value || 0) + (delta ? delta.length : 0);

        // Progressive SDUI: try to parse incrementally
        const parseResult = progressiveParser.feed(delta);
        if (parseResult.complete) {
          progressiveSdui.value = parseResult.complete;
        } else if (parseResult.partial && parseResult.partial.type) {
          // Show partial widget for progressive rendering
          progressiveSdui.value = parseResult.partial as unknown as Sdui.Widget;
        }

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
  startSseStream(lastEndpoint, lastRequestData);
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

function tryParseSdui(text: string): Sdui.Widget | null {
  try {
    const match = text.match(/\{[\s\S]*?"type"\s*:\s*"(\w+)"[\s\S]*?\}/);
    if (match) {
      const parsed = JSON.parse(match[0]);
      if (parsed.type) return parsed as Sdui.Widget;
    }
  } catch (err) {
    // Capture SDUI parse error for telemetry
    const widgetType = text.match(/"type"\s*:\s*"(\w+)"/)?.[1] || 'unknown';
    captureSduiError(widgetType, text.slice(0, 200), err instanceof Error ? err : new Error(String(err)));
  }
  return null;
}

async function executeToolCall(toolCall: ToolCall) {
  switch (toolCall.action) {
    case 'replace_item':
      if (toolCall.dayIndex != null && toolCall.itemIndex != null && toolCall.newItem) {
        planStore.updateDayItem(toolCall.dayIndex, {
          itemIndex: toolCall.itemIndex,
          newItem: toolCall.newItem
        });
        message.success('计划已更新');
      }
      break;
    case 'replace_day_items':
      if (toolCall.dayIndex != null && toolCall.newItems) {
        planStore.replaceDayItems(toolCall.dayIndex, toolCall.newItems as unknown as Record<string, unknown>);
        message.success('计划已更新');
      }
      break;
    case 'set_plan':
      if (toolCall.plan && toolCall.days) {
        planStore.setPlan(toolCall.plan, toolCall.days);
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
        const result = data as unknown as Api.Plan.Plan;
        router.push(`/plan/${result?.id || ''}`);
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

/** Handle RLHF feedback from RlhfFeedback component */
function handleRlhfFeedback(msg: ChatMessage, payload: { messageId: string | number; vote: 'up' | 'down'; aiContent?: string }) {
  msg.feedback = payload.vote === 'up' ? 'useful' : 'useless';
  // TODO: Call backend AI feedback API when available
  // fetchAiFeedback({ messageId: payload.messageId, vote: payload.vote, content: payload.aiContent })
  message.success('感谢反馈！');
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
  background: linear-gradient(135deg, var(--chart-sky), var(--sport-accent));
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--sport-text-primary);
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
  background: var(--n-color, var(--sport-bg-base));
  border-top: 1px solid var(--n-border-color, var(--sport-border));
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
  background: var(--sport-border);
  border-radius: 2px;
  margin: 8px auto 4px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px 12px;
  border-bottom: 1px solid var(--sport-bg-elevated);
}

.header-left { display: flex; align-items: center; gap: 8px; }

.header-dot {
  width: 8px;
  height: 8px;
  background: var(--sport-text-tertiary);
  border-radius: 50%;
}

.header-dot.active { background: var(--color-success); }

.header-title { font-weight: 600; font-size: 15px; color: var(--sport-text-primary); }

.header-right { display: flex; align-items: center; gap: 4px; }

.context-badge { display: flex; gap: 4px; margin-right: 8px; }

.session-list {
  max-height: 120px;
  overflow-y: auto;
  border-bottom: 1px solid var(--sport-bg-elevated);
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
  color: var(--sport-text-secondary);
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
  color: var(--sport-text-secondary);
  text-align: center;
}

.welcome-tip p { margin: 8px 0 0; font-size: 15px; color: var(--sport-text-primary); }
.welcome-tip .sub-tip { font-size: 13px; color: var(--sport-text-secondary); }

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
  background: var(--sport-bg-elevated);
  color: var(--sport-text-primary);
}

.message-row.user .message-avatar { background: var(--chart-sky); color: var(--sport-text-primary); }

.message-bubble-wrapper { flex: 1; min-width: 0; }

.message-bubble {
  padding: 10px 14px;
  border-radius: 12px;
  font-size: calc(14px * var(--a11y-font-scale, 1));
  line-height: 1.6;
  word-break: break-word;
  /* Micro-interaction: hover float + shadow */
  transition: transform 0.2s cubic-bezier(0.34, 1.56, 0.64, 1),
              box-shadow 0.2s ease;
}

.message-bubble:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.message-row.user .message-bubble {
  background: var(--chart-sky);
  color: var(--sport-text-primary);
  border-bottom-right-radius: 4px;
}

.message-row.assistant .message-bubble {
  background: var(--sport-bg-surface);
  color: var(--sport-text-primary);
  border: 1px solid var(--sport-bg-elevated);
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

.streaming .message-text { opacity: 0.9; }

.cursor-blink {
  animation: blink 1s step-end infinite;
  color: var(--chart-sky);
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
  color: var(--sport-text-secondary);
}

.progress-dot {
  width: 6px;
  height: 6px;
  background: var(--chart-sky);
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
  border-top: 1px solid var(--sport-bg-elevated);
  align-items: flex-end;
  position: relative;
}

.voice-btn.recording { color: var(--color-danger); }

.photo-btn { color: var(--sport-text-secondary); flex-shrink: 0; }
.photo-btn:hover { color: var(--chart-sky); }

.image-preview-bar {
  display: flex; align-items: center; gap: 6px; padding: 4px 0;
  position: absolute; bottom: 100%; left: 0; right: 0; padding-left: 12px;
}
.image-preview-bar img {
  height: 48px; border-radius: 6px; border: 1px solid var(--sport-bg-elevated);
  object-fit: cover;
}

.msg-image {
  max-width: 200px; max-height: 160px; border-radius: 8px;
  margin-bottom: 6px; display: block; object-fit: cover;
}

.drawer-disclaimer {
  text-align: center;
  font-size: 11px;
  color: var(--sport-text-tertiary);
  padding: 6px 16px 10px;
}

.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

/* ===== Mobile bottom sheet ===== */
.copilot-drawer.mobile-sheet {
  height: 75vh;
  max-height: none;
  border-radius: 16px 16px 0 0;
}

.copilot-drawer.mobile-sheet .handle-bar {
  width: 48px;
  height: 5px;
  cursor: grab;
  transition: background 0.2s;
}

.copilot-drawer.mobile-sheet .handle-bar:active {
  cursor: grabbing;
  background: var(--chart-sky);
}

.copilot-drawer.mobile-sheet .message-row {
  max-width: 92%;
}

.copilot-drawer.mobile-sheet .drawer-input {
  padding: 8px 12px;
}

/* ===== Accessibility: font scaling ===== */
.copilot-drawer {
  font-size: calc(1em * var(--a11y-font-scale, 1));
}

/* ===== Responsive: desktop drawer stays right-aligned ===== */
@media (min-width: 641px) {
  .copilot-drawer {
    position: fixed;
    bottom: 96px;
    right: 24px;
    left: auto;
    width: 420px;
    height: 65vh;
    max-height: 700px;
    border-radius: 16px;
    border: 1px solid var(--sport-bg-elevated);
  }
}
</style>
