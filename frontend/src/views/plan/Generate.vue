<template>
  <div class="p-1 relative">
    <MedicalDisclaimerBanner />

    <!-- Form Section -->
    <n-card v-if="!streaming" class="max-w-[720px]">
      <h2 class="text-xl font-semibold mb-1">AI 智能计划生成</h2>
      <p class="text-gray-400 text-sm mb-4">基于您的健康档案，由 DeepSeek 为您量身定制个性化健康计划（运动/饮食/综合/康复/冥想）</p>

      <n-alert title="每日最多生成3次计划" type="warning" :closable="false" class="mb-4" />

      <n-form ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="120" class="mt-6">
        <n-form-item label="计划类型" path="planType">
          <n-radio-group v-model:value="form.planType">
            <n-radio value="sport">运动计划</n-radio>
            <n-radio value="diet">饮食计划</n-radio>
            <n-radio value="comprehensive">综合计划</n-radio>
            <n-radio value="rehabilitation">康复计划</n-radio>
            <n-radio value="meditation">冥想放松</n-radio>
          </n-radio-group>
        </n-form-item>

        <n-form-item label="计划天数" path="durationDays">
          <n-radio-group v-model:value="form.durationDays">
            <n-radio :value="7">7天</n-radio>
            <n-radio :value="30">30天</n-radio>
          </n-radio-group>
        </n-form-item>

        <n-form-item label="运动强度" path="intensity">
          <n-select
            v-model:value="form.intensity"
            placeholder="请选择运动强度"
            clearable
            :options="intensityOptions"
            class="w-[240px]"
          />
        </n-form-item>

        <n-form-item v-if="form.planType === 'diet'" label="口味偏好" path="tastePreference">
          <n-select
            v-model:value="form.tastePreference"
            placeholder="请选择口味偏好"
            clearable
            :options="tasteOptions"
            class="w-[240px]"
          />
        </n-form-item>

        <n-form-item>
          <div class="flex gap-3">
            <n-button
              type="primary"
              size="large"
              :loading="generating"
              :disabled="generating"
              @click="handleGenerate"
            >
              {{ generating ? '正在生成中...' : '开始生成' }}
            </n-button>
            <n-button :disabled="generating" @click="router.push('/plan/list')">返回列表</n-button>
          </div>
        </n-form-item>
      </n-form>
    </n-card>

    <!-- Streaming Section -->
    <div v-if="streaming" class="max-w-[900px] border border-blue-400 rounded-lg overflow-hidden flex flex-col" style="height: calc(100vh - 140px)">
      <!-- Streaming Header -->
      <div class="flex items-center justify-between px-4 py-3 bg-gray-900/95 border-b border-gray-700">
        <div class="flex items-center gap-2">
          <span class="w-3 h-3 rounded-full" style="background:#3fb950"></span>
          <span class="w-3 h-3 rounded-full" style="background:#d29922"></span>
          <span class="w-3 h-3 rounded-full" style="background:#f85149"></span>
          <span class="text-[13px] text-gray-400 ml-2 font-mono">{{ planTypeLabel }} · {{ form.durationDays }}天 · AI生成中</span>
        </div>
        <div class="flex items-center">
          <span v-if="streamStatus === 'streaming'" class="inline-flex items-center gap-1.5 text-xs px-3 py-1 rounded-full bg-blue-500/12 text-blue-400 border border-blue-500/25">
            <span class="w-2 h-2 rounded-full bg-blue-400 animate-pulse"></span>实时生成中
          </span>
          <span v-else-if="streamStatus === 'complete'" class="inline-flex items-center gap-1.5 text-xs px-3 py-1 rounded-full bg-green-500/12 text-green-400 border border-green-500/25">
            ✓ 生成成功
          </span>
          <span v-else-if="streamStatus === 'error'" class="inline-flex items-center gap-1.5 text-xs px-3 py-1 rounded-full bg-red-500/12 text-red-400 border border-red-500/25">
            ✗ 生成失败
          </span>
        </div>
      </div>

      <!-- Streaming Body -->
      <div
        ref="streamBodyRef"
        class="flex-1 px-6 py-5 overflow-y-auto bg-gray-950/95 font-mono text-sm leading-relaxed text-gray-300 whitespace-pre-wrap break-words"
      >
        <pre class="m-0 whitespace-pre-wrap break-words">{{ displayText }}<span v-if="streamStatus === 'streaming'" class="text-blue-400 animate-pulse">|</span></pre>
      </div>

      <!-- Streaming Footer -->
      <div class="flex items-center justify-center gap-3 px-4 py-3 bg-gray-900/95 border-t border-gray-700 min-h-[48px]">
        <template v-if="streamStatus === 'complete'">
          <n-button type="primary" size="small" @click="goToPlanList">
            生成成功！正在跳转... ({{ countdown }}秒)
          </n-button>
        </template>
        <template v-else-if="streamStatus === 'error'">
          <n-alert :title="errorMessage" type="error" :closable="false" class="max-w-[360px]" />
          <n-button type="primary" size="small" @click="resetForm">重新生成</n-button>
        </template>
        <template v-else>
          <span class="text-xs text-gray-600 font-mono">{{ totalChars }} 字符已生成</span>
          <n-button size="tiny" quaternary type="error" @click="abortStreaming">取消</n-button>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { NAlert, NButton, NCard, NForm, NFormItem, NRadio, NRadioGroup, NSelect } from 'naive-ui';
import type { FormInst, FormRules } from 'naive-ui';
import { createSSEStream } from '@/utils/sseClient';
import { usePlanStore } from '@/store/modules/plan';
import MedicalDisclaimerBanner from '@/components/MedicalDisclaimerBanner.vue';

defineOptions({ name: 'PlanGenerate' });

interface PlanForm {
  planType: string;
  durationDays: number;
  intensity: string;
  tastePreference: string;
}

type StreamStatus = 'streaming' | 'complete' | 'error';

const router = useRouter();
const planStore = usePlanStore();
const formRef = ref<FormInst | null>(null);
const streamBodyRef = ref<HTMLElement | null>(null);
const generating = ref(false);
const streaming = ref(false);
const displayText = ref('');
const streamStatus = ref<StreamStatus>('streaming');
const errorMessage = ref('');
const totalChars = ref(0);
const countdown = ref(0);

let typewriterQueue: string[] = [];
let typewriterTimer: ReturnType<typeof setInterval> | null = null;
let aborted = false;
let countdownTimer: ReturnType<typeof setInterval> | null = null;
let currentStreamControl: ReturnType<typeof createSSEStream> | null = null;

const form = reactive<PlanForm>({
  planType: 'sport',
  durationDays: 7,
  intensity: '',
  tastePreference: ''
});

const intensityOptions = [
  { label: '轻松（适合入门）', value: '轻松' },
  { label: '适中（常规训练）', value: '适中' },
  { label: '高强度（挑战极限）', value: '高强度' }
];

const tasteOptions = [
  { label: '清淡', value: '清淡' },
  { label: '家常', value: '家常' },
  { label: '低脂', value: '低脂' },
  { label: '高蛋白', value: '高蛋白' }
];

const planTypeLabel = computed(() => {
  const map: Record<string, string> = {
    sport: '运动计划',
    diet: '饮食计划',
    comprehensive: '综合计划',
    rehabilitation: '康复计划',
    meditation: '冥想放松'
  };
  return map[form.planType] || '健康计划';
});

const rules: FormRules = {
  planType: [{ required: true, message: '请选择计划类型', trigger: 'change' }],
  durationDays: [{ required: true, type: 'number', message: '请选择计划天数', trigger: 'change' }]
};

function autoScroll(): void {
  if (streamBodyRef.value) {
    streamBodyRef.value.scrollTop = streamBodyRef.value.scrollHeight;
  }
}

function startTypewriter(fullText: string): void {
  const chars = fullText.split('');
  typewriterQueue.push(...chars);
  if (!typewriterTimer) {
    runTypewriter();
  }
}

function runTypewriter(): void {
  typewriterTimer = setInterval(() => {
    if (aborted) return;
    if (typewriterQueue.length === 0) {
      clearInterval(typewriterTimer!);
      typewriterTimer = null;
      return;
    }
    const chunk = typewriterQueue.splice(0, 1);
    displayText.value += chunk.join('');
    totalChars.value = displayText.value.length;
    autoScroll();
  }, 20);
}

function startCountdown(seconds: number): void {
  countdown.value = seconds;
  countdownTimer = setInterval(() => {
    countdown.value--;
    if (countdown.value <= 0) {
      clearInterval(countdownTimer!);
      countdownTimer = null;
      router.push('/plan/list');
    }
  }, 1000);
}

function flushTypewriterQueue(): void {
  if (typewriterTimer) {
    clearInterval(typewriterTimer);
    typewriterTimer = null;
  }
  if (typewriterQueue.length > 0) {
    displayText.value += typewriterQueue.join('');
    typewriterQueue = [];
    totalChars.value = displayText.value.length;
    autoScroll();
  }
}

function handleIncomingMessage(data: string): void {
  if (aborted) return;

  if (data === '[DONE]') {
    streamStatus.value = 'complete';
    flushTypewriterQueue();
    planStore.finishStreaming();
    startCountdown(2);
    return;
  }

  if (data === '[ERROR]') {
    streamStatus.value = 'error';
    flushTypewriterQueue();
    errorMessage.value = errorMessage.value || 'AI服务调用失败';
    generating.value = false;
    planStore.finishStreaming();
    return;
  }

  // Update store streaming content
  planStore.appendStreamingContent(data);
  totalChars.value = displayText.value.length + typewriterQueue.length + data.length;
  startTypewriter(data);
}

function abortStreaming(): void {
  aborted = true;
  if (currentStreamControl) {
    currentStreamControl.abort();
    currentStreamControl = null;
  }
  flushTypewriterQueue();
  planStore.resetStreaming();
  streaming.value = false;
  generating.value = false;
}

async function handleGenerate(): Promise<void> {
  if (!formRef.value) return;
  try {
    await formRef.value.validate();
  } catch {
    return;
  }

  generating.value = true;
  streaming.value = true;
  displayText.value = '';
  streamStatus.value = 'streaming';
  errorMessage.value = '';
  totalChars.value = 0;
  aborted = false;

  // Start store streaming state
  planStore.startStreaming(form.planType);

  try {
    const payload = {
      planType: form.planType,
      durationDays: form.durationDays,
      intensity: form.intensity || undefined,
      tastePreference: form.tastePreference || undefined
    };

    // CRITICAL: createSSEStream takes 3 args: (url, data, callbacks_object)
    currentStreamControl = createSSEStream('/ai-plan/generate-stream', payload, {
      onMessage: (text: string) => {
        handleIncomingMessage(text);
      },
      onDone: () => {
        streaming.value = false;
        generating.value = false;
        currentStreamControl = null;
      },
      onError: (err: Error) => {
        if (aborted) return;
        streamStatus.value = 'error';
        errorMessage.value = err.message || '网络连接中断，请重试';
        generating.value = false;
        planStore.finishStreaming();
        if (typewriterTimer) {
          clearInterval(typewriterTimer);
          typewriterTimer = null;
        }
        currentStreamControl = null;
      }
    });

    await currentStreamControl.promise;
  } catch {
    if (aborted) return;
    streamStatus.value = 'error';
    errorMessage.value = '网络连接中断，请重试';
    planStore.resetStreaming();
  }
  generating.value = false;
  currentStreamControl = null;
}

function resetForm(): void {
  aborted = true;
  if (currentStreamControl) {
    currentStreamControl.abort();
    currentStreamControl = null;
  }
  if (typewriterTimer) {
    clearInterval(typewriterTimer);
    typewriterTimer = null;
  }
  if (countdownTimer) {
    clearInterval(countdownTimer);
    countdownTimer = null;
  }
  typewriterQueue = [];
  streaming.value = false;
  displayText.value = '';
  generating.value = false;
  errorMessage.value = '';
  countdown.value = 0;
  streamStatus.value = 'streaming';
  totalChars.value = 0;
  planStore.resetStreaming();
}

function goToPlanList(): void {
  if (countdownTimer) {
    clearInterval(countdownTimer);
    countdownTimer = null;
  }
  router.push('/plan/list');
}

onUnmounted(() => {
  aborted = true;
  if (currentStreamControl) {
    currentStreamControl.abort();
    currentStreamControl = null;
  }
  if (typewriterTimer) {
    clearInterval(typewriterTimer);
    typewriterTimer = null;
  }
  if (countdownTimer) {
    clearInterval(countdownTimer);
    countdownTimer = null;
  }
});
</script>
