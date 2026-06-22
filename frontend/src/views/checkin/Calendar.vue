<template>
  <NSpin :show="pageLoading">
    <div class="checkin-page flex flex-col gap-4 p-1">
      <!-- Stats Bar -->
      <NCard :bordered="false" class="stats-bar">
        <div class="flex items-center justify-center gap-6">
          <div class="flex flex-col items-center gap-1">
            <span class="text-2xl font-bold font-tabular text-green">{{ stats.consecutiveDays }}</span>
            <span class="text-xs text-secondary">{{ $t('checkin.consecutiveDays') || '连续打卡' }}</span>
          </div>
          <div class="h-9 w-px bg-[#30363d]" />
          <div class="flex flex-col items-center gap-1">
            <span class="text-2xl font-bold font-tabular">{{ stats.totalDays }}</span>
            <span class="text-xs text-secondary">{{ $t('checkin.totalDays') || '累计天数' }}</span>
          </div>
          <div class="h-9 w-px bg-[#30363d]" />
          <div class="flex flex-col items-center gap-1">
            <span class="text-2xl font-bold font-tabular">{{ stats.currentWeekDays }}</span>
            <span class="text-xs text-secondary">{{ $t('checkin.currentWeekDays') || '本周打卡' }}</span>
          </div>
          <div class="h-9 w-px bg-[#30363d]" />
          <div class="flex flex-col items-center gap-1">
            <span class="text-2xl font-bold font-tabular">{{ stats.currentMonthDays }}</span>
            <span class="text-xs text-secondary">{{ $t('checkin.currentMonthDays') || '本月打卡' }}</span>
          </div>
          <div class="h-9 w-px bg-[#30363d]" />
          <div class="flex flex-col items-center gap-1">
            <span class="text-2xl font-bold font-tabular text-green">{{ stats.exerciseCompleteRate }}%</span>
            <span class="text-xs text-secondary">{{ $t('checkin.exerciseCompleteRate') || '运动完成率' }}</span>
          </div>
          <div class="h-9 w-px bg-[#30363d]" />
          <div class="flex flex-col items-center gap-1">
            <span class="text-2xl font-bold font-tabular text-[#58a6ff]">{{ stats.dietCompleteRate }}%</span>
            <span class="text-xs text-secondary">{{ $t('checkin.dietCompleteRate') || '饮食完成率' }}</span>
          </div>
        </div>
      </NCard>

      <!-- Navigation Links -->
      <NCard :bordered="false">
        <div class="flex flex-wrap items-center justify-center gap-3">
          <NButton text type="primary" @click="router.push('/food-record')">
            {{ $t('checkin.foodRecord') || '饮食记录' }}
          </NButton>
          <NButton text type="primary" @click="router.push('/exercise-record')">
            {{ $t('checkin.exerciseRecord') || '运动记录' }}
          </NButton>
          <NButton text type="primary" @click="router.push('/sleep-record')">
            {{ $t('checkin.sleepRecord') || '睡眠记录' }}
          </NButton>
          <NButton text type="primary" @click="router.push('/water-record')">
            {{ $t('checkin.waterRecord') || '饮水记录' }}
          </NButton>
          <NButton text type="primary" @click="router.push('/body-measurement')">
            {{ $t('checkin.bodyMeasurement') || '身体围度' }}
          </NButton>
          <NButton text type="primary" @click="router.push('/statistics')">
            {{ $t('checkin.statistics') || '数据统计' }}
          </NButton>
        </div>
      </NCard>

      <!-- Badges -->
      <NCard v-if="badges.length > 0" :bordered="false" class="badges-section">
        <div class="mb-3 text-[15px] font-semibold">{{ $t('checkin.badges') || '打卡成就' }}</div>
        <div class="flex flex-wrap gap-2.5">
          <div
            v-for="badge in badges"
            :key="badge.name"
            class="flex flex-col items-center gap-0.5 rounded-lg border px-3.5 py-2.5 transition-all"
            :class="badge.unlocked
              ? 'border-[#3fb950] bg-[rgba(63,185,80,0.08)] opacity-100'
              : 'border-[#30363d] bg-[#161b22] opacity-35'"
          >
            <span class="text-[22px]">{{ badge.icon }}</span>
            <span class="whitespace-nowrap text-[11px] font-semibold">{{ badge.name }}</span>
            <span class="text-[10px] text-secondary">{{ badge.desc }}</span>
          </div>
        </div>
      </NCard>

      <!-- Heatmap -->
      <NCard :bordered="false" class="heatmap-card">
        <div class="mb-3 text-[15px] font-semibold">{{ $t('checkin.heatmap') || '打卡热力图(近一年)' }}</div>
        <div ref="heatmapRef" class="h-[170px] w-full" />
      </NCard>

      <!-- Main Row: Calendar + Today -->
      <div class="flex flex-col gap-4 md:flex-row">
        <!-- Calendar -->
        <NCard :bordered="false" class="min-w-0 flex-1">
          <NCalendar v-model:value="calendarTs" @update:value="onCalendarChange">
            <template #default="{ date }">
              <div
                class="date-cell relative flex min-h-[56px] cursor-pointer flex-col items-center rounded-lg border p-1.5 transition-all"
                :class="getDateCellClass(toDateStr(date))"
                @click="handleDateClick(toDateStr(date))"
              >
                <span class="text-sm font-semibold">{{ formatDateNum(date) }}</span>
                <span class="mt-1 flex gap-0.5">
                  <span
                    v-if="getExerciseDot(toDateStr(date))"
                    class="dot h-1.5 w-1.5 rounded-full"
                    :class="getExerciseDot(toDateStr(date))"
                  />
                  <span
                    v-if="getDietDot(toDateStr(date))"
                    class="dot h-1.5 w-1.5 rounded-full"
                    :class="getDietDot(toDateStr(date))"
                  />
                </span>
              </div>
            </template>
          </NCalendar>
        </NCard>

        <!-- Today Card -->
        <NCard :bordered="false" class="w-full shrink-0 md:w-[340px]">
          <div class="mb-5 flex items-center justify-between">
            <span class="text-base font-semibold">{{ isTodayCheckedIn ? ($t('checkin.todayRecord') || '今日打卡记录') : ($t('checkin.todayCheckin') || '今日打卡') }}</span>
            <NTag v-if="todayRecord" :type="todayTagType" size="small">{{ todayTagText }}</NTag>
          </div>

          <!-- Submit Form -->
          <NForm
            v-if="!isTodayCheckedIn"
            ref="submitFormRef"
            :model="submitForm"
            :rules="submitRules"
            label-placement="top"
            @submit.prevent="handleSubmit"
          >
            <NFormItem :label="$t('checkin.exerciseStatus') || '运动完成'" path="exerciseStatus">
              <NRadioGroup v-model:value="submitForm.exerciseStatus">
                <NRadioButton :value="0">{{ $t('checkin.notCompleted') || '未完成' }}</NRadioButton>
                <NRadioButton :value="1">{{ $t('checkin.partiallyCompleted') || '部分完成' }}</NRadioButton>
                <NRadioButton :value="2">{{ $t('checkin.fullyCompleted') || '全部完成' }}</NRadioButton>
              </NRadioGroup>
            </NFormItem>

            <NFormItem :label="$t('checkin.dietStatus') || '饮食完成'" path="dietStatus">
              <NRadioGroup v-model:value="submitForm.dietStatus">
                <NRadioButton :value="0">{{ $t('checkin.notCompleted') || '未完成' }}</NRadioButton>
                <NRadioButton :value="1">{{ $t('checkin.partiallyCompleted') || '部分完成' }}</NRadioButton>
                <NRadioButton :value="2">{{ $t('checkin.fullyCompleted') || '全部完成' }}</NRadioButton>
              </NRadioGroup>
            </NFormItem>

            <NFormItem :label="$t('checkin.currentWeight') || '当前体重 (kg)'">
              <NInputNumber
                v-model:value="submitForm.currentWeight"
                :min="30"
                :max="300"
                :precision="1"
                :show-button="false"
                :placeholder="$t('common.optional') || '选填'"
                class="w-full"
              />
            </NFormItem>

            <NFormItem :label="$t('checkin.mood') || '心情'">
              <NSelect
                v-model:value="submitForm.mood"
                :options="moodOptions"
                :placeholder="$t('common.optional') || '选填'"
                clearable
              />
            </NFormItem>

            <NFormItem :label="$t('checkin.note') || '备注'">
              <NInput
                v-model:value="submitForm.note"
                type="textarea"
                :rows="2"
                :maxlength="200"
                show-count
                :placeholder="$t('checkin.notePlaceholder') || '记录今天的感受...'"
              />
            </NFormItem>

            <NButton type="primary" attr-type="submit" :loading="submitting" :disabled="submitting" block size="large">
              {{ $t('checkin.submit') || '提交打卡' }}
            </NButton>
          </NForm>

          <!-- Today Record Display -->
          <div v-else-if="todayRecord" class="flex flex-col gap-3.5">
            <div class="flex items-center justify-between">
              <span class="text-[13px] text-secondary">{{ $t('checkin.exercise') || '运动' }}</span>
              <NTag :type="todayRecord.exerciseStatus === 2 ? 'success' : todayRecord.exerciseStatus === 1 ? 'warning' : 'default'" size="small">
                {{ statusLabel(todayRecord.exerciseStatus) }}
              </NTag>
            </div>
            <div class="flex items-center justify-between">
              <span class="text-[13px] text-secondary">{{ $t('checkin.diet') || '饮食' }}</span>
              <NTag :type="todayRecord.dietStatus === 2 ? 'success' : todayRecord.dietStatus === 1 ? 'warning' : 'default'" size="small">
                {{ statusLabel(todayRecord.dietStatus) }}
              </NTag>
            </div>
            <div v-if="todayRecord.currentWeight" class="flex items-center justify-between">
              <span class="text-[13px] text-secondary">{{ $t('checkin.weight') || '体重' }}</span>
              <span class="text-sm">{{ todayRecord.currentWeight }} kg</span>
            </div>
            <div v-if="todayRecord.mood" class="flex items-center justify-between">
              <span class="text-[13px] text-secondary">{{ $t('checkin.mood') || '心情' }}</span>
              <span class="text-sm">{{ todayRecord.mood }}</span>
            </div>
            <div v-if="todayRecord.note" class="flex items-center justify-between">
              <span class="text-[13px] text-secondary">{{ $t('checkin.note') || '备注' }}</span>
              <span class="text-sm">{{ todayRecord.note }}</span>
            </div>
          </div>
        </NCard>
      </div>

      <!-- History Section -->
      <NCard :bordered="false">
        <template #header>
          <div class="flex items-center justify-between">
            <span class="text-[15px] font-semibold">{{ $t('checkin.history') || '打卡历史' }}</span>
            <div class="flex items-center gap-2">
              <NDatePicker
                v-model:formatted-value="historyDateRange"
                type="daterange"
                value-format="yyyy-MM-dd"
                :shortcuts="dateRangeShortcuts"
                clearable
                class="w-[280px]"
                @update:formatted-value="loadHistory"
              />
              <NButton size="small" @click="resetHistoryFilter">
                {{ $t('common.reset') || '重置' }}
              </NButton>
            </div>
          </div>
        </template>

        <NDataTable
          :columns="historyColumns"
          :data="historyRecords"
          :loading="historyLoading"
          :bordered="false"
          :row-key="(row: CheckinRecord) => row.id"
          striped
        />

        <div v-if="historyTotal > 0" class="mt-4 flex justify-end">
          <NPagination
            v-model:page="historyPage"
            :page-count="Math.ceil(historyTotal / historyPageSize)"
            :page-size="historyPageSize"
            show-size-picker
            :page-sizes="[10, 20, 50]"
            @update:page="loadHistory"
            @update:page-size="handlePageSizeChange"
          />
        </div>
      </NCard>

      <!-- Supplement Modal -->
      <NModal
        v-model:show="supplementVisible"
        preset="dialog"
        :title="$t('checkin.supplement') || '补卡'"
        :positive-text="$t('checkin.confirmSupplement') || '确认补卡'"
        :negative-text="$t('common.cancel') || '取消'"
        :style="{ width: '440px' }"
        :mask-closable="false"
        @positive-click="handleSupplementSubmit"
      >
        <div class="mb-4 flex items-center gap-3 rounded-lg bg-[rgba(88,166,255,0.06)] px-3.5 py-2.5">
          <span class="text-[13px] text-secondary">补卡日期</span>
          <span class="text-[15px] font-semibold">{{ supplementDate }}</span>
        </div>
        <NForm ref="supplementFormRef" :model="supplementForm" :rules="supplementRules" label-placement="top">
          <NFormItem label="运动完成" path="exerciseStatus">
            <NRadioGroup v-model:value="supplementForm.exerciseStatus">
              <NRadioButton :value="0">未完成</NRadioButton>
              <NRadioButton :value="1">部分完成</NRadioButton>
              <NRadioButton :value="2">全部完成</NRadioButton>
            </NRadioGroup>
          </NFormItem>
          <NFormItem label="饮食完成" path="dietStatus">
            <NRadioGroup v-model:value="supplementForm.dietStatus">
              <NRadioButton :value="0">未完成</NRadioButton>
              <NRadioButton :value="1">部分完成</NRadioButton>
              <NRadioButton :value="2">全部完成</NRadioButton>
            </NRadioGroup>
          </NFormItem>
          <NFormItem label="当前体重 (kg)">
            <NInputNumber v-model:value="supplementForm.currentWeight" :min="30" :max="300" :precision="1" :show-button="false" placeholder="选填" class="w-full" />
          </NFormItem>
          <NFormItem label="心情">
            <NSelect v-model:value="supplementForm.mood" :options="moodOptions" placeholder="选填" clearable />
          </NFormItem>
          <NFormItem label="备注">
            <NInput v-model:value="supplementForm.note" type="textarea" :rows="2" :maxlength="200" show-count placeholder="记录当天的感受..." />
          </NFormItem>
          <NFormItem label="补卡原因" path="reason">
            <NInput
              v-model:value="supplementForm.reason"
              type="textarea"
              :rows="2"
              :maxlength="200"
              show-count
              placeholder="请说明补卡原因..."
            />
          </NFormItem>
        </NForm>
      </NModal>
    </div>
  </NSpin>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick, h } from 'vue';
import { useRouter } from 'vue-router';
import {
  useMessage, NCard, NCalendar, NForm, NFormItem, NRadioGroup, NRadioButton,
  NInputNumber, NSelect, NInput, NButton, NTag, NModal, NDataTable, NPagination,
  NDatePicker, NSpin, type DataTableColumns
} from 'naive-ui';
import * as echarts from 'echarts/core';
import { HeatmapChart } from 'echarts/charts';
import { CalendarComponent, TooltipComponent, VisualMapComponent } from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import {
  fetchSubmitCheckin, fetchSupplementCheckin, fetchGetCheckinList,
  fetchGetCheckinStats, fetchGetCheckinPage, fetchGetTodayCheckin
} from '@/service/api';

echarts.use([HeatmapChart, CalendarComponent, TooltipComponent, VisualMapComponent, CanvasRenderer]);

defineOptions({ name: 'CheckinCalendar' });
const message = useMessage();
const router = useRouter();

const today = new Date().toISOString().split('T')[0];
const DAY_MS = 86400000;

const calendarTs = ref<number>(Date.now());
const pageLoading = ref(false);
const submitting = ref(false);
const submitFormRef = ref();
const supplementFormRef = ref();

const stats = reactive({
  consecutiveDays: 0,
  totalDays: 0,
  currentWeekDays: 0,
  currentMonthDays: 0,
  exerciseCompleteRate: 0,
  dietCompleteRate: 0
});

type CheckinRecord = Api.Checkin.CheckinRecord;

const records = ref<Record<string, CheckinRecord>>({});
const todayRecord = ref<CheckinRecord | null>(null);
const supplementVisible = ref(false);
const supplementDate = ref('');
const heatmapRef = ref<HTMLDivElement | null>(null);
let heatmapChart: echarts.ECharts | null = null;

// History pagination
const historyRecords = ref<CheckinRecord[]>([]);
const historyLoading = ref(false);
const historyPage = ref(1);
const historyTotal = ref(0);
const historyPageSize = ref(10);
const historyDateRange = ref<[string, string] | null>(null);

const dateRangeShortcuts = {
  '近7天': () => {
    const end = new Date();
    const start = new Date();
    start.setDate(start.getDate() - 7);
    return [start.getTime(), end.getTime()] as [number, number];
  },
  '近30天': () => {
    const end = new Date();
    const start = new Date();
    start.setDate(start.getDate() - 30);
    return [start.getTime(), end.getTime()] as [number, number];
  },
  '近90天': () => {
    const end = new Date();
    const start = new Date();
    start.setDate(start.getDate() - 90);
    return [start.getTime(), end.getTime()] as [number, number];
  }
};

const moodOptions = [
  { label: '😄 开心', value: '开心' },
  { label: '😊 不错', value: '不错' },
  { label: '😐 一般', value: '一般' },
  { label: '😞 低落', value: '低落' },
  { label: '😤 烦躁', value: '烦躁' }
];

const submitForm = reactive({
  planId: null as number | null,
  exerciseStatus: 0,
  dietStatus: 0,
  currentWeight: null as number | null,
  mood: '',
  note: ''
});

const supplementForm = reactive({
  planId: null as number | null,
  checkDate: '',
  exerciseStatus: 0,
  dietStatus: 0,
  currentWeight: null as number | null,
  mood: '',
  note: '',
  reason: ''
});

const submitRules = {
  exerciseStatus: { type: 'number' as const, required: true, message: '请选择运动完成状态', trigger: 'change' },
  dietStatus: { type: 'number' as const, required: true, message: '请选择饮食完成状态', trigger: 'change' }
};

const supplementRules = {
  exerciseStatus: { type: 'number' as const, required: true, message: '请选择运动完成状态', trigger: 'change' },
  dietStatus: { type: 'number' as const, required: true, message: '请选择饮食完成状态', trigger: 'change' },
  reason: { type: 'string' as const, required: true, message: '请填写补卡原因', trigger: 'blur' }
};

const isTodayCheckedIn = computed(() => todayRecord.value !== null);

const todayTagType = computed<'success' | 'warning' | 'default'>(() => {
  if (!todayRecord.value) return 'default';
  const e = todayRecord.value.exerciseStatus;
  const d = todayRecord.value.dietStatus;
  if (e === 2 && d === 2) return 'success';
  if (e >= 1 || d >= 1) return 'warning';
  return 'default';
});

const todayTagText = computed(() => {
  if (!todayRecord.value) return '';
  const e = todayRecord.value.exerciseStatus;
  const d = todayRecord.value.dietStatus;
  if (e === 2 && d === 2) return '全部完成';
  if (e >= 1 || d >= 1) return '部分完成';
  return '未完成';
});

const badges = computed(() => {
  const list: Array<{ days: number; icon: string; name: string; desc: string; unlocked: boolean }> = [];
  const total = stats.totalDays;
  const streak = stats.consecutiveDays;
  const conditions = [
    { days: 3, icon: '🌱', name: '新手上路', desc: '累计打卡3天' },
    { days: 7, icon: '🔥', name: '一周打卡', desc: '累计打卡7天' },
    { days: 14, icon: '💪', name: '坚持两周', desc: '累计打卡14天' },
    { days: 30, icon: '⭐', name: '月度之星', desc: '累计打卡30天' },
    { days: 60, icon: '🏆', name: '健身达人', desc: '累计打卡60天' },
    { days: 100, icon: '👑', name: '百炼成钢', desc: '累计打卡100天' },
    { days: 180, icon: '💎', name: '半年坚持', desc: '累计打卡180天' },
    { days: 365, icon: '🌟', name: '年度传奇', desc: '累计打卡365天' }
  ];
  const streakConditions = [
    { days: 3, icon: '🔥', name: '连击3天', desc: '连续3天不中断' },
    { days: 7, icon: '🔥🔥', name: '一周连击', desc: '连续打卡7天' },
    { days: 14, icon: '🚀', name: '双周王者', desc: '连续打卡14天' },
    { days: 30, icon: '⚡', name: '月度全勤', desc: '连续打卡30天' },
    { days: 60, icon: '💥', name: '铁人模式', desc: '连续打卡60天' }
  ];
  conditions.forEach(c => list.push({ ...c, unlocked: total >= c.days }));
  streakConditions.forEach(c => list.push({ ...c, unlocked: streak >= c.days }));
  return list;
});

const historyColumns: DataTableColumns<CheckinRecord> = [
  {
    title: '日期',
    key: 'checkDate',
    width: 120,
    render(row) {
      return h('div', { class: 'flex items-center gap-2' }, [
        h('span', {}, row.checkDate),
        row.isSupplement === 1 ? h(NTag, { size: 'small', type: 'warning' }, { default: () => '补卡' }) : null
      ]);
    }
  },
  {
    title: '运动',
    key: 'exerciseStatus',
    width: 100,
    render(row) {
      const type = row.exerciseStatus === 2 ? 'success' : row.exerciseStatus === 1 ? 'warning' : 'default';
      return h(NTag, { size: 'small', type: type as any }, { default: () => statusLabel(row.exerciseStatus) });
    }
  },
  {
    title: '饮食',
    key: 'dietStatus',
    width: 100,
    render(row) {
      const type = row.dietStatus === 2 ? 'success' : row.dietStatus === 1 ? 'warning' : 'default';
      return h(NTag, { size: 'small', type: type as any }, { default: () => statusLabel(row.dietStatus) });
    }
  },
  {
    title: '体重(kg)',
    key: 'currentWeight',
    width: 90,
    render: (row) => row.currentWeight ? `${row.currentWeight}` : '-'
  },
  {
    title: '心情',
    key: 'mood',
    width: 80,
    render: (row) => row.mood || '-'
  },
  {
    title: '备注',
    key: 'note',
    ellipsis: { tooltip: true },
    render: (row) => row.note || '-'
  }
];

function toDateStr(date: string | number | Date): string {
  if (typeof date === 'string') return date;
  const d = new Date(date);
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

function formatDateNum(date: number | Date): string {
  const d = new Date(date);
  return String(d.getDate());
}

function statusLabel(val: number): string {
  if (val === 2) return '全部完成';
  if (val === 1) return '部分完成';
  return '未完成';
}

function getDateCellClass(dateStr: string): string {
  const record = records.value[dateStr];
  if (record) {
    if (record.exerciseStatus === 2 && record.dietStatus === 2) return 'cell-full';
    if (record.exerciseStatus >= 1 || record.dietStatus >= 1) return 'cell-partial';
    return 'cell-incomplete';
  }
  if (dateStr === today) return 'cell-today';
  if (dateStr < today) {
    return isSupplementable(dateStr) ? 'cell-can-supplement' : 'cell-missed';
  }
  return 'cell-future';
}

function isSupplementable(dateStr: string): boolean {
  if (dateStr >= today) return false;
  if (records.value[dateStr]) return false;
  const daysBetween = Math.floor((new Date(today).getTime() - new Date(dateStr).getTime()) / DAY_MS);
  return daysBetween > 0 && daysBetween <= 7;
}

function getExerciseDot(dateStr: string): string {
  const record = records.value[dateStr];
  if (!record) return '';
  if (record.exerciseStatus === 2) return 'dot-full';
  if (record.exerciseStatus === 1) return 'dot-partial';
  return 'dot-incomplete';
}

function getDietDot(dateStr: string): string {
  const record = records.value[dateStr];
  if (!record) return '';
  if (record.dietStatus === 2) return 'dot-full';
  if (record.dietStatus === 1) return 'dot-partial';
  return 'dot-incomplete';
}

function handleDateClick(dateVal: string) {
  if (dateVal > today) return;
  if (records.value[dateVal]) return;
  if (dateVal === today) return;

  const daysBetween = Math.floor((new Date(today).getTime() - new Date(dateVal).getTime()) / DAY_MS);
  if (daysBetween < 0 || daysBetween > 7) {
    message.warning('仅支持补卡过去7天内的日期');
    return;
  }

  supplementDate.value = dateVal;
  supplementForm.planId = submitForm.planId;
  supplementForm.checkDate = dateVal;
  supplementForm.exerciseStatus = 0;
  supplementForm.dietStatus = 0;
  supplementForm.currentWeight = null;
  supplementForm.mood = '';
  supplementForm.note = '';
  supplementForm.reason = '';
  supplementVisible.value = true;
}

function onCalendarChange() {
  // NCalendar v-model updates automatically
}

async function handleSubmit() {
  submitting.value = true;
  try {
    const payload: Record<string, unknown> = {};
    if (submitForm.planId) payload.planId = submitForm.planId;
    payload.exerciseStatus = submitForm.exerciseStatus;
    payload.dietStatus = submitForm.dietStatus;
    if (submitForm.currentWeight != null) payload.currentWeight = submitForm.currentWeight;
    if (submitForm.mood) payload.mood = submitForm.mood;
    if (submitForm.note) payload.note = submitForm.note;

    const { error } = await fetchSubmitCheckin(payload as any);
    if (error) {
      message.error('打卡失败');
      return;
    }
    message.success('打卡成功');
    submitForm.exerciseStatus = 0;
    submitForm.dietStatus = 0;
    submitForm.currentWeight = null;
    submitForm.mood = '';
    submitForm.note = '';
    await loadData();
  } catch {
    // handled by interceptor
  } finally {
    submitting.value = false;
  }
}

async function handleSupplementSubmit() {
  submitting.value = true;
  try {
    const payload: Record<string, unknown> = {
      planId: supplementForm.planId,
      checkDate: supplementForm.checkDate,
      exerciseStatus: supplementForm.exerciseStatus,
      dietStatus: supplementForm.dietStatus,
      date: supplementForm.checkDate,
      reason: supplementForm.reason
    };
    if (supplementForm.currentWeight != null) payload.currentWeight = supplementForm.currentWeight;
    if (supplementForm.mood) payload.mood = supplementForm.mood;
    if (supplementForm.note) payload.note = supplementForm.note;

    const { error } = await fetchSupplementCheckin(payload as any);
    if (error) {
      message.error('补卡失败');
      return;
    }
    message.success('补卡成功');
    supplementVisible.value = false;
    await loadData();
  } catch {
    // handled by interceptor
  } finally {
    submitting.value = false;
  }
}

async function loadData() {
  pageLoading.value = true;
  try {
    const [todayResult, listResult, statsResult] = await Promise.all([
      fetchGetTodayCheckin(),
      fetchGetCheckinList(),
      fetchGetCheckinStats()
    ]);

    // Load today's record
    if (todayResult.data) {
      todayRecord.value = todayResult.data;
    } else {
      todayRecord.value = null;
    }

    const list: CheckinRecord[] = listResult.data || [];
    const statsData = (statsResult.data || {}) as Partial<Api.Checkin.CheckinStats>;

    records.value = {};
    list.forEach(item => {
      records.value[item.checkDate] = item;
    });

    Object.assign(stats, {
      consecutiveDays: statsData.consecutiveDays ?? 0,
      totalDays: statsData.totalDays ?? 0,
      currentWeekDays: statsData.currentWeekDays ?? 0,
      currentMonthDays: statsData.currentMonthDays ?? 0,
      exerciseCompleteRate: statsData.exerciseCompleteRate ?? 0,
      dietCompleteRate: statsData.dietCompleteRate ?? 0
    });

    // If today's record not loaded from dedicated API, try from list
    if (!todayRecord.value && records.value[today]) {
      todayRecord.value = records.value[today];
    }

    nextTick(() => renderHeatmap());
    await loadHistory();
  } catch {
    // ignore
  } finally {
    pageLoading.value = false;
  }
}

async function loadHistory() {
  historyLoading.value = true;
  try {
    const params: { page: number; size: number; startDate?: string; endDate?: string } = {
      page: historyPage.value,
      size: historyPageSize.value
    };

    if (historyDateRange.value && historyDateRange.value[0] && historyDateRange.value[1]) {
      params.startDate = historyDateRange.value[0];
      params.endDate = historyDateRange.value[1];
    }

    const { data, error } = await fetchGetCheckinPage(params as any);
    if (error || !data) {
      historyRecords.value = [];
      historyTotal.value = 0;
      return;
    }

    historyRecords.value = data.records || [];
    historyTotal.value = data.total || 0;
  } catch {
    historyRecords.value = [];
    historyTotal.value = 0;
  } finally {
    historyLoading.value = false;
  }
}

function resetHistoryFilter() {
  historyDateRange.value = null;
  historyPage.value = 1;
  loadHistory();
}

function handlePageSizeChange(pageSize: number) {
  historyPageSize.value = pageSize;
  historyPage.value = 1;
  loadHistory();
}

function getDaysInYear(): string[] {
  const days: string[] = [];
  const now = new Date();
  const startDate = new Date(now.getFullYear(), now.getMonth() - 11, 1);
  for (let d = new Date(startDate); d <= now; d.setDate(d.getDate() + 1)) {
    days.push(toDateStr(d));
  }
  return days;
}

function getHeatmapLevel(dateStr: string): number {
  const r = records.value[dateStr];
  if (!r) return 0;
  if (r.exerciseStatus === 2 && r.dietStatus === 2) return 3;
  if (r.exerciseStatus >= 1 || r.dietStatus >= 1) return 2;
  return 1;
}

function renderHeatmap() {
  if (!heatmapRef.value) return;
  if (heatmapChart) heatmapChart.dispose();
  heatmapChart = echarts.init(heatmapRef.value);
  const days = getDaysInYear();
  const data = days.map(d => [d, getHeatmapLevel(d)]);
  heatmapChart.setOption({
    tooltip: {
      backgroundColor: 'rgba(22,27,34,0.95)',
      borderColor: '#30363d',
      textStyle: { color: '#c9d1d9' },
      formatter: (p: any) => `${p.data[0]}: ${['未打卡', '部分', '不错', '完美'][p.data[1]]}`
    },
    visualMap: {
      min: 0, max: 3, orient: 'horizontal', left: 'center', bottom: 0,
      calculable: false,
      inRange: { color: ['#161b22', '#0e4429', '#006d32', '#26a641'] },
      show: false
    },
    calendar: {
      top: 20, left: 40, right: 20,
      range: [days[0], days[days.length - 1]],
      cellSize: [14, 14],
      splitLine: { lineStyle: { color: '#0d1117' } },
      itemStyle: { borderColor: '#0d1117', borderWidth: 2, borderRadius: 2 },
      dayLabel: { color: '#8b949e' },
      monthLabel: { color: '#8b949e' },
      yearLabel: { show: false }
    },
    series: [{
      type: 'heatmap',
      coordinateSystem: 'calendar',
      data,
      emphasis: { itemStyle: { shadowBlur: 8, shadowColor: 'rgba(0,0,0,0.3)' } }
    }]
  });
  heatmapChart.resize();
}

onMounted(() => {
  loadData();
});

onBeforeUnmount(() => {
  if (heatmapChart) {
    heatmapChart.dispose();
    heatmapChart = null;
  }
});
</script>

<style scoped>
.stats-bar :deep(.n-card__content) { padding: 16px 32px; }

.text-green { color: #3fb950; }

.date-cell.cell-full {
  background: rgba(63, 185, 80, 0.08);
  border-color: rgba(63, 185, 80, 0.35);
  box-shadow: 0 0 8px rgba(63, 185, 80, 0.12);
}
.date-cell.cell-partial {
  background: rgba(210, 153, 34, 0.08);
  border-color: rgba(210, 153, 34, 0.35);
}
.date-cell.cell-incomplete {
  background: rgba(139, 148, 158, 0.04);
  border-color: transparent;
}
.date-cell.cell-missed {
  background: rgba(248, 81, 73, 0.04);
  border-color: transparent;
}
.date-cell.cell-missed:hover {
  border-color: rgba(248, 81, 73, 0.3);
  background: rgba(248, 81, 73, 0.08);
}
.date-cell.cell-can-supplement {
  background: rgba(248, 81, 73, 0.04);
  border: 1px dashed rgba(248, 81, 73, 0.3);
}
.date-cell.cell-can-supplement:hover {
  border-color: rgba(248, 81, 73, 0.5);
  background: rgba(248, 81, 73, 0.1);
}
.date-cell.cell-today {
  background: rgba(88, 166, 255, 0.06);
  border-color: rgba(88, 166, 255, 0.3);
}
.date-cell.cell-future {
  cursor: default;
  opacity: 0.45;
}

.dot.dot-full {
  background: #3fb950;
  box-shadow: 0 0 4px rgba(63, 185, 80, 0.6);
}
.dot.dot-partial {
  background: #d29922;
  box-shadow: 0 0 4px rgba(210, 153, 34, 0.5);
}
.dot.dot-incomplete {
  background: #8b949e;
}
</style>
