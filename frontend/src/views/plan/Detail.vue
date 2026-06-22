<template>
  <div class="p-1">
    <n-spin :show="pageLoading">
      <div v-if="plan" class="max-w-[860px]">
        <MedicalDisclaimerBanner />

        <!-- Header Card -->
        <n-card class="px-8 py-7 mb-4">
          <div class="flex items-center justify-between mb-3">
            <div class="flex items-center gap-3">
              <n-button quaternary @click="router.push('/plan/list')">
                <template #icon><n-icon :size="16"><ArrowBackOutline /></n-icon></template>
                返回
              </n-button>
              <h2 class="text-xl font-semibold m-0">{{ plan.planName }}</h2>
              <n-tag :type="planTypeTagType" size="small">
                {{ planTypeLabel }}
              </n-tag>
              <n-tag v-if="plan.status === 1" type="warning" size="small">当前生效</n-tag>
            </div>
            <div class="flex items-center gap-2">
              <n-button secondary type="info" @click="openEditContentModal">编辑计划内容</n-button>
              <n-button type="primary" @click="handlePrintPreview">导出 PDF</n-button>
            </div>
          </div>
          <div class="text-[13px] text-gray-400 mt-1">
            <span>{{ plan.durationDays }}天计划</span>
            <span class="mx-1.5 text-gray-700">|</span>
            <span>开始日期：{{ plan.startDate }}</span>
          </div>

          <!-- Progress Overview -->
          <div v-if="totalTasks > 0" class="mt-4 flex items-center gap-3">
            <span class="text-xs text-gray-400 shrink-0">整体进度</span>
            <n-progress
              type="line"
              :percentage="completionPercentage"
              :show-indicator="true"
              class="flex-1"
            />
            <span class="text-xs text-gray-400 shrink-0">{{ completedTasks }}/{{ totalTasks }} 已完成</span>
          </div>
        </n-card>

        <!-- Day Cards -->
        <div v-if="planDays.length > 0" class="flex flex-col gap-3">
          <n-card v-for="(day, dayIdx) in planDays" :key="day.d" class="px-6 py-5">
            <div class="flex items-center justify-between mb-3 pb-2 border-b border-gray-700/30">
              <div class="flex items-center gap-3">
                <span class="text-base font-semibold text-blue-400">Day {{ day.d }}</span>
                <span class="text-xs text-gray-400">{{ formatDayDate(day.d) }}</span>
              </div>
              <div class="flex items-center gap-2">
                <n-button size="tiny" secondary @click="openReplaceDayModal(dayIdx)">替换当天任务</n-button>
                <span v-if="dayTaskCount(dayIdx) > 0" class="text-xs text-gray-500">
                  {{ dayCompletedCount(dayIdx) }}/{{ dayTaskCount(dayIdx) }} 完成
                </span>
              </div>
            </div>
            <ul class="flex flex-col gap-2 list-none m-0 p-0">
              <li v-for="(item, idx) in day.items" :key="idx">
                <!-- Structured phase format -->
                <template v-if="isPhaseItem(item)">
                  <div class="border border-gray-700/40 rounded-lg p-3.5 bg-gray-900/50">
                    <div class="flex items-center gap-2 mb-3 pb-2.5 border-b border-gray-700/30">
                      <n-checkbox
                        :checked="isTaskCompleted(dayIdx, idx)"
                        :disabled="isTaskCompleted(dayIdx, idx)"
                        @update:checked="() => handleCompleteTask(dayIdx, idx, item)"
                      />
                      <span class="text-sm font-semibold flex-1" :class="{ 'line-through text-gray-500': isTaskCompleted(dayIdx, idx) }">
                        {{ item.name }}
                      </span>
                      <n-tag size="small" type="info">{{ item.durationMin }}分钟</n-tag>
                    </div>
                    <div class="flex flex-col">
                      <div
                        v-for="(phase, pi) in item.phases"
                        :key="pi"
                        class="flex gap-3"
                      >
                        <!-- Phase indicator -->
                        <div class="flex flex-col items-center w-3.5 shrink-0">
                          <span
                            class="w-2.5 h-2.5 rounded-full mt-1.5 shrink-0"
                            :class="{
                              'bg-yellow-500': phase.type === 'warmup',
                              'bg-red-500': phase.type === 'core',
                              'bg-green-500': phase.type === 'cooldown'
                            }"
                          ></span>
                          <span v-if="pi < item.phases.length - 1" class="w-0.5 flex-1 min-h-[18px] bg-gray-700/50 mt-0.5"></span>
                        </div>
                        <!-- Phase body -->
                        <div class="flex-1 pb-3">
                          <div class="flex items-center gap-2 flex-wrap">
                            <n-tag :type="phaseTagType(phase.type)" size="small">
                              {{ phaseTypeLabel(phase.type) }}
                            </n-tag>
                            <span class="text-[13px] text-gray-300 font-medium">{{ phase.name }}</span>
                            <span class="text-xs text-gray-400 ml-auto">{{ phase.minutes }}分钟</span>
                          </div>
                          <p v-if="phase.instruction" class="mt-1 mb-0 text-xs text-gray-400 leading-relaxed">{{ phase.instruction }}</p>
                        </div>
                      </div>
                    </div>
                  </div>
                </template>
                <!-- Flat string format (legacy) -->
                <template v-else>
                  <div class="flex items-start gap-2 text-sm leading-relaxed">
                    <n-checkbox
                      :checked="isTaskCompleted(dayIdx, idx)"
                      :disabled="isTaskCompleted(dayIdx, idx)"
                      @update:checked="() => handleCompleteTask(dayIdx, idx, item)"
                    />
                    <!-- Inline editing mode -->
                    <template v-if="editingTask?.dayIdx === dayIdx && editingTask?.itemIdx === idx">
                      <n-input
                        v-model:value="editText"
                        size="small"
                        class="flex-1"
                        @keyup.enter="saveEditTask"
                      />
                      <n-button size="tiny" type="primary" @click="saveEditTask">保存</n-button>
                      <n-button size="tiny" @click="cancelEditTask">取消</n-button>
                    </template>
                    <!-- Normal display mode -->
                    <template v-else>
                      <span
                        class="flex-1 cursor-pointer hover:text-blue-400 transition-colors"
                        :class="{ 'line-through text-gray-500': isTaskCompleted(dayIdx, idx) }"
                        @dblclick="startEditTask(dayIdx, idx, item)"
                      >
                        {{ typeof item === 'string' ? item : item }}
                      </span>
                    </template>
                  </div>
                </template>
              </li>
            </ul>
          </n-card>
        </div>

        <!-- Empty plan days -->
        <n-empty v-if="planDays.length === 0" description="计划内容为空" class="my-8" />

        <!-- AI Dynamic Adjustment -->
        <n-card class="mt-5 px-6 py-5 border-blue-400/20">
          <div class="flex justify-between items-center mb-2.5">
            <h3 class="m-0 text-[15px] text-blue-400 flex items-center gap-1.5">
              <n-icon :size="16"><SparklesOutline /></n-icon>
              AI动态计划调整
            </h3>
          </div>
          <p class="text-[13px] text-gray-400 mb-4">AI会分析你最近的打卡完成情况、体重变化和身体状况，智能调整后续计划</p>

          <!-- Adjustment feedback input -->
          <div class="mb-4">
            <n-input
              v-model:value="adjustFeedback"
              type="textarea"
              :rows="2"
              placeholder="请描述你的调整需求，例如：感觉强度太高了、想增加有氧运动、膝盖不舒服需要避免跑步..."
              :maxlength="500"
              show-count
            />
          </div>

          <n-button
            type="primary"
            size="small"
            :loading="adjusting"
            :disabled="adjusting || !adjustFeedback.trim()"
            @click="handleAdjustPlan"
          >
            分析打卡数据并调整计划
          </n-button>

          <!-- Adjustment streaming display -->
          <div v-if="adjustStreaming" class="mt-4 p-4 bg-gray-950/80 rounded-lg border border-blue-500/20 font-mono text-sm leading-relaxed text-gray-300 whitespace-pre-wrap">
            {{ adjustStreamText }}<span class="text-blue-400 animate-pulse">|</span>
          </div>

          <!-- Adjustment result (non-streaming) -->
          <div v-if="adjustResult && !adjustStreaming" class="mt-4 p-4 bg-blue-500/6 rounded-lg border border-blue-500/12">
            <div class="flex items-start gap-2.5 mb-4">
              <n-tag type="success" size="small">调整建议</n-tag>
              <span class="text-sm text-gray-300 leading-relaxed">{{ adjustResult.summary }}</span>
            </div>
            <div v-if="adjustResult.changes?.length">
              <h4 class="text-[13px] text-blue-400 mb-2.5">调整项</h4>
              <ul class="flex flex-col gap-2 list-none m-0 p-0">
                <li
                  v-for="(change, i) in adjustResult.changes"
                  :key="i"
                  class="flex items-start gap-2 p-2 bg-gray-950 rounded-md border border-gray-800 text-[13px] text-gray-300 leading-relaxed"
                >
                  <n-tag
                    size="small"
                    :type="change.type === 'increase' ? 'success' : change.type === 'decrease' ? 'warning' : 'info'"
                  >
                    {{ change.type === 'increase' ? '增加' : change.type === 'decrease' ? '减少' : '调整' }}
                  </n-tag>
                  <span>{{ change.description }}</span>
                  <em class="text-gray-400 text-xs not-italic ml-auto shrink-0">{{ change.reason }}</em>
                </li>
              </ul>
            </div>
          </div>
        </n-card>

        <!-- Plan Feedback Submission -->
        <n-card class="mt-5">
          <template #header><span>计划反馈</span></template>
          <p class="text-gray-400 text-[13px] mb-4">对这份 AI 计划有什么感受？你的反馈将帮助 AI 优化后续计划。</p>
          <n-form :model="feedbackForm" label-placement="left" label-width="80">
            <n-form-item label="评分">
              <n-rate v-model:value="feedbackForm.rating" :count="5" />
            </n-form-item>
            <n-form-item label="反馈内容">
              <n-input
                v-model:value="feedbackForm.content"
                type="textarea"
                :rows="3"
                placeholder="分享你的使用体验或改进建议..."
                :maxlength="500"
                show-count
              />
            </n-form-item>
            <n-form-item>
              <n-button
                type="primary"
                :loading="feedbackSubmitting"
                :disabled="feedbackSubmitted"
                @click="handleSubmitFeedback"
              >
                {{ feedbackSubmitted ? '已提交，感谢反馈' : '提交反馈' }}
              </n-button>
            </n-form-item>
          </n-form>
        </n-card>

        <!-- User Feedbacks List (Group E) -->
        <n-card class="mt-5">
          <template #header><span>用户反馈</span></template>
          <n-spin :show="feedbacksLoading">
            <div v-if="planFeedbacks.length > 0" class="flex flex-col gap-3">
              <n-card
                v-for="fb in planFeedbacks"
                :key="fb.id"
                size="small"
                class="bg-gray-900/40"
              >
                <div class="flex items-center justify-between mb-2">
                  <n-space align="center" :size="8">
                    <n-rate :value="fb.rating" :count="5" readonly size="small" />
                    <n-tag size="small" :type="fb.rating >= 4 ? 'success' : fb.rating >= 3 ? 'warning' : 'error'">
                      {{ fb.rating }}分
                    </n-tag>
                  </n-space>
                  <span class="text-xs text-gray-500">{{ fb.createdAt }}</span>
                </div>
                <p class="text-sm text-gray-300 m-0">{{ fb.content }}</p>
              </n-card>
            </div>
            <n-empty v-else description="暂无反馈记录" />
          </n-spin>
        </n-card>
      </div>
    </n-spin>

    <!-- Edit Plan Content Modal (Group A) -->
    <n-modal
      v-model:show="showEditContentModal"
      preset="card"
      title="编辑计划内容"
      style="max-width: 720px;"
      :mask-closable="false"
    >
      <n-input
        v-model:value="editContentText"
        type="textarea"
        :rows="16"
        placeholder="编辑计划的 AI 内容（JSON 格式）..."
        class="font-mono"
      />
      <template #footer>
        <n-space justify="end">
          <n-button @click="showEditContentModal = false">取消</n-button>
          <n-button type="primary" :loading="editContentSaving" @click="handleSavePlanContent">保存</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- Replace Day Items Modal (Group A) -->
    <n-modal
      v-model:show="showReplaceDayModal"
      preset="card"
      :title="`替换 Day ${replaceDayIdx >= 0 ? planDays[replaceDayIdx]?.d : ''} 的所有任务`"
      style="max-width: 600px;"
      :mask-closable="false"
    >
      <p class="text-sm text-gray-400 mb-3">每行一个任务，替换后原有任务将被覆盖。</p>
      <n-input
        v-model:value="replaceDayText"
        type="textarea"
        :rows="10"
        placeholder="每行输入一个任务..."
      />
      <template #footer>
        <n-space justify="end">
          <n-button @click="showReplaceDayModal = false">取消</n-button>
          <n-button type="primary" :loading="replaceDaySaving" @click="handleReplaceDayItems">确认替换</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import {
  NButton, NCard, NCheckbox, NEmpty, NForm, NFormItem, NIcon, NInput, NProgress,
  NRate, NSpin, NTag, NModal, NSpace, NDivider, NGrid, NGi
} from 'naive-ui';
import { ArrowBackOutline, CheckmarkOutline, SparklesOutline } from '@vicons/ionicons5';
import {
  fetchGetPlanDetail, fetchAdjustPlan, fetchCompleteTask, fetchSubmitPlanFeedback,
  fetchUpdateDayItem, fetchReplaceDayItems, fetchUpdatePlanContent,
  fetchGetPlanFeedbacksByPlanId, fetchGetMyPlanFeedbacks
} from '@/service/api';
import { createSSEStream } from '@/utils/sseClient';
import MedicalDisclaimerBanner from '@/components/MedicalDisclaimerBanner.vue';
import { usePlanStore } from '@/store/modules/plan';

defineOptions({ name: 'PlanDetail' });

interface PhaseItem {
  type: string;
  name: string;
  minutes: number;
  instruction?: string;
}

interface StructuredItem {
  name: string;
  durationMin: number;
  phases: PhaseItem[];
  id?: number;
}

interface PlanDay {
  d: number;
  items: (string | StructuredItem)[];
}

interface PlanData {
  id: number | string;
  planName: string;
  planType: string;
  status: number;
  durationDays: number;
  startDate: string;
  aiContent?: string;
}

interface AdjustChange {
  type: string;
  description: string;
  reason: string;
}

interface AdjustResult {
  summary: string;
  changes?: AdjustChange[];
}

const route = useRoute();
const router = useRouter();
const planStore = usePlanStore();
const pageLoading = ref(false);
const adjusting = ref(false);
const adjustResult = ref<AdjustResult | null>(null);
const adjustFeedback = ref('');
const adjustStreaming = ref(false);
const adjustStreamText = ref('');

// Task completion tracking: Map of "dayIdx-itemIdx" -> completed boolean
const completedTaskKeys = ref<Set<string>>(new Set());

// Inline editing state
const editingTask = ref<{ dayIdx: number; itemIdx: number } | null>(null);
const editText = ref('');

// Feedback state
const feedbackForm = ref({ rating: 0, content: '' });
const feedbackSubmitting = ref(false);
const feedbackSubmitted = ref(false);

// Edit plan content modal state
const showEditContentModal = ref(false);
const editContentText = ref('');
const editContentSaving = ref(false);

// Replace day items modal state
const showReplaceDayModal = ref(false);
const replaceDayIdx = ref<number>(-1);
const replaceDayText = ref('');
const replaceDaySaving = ref(false);

// Plan feedbacks list
const planFeedbacks = ref<Api.Plan.PlanFeedback[]>([]);
const feedbacksLoading = ref(false);

// Use Pinia store for plan data
const plan = computed(() => planStore.currentPlan as PlanData | null);
const planDays = computed(() => planStore.currentPlanDays as PlanDay[]);

// Plan type display helpers
const planTypeLabel = computed(() => {
  const map: Record<string, string> = {
    sport: '运动计划',
    diet: '饮食计划',
    comprehensive: '综合计划',
    rehabilitation: '康复计划',
    meditation: '冥想放松'
  };
  return map[plan.value?.planType || ''] || '健康计划';
});

const planTypeTagType = computed(() => {
  const map: Record<string, 'success' | 'info' | 'warning' | 'error'> = {
    sport: 'success',
    diet: 'info',
    comprehensive: 'warning',
    rehabilitation: 'error',
    meditation: 'info'
  };
  return map[plan.value?.planType || ''] || 'info';
});

// Progress tracking
const totalTasks = computed(() => {
  let count = 0;
  planDays.value.forEach(day => {
    count += day.items.length;
  });
  return count;
});

const completedTasks = computed(() => completedTaskKeys.value.size);

const completionPercentage = computed(() => {
  if (totalTasks.value === 0) return 0;
  return Math.round((completedTasks.value / totalTasks.value) * 100);
});

function dayTaskCount(dayIdx: number): number {
  return planDays.value[dayIdx]?.items.length || 0;
}

function dayCompletedCount(dayIdx: number): number {
  let count = 0;
  const day = planDays.value[dayIdx];
  if (!day) return 0;
  for (let i = 0; i < day.items.length; i++) {
    if (completedTaskKeys.value.has(`${dayIdx}-${i}`)) count++;
  }
  return count;
}

function isTaskCompleted(dayIdx: number, itemIdx: number): boolean {
  return completedTaskKeys.value.has(`${dayIdx}-${itemIdx}`);
}

// Watch store version changes for UI updates (e.g., AI Copilot hot update)
watch(() => planStore.planVersion, (newVer, oldVer) => {
  if (oldVer !== undefined && oldVer > 0) {
    window.$message?.success('计划已更新');
  }
});

/** Check if item is a structured object with phases */
function isPhaseItem(item: string | StructuredItem): item is StructuredItem {
  return (
    item !== null &&
    typeof item === 'object' &&
    'phases' in item &&
    Array.isArray(item.phases) &&
    item.phases.length > 0
  );
}

/** Get tag type for phase type */
function phaseTagType(phaseType: string): 'warning' | 'error' | 'success' | 'info' {
  switch (phaseType) {
    case 'warmup': return 'warning';
    case 'core': return 'error';
    case 'cooldown': return 'success';
    default: return 'info';
  }
}

/** Get Chinese label for phase type */
function phaseTypeLabel(phaseType: string): string {
  switch (phaseType) {
    case 'warmup': return '热身';
    case 'core': return '核心';
    case 'cooldown': return '放松';
    default: return phaseType;
  }
}

function formatDayDate(dayNum: number): string {
  if (!plan.value?.startDate) return '';
  const start = new Date(plan.value.startDate);
  start.setDate(start.getDate() + dayNum - 1);
  const m = String(start.getMonth() + 1).padStart(2, '0');
  const d = String(start.getDate()).padStart(2, '0');
  return `${m}-${d}`;
}

/** Mark a task as completed */
async function handleCompleteTask(dayIdx: number, itemIdx: number, item: string | StructuredItem): Promise<void> {
  const key = `${dayIdx}-${itemIdx}`;
  if (completedTaskKeys.value.has(key)) return;

  // Get task detail ID - structured items may have an id, otherwise use a generated ID
  let detailId: number | undefined;
  if (typeof item === 'object' && item !== null && 'id' in item && item.id) {
    detailId = item.id as number;
  }

  try {
    if (detailId) {
      await fetchCompleteTask(detailId);
    }
    completedTaskKeys.value.add(key);
    // Trigger reactivity
    completedTaskKeys.value = new Set(completedTaskKeys.value);

    // Update store to trigger version bump
    planStore.updateDayItem(dayIdx, { completed: true } as any);

    window.$message?.success('任务已完成！');
  } catch {
    // handled by interceptor
    window.$message?.error('打卡失败，请重试');
  }
}

/** Start inline editing a task */
function startEditTask(dayIdx: number, itemIdx: number, item: string | StructuredItem): void {
  editingTask.value = { dayIdx, itemIdx };
  editText.value = typeof item === 'string' ? item : (item as StructuredItem).name || '';
}

/** Save edited task — persists to backend via fetchUpdateDayItem */
async function saveEditTask(): Promise<void> {
  if (!editingTask.value || !editText.value.trim()) {
    cancelEditTask();
    return;
  }

  const { dayIdx, itemIdx } = editingTask.value;
  const day = planDays.value[dayIdx];
  if (!day || !plan.value) {
    cancelEditTask();
    return;
  }

  const originalItem = day.items[itemIdx];
  let updatedItem: Record<string, unknown>;

  if (typeof originalItem === 'string') {
    updatedItem = { name: editText.value.trim() };
  } else if (isPhaseItem(originalItem as StructuredItem)) {
    updatedItem = { ...(originalItem as StructuredItem), name: editText.value.trim() } as unknown as Record<string, unknown>;
  } else {
    updatedItem = { name: editText.value.trim() };
  }

  try {
    await fetchUpdateDayItem(plan.value.id as number, dayIdx, itemIdx, updatedItem);

    // Update local store
    if (typeof originalItem === 'string') {
      const newItems = [...day.items];
      newItems[itemIdx] = editText.value.trim();
      planStore.replaceDayItems(dayIdx, { ...day, items: newItems });
    } else if (isPhaseItem(originalItem as StructuredItem)) {
      const newItems = [...day.items];
      newItems[itemIdx] = { ...(originalItem as StructuredItem), name: editText.value.trim() };
      planStore.replaceDayItems(dayIdx, { ...day, items: newItems });
    }

    editingTask.value = null;
    editText.value = '';
    window.$message?.success('已更新');
  } catch {
    window.$message?.error('保存失败，请重试');
  }
}

/** Cancel inline editing */
function cancelEditTask(): void {
  editingTask.value = null;
  editText.value = '';
}

function handlePrintPreview(): void {
  const title = plan.value?.planName || 'AI健康计划';
  const typeLabel = planTypeLabel.value;
  let content = `<html><head><meta charset="utf-8"><title>${title}</title>
<style>
body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;background:#0d1117;color:#e6edf3;padding:32px;margin:0}
h1{font-size:22px;color:#58a6ff;margin-bottom:4px}
.meta{font-size:13px;color:#8b949e;margin-bottom:24px}
.day-card{background:#161b22;border-radius:8px;padding:20px 24px;margin-bottom:16px;box-shadow:0 4px 16px rgba(0,0,0,0.25)}
.day-header{font-size:16px;font-weight:600;color:#58a6ff;margin-bottom:12px;border-bottom:1px solid #30363d;padding-bottom:8px}
ul{list-style:none;padding:0;margin:0}
li{font-size:14px;padding:6px 0;color:#e6edf3;display:flex;align-items:flex-start;gap:8px}
li::before{content:'✓';color:#3fb950;flex-shrink:0}
</style></head><body>
<h1>${title}</h1>
<div class="meta">${typeLabel} · ${plan.value?.durationDays}天 · 开始日期：${plan.value?.startDate}</div>`;

  planDays.value.forEach(day => {
    content += `<div class="day-card"><div class="day-header">Day ${day.d} — ${formatDayDate(day.d)}</div><ul>`;
    day.items.forEach(item => {
      content += `<li>${typeof item === 'string' ? item : item.name}</li>`;
    });
    content += '</ul></div>';
  });

  content += '</body></html>';

  const blob = new Blob([content], { type: 'text/html;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `${title}.html`;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}

let adjustStreamControl: any = null;

/** Handle plan adjustment with feedback parameter */
async function handleAdjustPlan(): Promise<void> {
  if (!plan.value || !adjustFeedback.value.trim()) return;

  adjusting.value = true;
  adjustResult.value = null;
  adjustStreaming.value = false;
  adjustStreamText.value = '';

  const feedback = adjustFeedback.value.trim();

  // Try SSE streaming adjustment first, fall back to regular API
  try {
    adjustStreamControl = createSSEStream('/ai-plan/adjust-stream', {
      originalPlanId: plan.value.id as number,
      feedback
    }, {
      onMessage: (text: string) => {
        if (text === '[DONE]') {
          adjustStreaming.value = false;
          // Parse the streamed text as adjustment result
          try {
            const parsed = JSON.parse(adjustStreamText.value);
            adjustResult.value = {
              summary: parsed.summary || '调整建议已生成',
              changes: parsed.changes || []
            };
          } catch {
            // If not JSON, just show as summary
            adjustResult.value = { summary: adjustStreamText.value || '调整建议已生成' };
          }
          return;
        }
        if (text === '[ERROR]') {
          adjustStreaming.value = false;
          window.$message?.error('AI调整失败，请重试');
          return;
        }
        adjustStreamText.value += text;
      },
      onDone: () => {
        adjusting.value = false;
        adjustStreaming.value = false;
        window.$message?.success('计划调整建议已生成');
      },
      onError: () => {
        adjusting.value = false;
        adjustStreaming.value = false;
      }
    });

    // Show streaming UI
    adjustStreaming.value = true;

    // Wait for the stream to complete or timeout
    await Promise.race([
      adjustStreamControl.promise,
      new Promise<void>((_, reject) => setTimeout(() => reject(new Error('timeout')), 30000))
    ]);
  } catch {
    // SSE not available, fall back to regular API call
    adjustStreaming.value = false;
    try {
      const { data } = await fetchAdjustPlan({
        originalPlanId: plan.value.id as number,
        feedback
      });
      if (data) {
        adjustResult.value = {
          summary: (data as any).text || (data as any).summary || '调整建议已生成',
          changes: (data as any).widgets?.map((w: any) => ({
            type: w.type || 'adjust',
            description: w.title || w.description || '',
            reason: w.detail || w.reason || ''
          })) || (data as any).changes || []
        };
        window.$message?.success('计划调整建议已生成');
      }
    } catch {
      // handled by interceptor
    }
  } finally {
    adjusting.value = false;
    adjustStreaming.value = false;
  }
}

async function handleSubmitFeedback(): Promise<void> {
  if (!feedbackForm.value.rating) {
    window.$message?.warning('请先评分');
    return;
  }
  if (!plan.value) return;
  feedbackSubmitting.value = true;
  try {
    await fetchSubmitPlanFeedback({
      planId: plan.value.id as number,
      rating: feedbackForm.value.rating,
      content: feedbackForm.value.content
    });
    feedbackSubmitted.value = true;
    window.$message?.success('感谢你的反馈！');
    // Reload feedbacks after submitting
    loadPlanFeedbacks();
  } catch {
    // handled by interceptor
  } finally {
    feedbackSubmitting.value = false;
  }
}

/** Open the edit plan content modal */
function openEditContentModal(): void {
  editContentText.value = plan.value?.aiContent || '';
  showEditContentModal.value = true;
}

/** Save plan content via fetchUpdatePlanContent */
async function handleSavePlanContent(): Promise<void> {
  if (!plan.value) return;
  editContentSaving.value = true;
  try {
    await fetchUpdatePlanContent(plan.value.id as number, { plan: editContentText.value });
    showEditContentModal.value = false;
    window.$message?.success('计划内容已更新');
    // Reload the plan to reflect changes
    const { data: planData } = await fetchGetPlanDetail(Number(route.params.id));
    const days: PlanDay[] = [];
    if (planData?.aiContent) {
      try {
        const parsed = JSON.parse(planData.aiContent);
        days.push(...(parsed.days || []));
      } catch { /* ignore */ }
    }
    if (planData?.days && days.length === 0) {
      days.push(...planData.days.map((d: any) => ({
        d: d.day || d.d || 0,
        items: d.tasks?.map((t: any) => t.description || t) || d.items || []
      })));
    }
    planStore.setPlan(planData as any, days);
  } catch {
    window.$message?.error('更新计划内容失败');
  } finally {
    editContentSaving.value = false;
  }
}

/** Open the replace day items modal for a specific day */
function openReplaceDayModal(dayIdx: number): void {
  const day = planDays.value[dayIdx];
  if (!day) return;
  replaceDayIdx.value = dayIdx;
  replaceDayText.value = day.items
    .map(item => typeof item === 'string' ? item : item.name)
    .join('\n');
  showReplaceDayModal.value = true;
}

/** Save replaced day items via fetchReplaceDayItems */
async function handleReplaceDayItems(): Promise<void> {
  if (!plan.value || replaceDayIdx.value < 0) return;
  replaceDaySaving.value = true;
  try {
    const lines = replaceDayText.value.split('\n').filter(l => l.trim());
    const items = lines.map(line => ({ name: line.trim() }));
    await fetchReplaceDayItems(plan.value.id as number, replaceDayIdx.value, items);
    // Update local store
    const day = planDays.value[replaceDayIdx.value];
    if (day) {
      planStore.replaceDayItems(replaceDayIdx.value, { ...day, items: lines.map(l => l.trim()) });
    }
    showReplaceDayModal.value = false;
    window.$message?.success('当天任务已替换');
  } catch {
    window.$message?.error('替换任务失败');
  } finally {
    replaceDaySaving.value = false;
  }
}

/** Load feedbacks for this plan */
async function loadPlanFeedbacks(): Promise<void> {
  if (!plan.value) return;
  feedbacksLoading.value = true;
  try {
    const { data } = await fetchGetPlanFeedbacksByPlanId(plan.value.id as number);
    planFeedbacks.value = data || [];
  } catch {
    // silently fail
  } finally {
    feedbacksLoading.value = false;
  }
}

onMounted(async () => {
  pageLoading.value = true;
  try {
    const { data: planData } = await fetchGetPlanDetail(Number(route.params.id));
    const days: PlanDay[] = [];
    if (planData?.aiContent) {
      try {
        const parsed = JSON.parse(planData.aiContent);
        days.push(...(parsed.days || []));
      } catch { /* ignore parse errors */ }
    }
    // Also check structured days from the API response
    if (planData?.days && days.length === 0) {
      days.push(...planData.days.map((d: any) => ({
        d: d.day || d.d || 0,
        items: d.tasks?.map((t: any) => t.description || t) || d.items || []
      })));
    }
    planStore.setPlan(planData as any, days);
    // Load existing feedbacks for this plan
    loadPlanFeedbacks();
  } finally {
    pageLoading.value = false;
  }
});

onBeforeUnmount(() => {
  adjustStreamControl?.abort();
  adjustStreamControl = null;
});
</script>
