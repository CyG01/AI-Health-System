<template>
  <div class="sleep-page flex flex-col gap-5">
    <div>
      <h2 class="text-xl font-semibold">{{ $t('sleep.management') || '睡眠管理' }}</h2>
    </div>

    <!-- 提交记录 -->
    <NCard :bordered="false">
      <template #header>
        <span>{{ todayRecord ? ($t('sleep.updateToday') || '更新今日睡眠') : ($t('sleep.recordToday') || '记录今日睡眠') }}</span>
      </template>
      <NForm ref="formRef" :model="form" :rules="formRules" label-placement="left" label-width="80" @submit.prevent="handleSubmit">
        <div class="flex flex-wrap items-end gap-4">
          <NFormItem label="日期" path="recordDate" class="min-w-[160px]">
            <NDatePicker
              v-model:formatted-value="form.recordDate"
              type="date"
              value-format="yyyy-MM-dd"
              clearable
              class="w-full"
            />
          </NFormItem>
          <NFormItem label="入睡时间" path="sleepTime" class="min-w-[140px]">
            <NTimePicker
              v-model:formatted-value="form.sleepTime"
              format="HH:mm"
              clearable
              class="w-full"
            />
          </NFormItem>
          <NFormItem label="起床时间" path="wakeTime" class="min-w-[140px]">
            <NTimePicker
              v-model:formatted-value="form.wakeTime"
              format="HH:mm"
              clearable
              class="w-full"
            />
          </NFormItem>
          <NFormItem label="睡眠质量" class="min-w-[180px]">
            <div class="flex items-center gap-1">
              <span
                v-for="star in 5"
                :key="star"
                class="star-btn"
                :class="{ active: star <= form.quality }"
                @click="form.quality = star"
              >&#9733;</span>
              <span class="ml-2 text-xs text-secondary">{{ qualityTexts[form.quality - 1] }}</span>
            </div>
          </NFormItem>
          <NFormItem>
            <NButton type="primary" attr-type="submit" :loading="submitting">提交</NButton>
          </NFormItem>
        </div>
        <NFormItem label="备注">
          <NInput v-model:value="form.dreamNotes" placeholder="梦境记录或其他备注..." :maxlength="200" show-count />
        </NFormItem>
      </NForm>
    </NCard>

    <!-- 统计概览 -->
    <div v-if="records.length > 0" class="flex gap-5">
      <NCard :bordered="false" class="flex-1 text-center">
        <div class="text-2xl font-bold">{{ avgDuration.toFixed(1) }}</div>
        <div class="text-[13px] text-secondary">近7天平均时长(小时)</div>
      </NCard>
      <NCard :bordered="false" class="flex-1 text-center">
        <div class="text-2xl font-bold">{{ avgQuality.toFixed(1) }}</div>
        <div class="text-[13px] text-secondary">近7天平均质量 /5</div>
      </NCard>
      <NCard :bordered="false" class="flex-1 text-center">
        <div class="text-2xl font-bold">{{ records.length }}</div>
        <div class="text-[13px] text-secondary">总记录天数</div>
      </NCard>
      <NCard :bordered="false" class="flex-1 text-center">
        <div class="text-2xl font-bold">{{ bestQuality }}</div>
        <div class="text-[13px] text-secondary">最好质量(分)</div>
      </NCard>
    </div>

    <!-- AI分析 -->
    <NCard :bordered="false">
      <template #header>
        <div class="flex items-center justify-between">
          <span>{{ $t('sleep.aiAnalysis') || 'AI睡眠分析' }}</span>
          <NButton size="small" type="primary" :loading="analyzing" @click="handleAnalyze">
            {{ aiAnalysis ? '重新分析' : '开始分析' }}
          </NButton>
        </div>
      </template>
      <div v-if="aiAnalysis" class="flex items-start gap-2 rounded-lg border border-[rgba(88,166,255,0.15)] bg-[rgba(88,166,255,0.06)] p-4 text-sm leading-relaxed">
        <NIcon :size="20" color="#58a6ff"><FlashOutline /></NIcon>
        <p v-html="formatAnalysis(aiAnalysis)"></p>
      </div>
      <NEmpty v-else description="记录至少3天睡眠数据后可获得AI分析" size="small" />
    </NCard>

    <!-- 历史记录 -->
    <NCard :bordered="false">
      <template #header><span>{{ $t('sleep.records') || '睡眠记录' }}</span></template>
      <NEmpty v-if="records.length === 0 && !pageLoading" :description="$t('sleep.empty') || '还没有睡眠记录，使用上方的表单记录你的第一次睡眠'" />
      <NDataTable
        v-else
        :data="records"
        :columns="tableColumns"
        :loading="pageLoading"
        :bordered="false"
        striped
      />
    </NCard>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, h } from 'vue';
import { useMessage, useDialog, NCard, NForm, NFormItem, NDatePicker, NTimePicker, NInput, NButton, NDataTable, NEmpty, NIcon } from 'naive-ui';
import type { DataTableColumns, FormInst } from 'naive-ui';
import { FlashOutline } from '@vicons/ionicons5';
import { fetchSubmitSleep, fetchGetTodaySleep, fetchGetSleepList, fetchAnalyzeSleep, fetchDeleteSleep } from '@/service/api';
import { sanitizeHtml } from '@/utils/sanitize';

defineOptions({ name: 'SleepRecord' });
const message = useMessage();
const dialog = useDialog();

interface SleepRecord {
  id: number;
  recordDate: string;
  sleepTime: string;
  wakeTime: string;
  durationMin: number;
  quality: number;
  dreamNotes?: string;
}

const pageLoading = ref(false);
const submitting = ref(false);
const analyzing = ref(false);
const records = ref<SleepRecord[]>([]);
const todayRecord = ref<SleepRecord | null>(null);
const aiAnalysis = ref('');
const formRef = ref<FormInst | null>(null);

const formRules = {
  recordDate: { type: 'string' as const, required: true, message: '请选择日期', trigger: 'change' },
  sleepTime: { type: 'string' as const, required: true, message: '请选择入睡时间', trigger: 'change' },
  wakeTime: { type: 'string' as const, required: true, message: '请选择起床时间', trigger: 'change' }
};

const form = ref({
  recordDate: new Date().toISOString().slice(0, 10),
  sleepTime: null as string | null,
  wakeTime: null as string | null,
  quality: 3,
  dreamNotes: ''
});

const qualityTexts = ['很差', '较差', '一般', '较好', '很好'];

const avgDuration = computed(() => {
  const recent = records.value.slice(0, 7);
  if (recent.length === 0) return 0;
  return recent.reduce((s, r) => s + r.durationMin, 0) / recent.length / 60;
});

const avgQuality = computed(() => {
  const recent = records.value.slice(0, 7);
  if (recent.length === 0) return 0;
  return recent.reduce((s, r) => s + r.quality, 0) / recent.length;
});

const bestQuality = computed(() => {
  if (records.value.length === 0) return 0;
  return Math.max(...records.value.map(r => r.quality));
});

function renderStars(quality: number) {
  return h('div', { class: 'flex items-center gap-0.5' },
    Array.from({ length: 5 }, (_, i) =>
      h('span', {
        class: i < quality ? 'text-[#f5a623]' : 'text-[#484848]',
        style: 'font-size: 14px;'
      }, '\u2605')
    )
  );
}

const tableColumns: DataTableColumns<SleepRecord> = [
  { title: '日期', key: 'recordDate', width: 120 },
  { title: '入睡', key: 'sleepTime', width: 100, render: (row) => formatTime(row.sleepTime) },
  { title: '起床', key: 'wakeTime', width: 100, render: (row) => formatTime(row.wakeTime) },
  { title: '时长', key: 'durationMin', width: 100, render: (row) => `${(row.durationMin / 60).toFixed(1)}h` },
  { title: '质量', key: 'quality', width: 140, render: (row) => renderStars(row.quality) },
  { title: '备注', key: 'dreamNotes', minWidth: 150, ellipsis: { tooltip: true } },
  {
    title: '操作', key: 'actions', width: 80, fixed: 'right',
    render: (row) => h(NButton, { type: 'error', size: 'small', text: true, onClick: () => handleDeleteSleep(row.id) }, { default: () => '删除' })
  }
];

async function loadRecords() {
  pageLoading.value = true;
  try {
    const { data } = await fetchGetSleepList(30);
    records.value = data || [];
  } finally {
    pageLoading.value = false;
  }
}

async function loadToday() {
  try {
    const { data } = await fetchGetTodaySleep();
    todayRecord.value = data ?? null;
    if (todayRecord.value) {
      form.value.recordDate = todayRecord.value.recordDate;
      form.value.sleepTime = todayRecord.value.sleepTime;
      form.value.wakeTime = todayRecord.value.wakeTime;
      form.value.quality = todayRecord.value.quality;
      form.value.dreamNotes = todayRecord.value.dreamNotes || '';
    }
  } catch {
    // no record for today
  }
}

async function handleSubmit() {
  if (formRef.value) {
    try {
      await formRef.value.validate();
    } catch {
      return;
    }
  }
  submitting.value = true;
  try {
    await fetchSubmitSleep({ ...form.value, quality: form.value.quality || 3 });
    message.success('已记录');
    await loadToday();
    await loadRecords();
  } finally {
    submitting.value = false;
  }
}

async function handleAnalyze() {
  analyzing.value = true;
  try {
    const { data } = await fetchAnalyzeSleep();
    aiAnalysis.value = data?.analysis || '';
  } finally {
    analyzing.value = false;
  }
}

function handleDeleteSleep(id: number) {
  dialog.warning({
    title: '删除确认',
    content: '确定删除此条睡眠记录吗？',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      await fetchDeleteSleep(id);
      message.success('已删除');
      loadRecords();
    }
  });
}

function formatTime(timeStr: string | null): string {
  if (!timeStr) return '-';
  return timeStr.substring(0, 5);
}

function formatAnalysis(text: string): string {
  if (!text) return '';
  return sanitizeHtml(text.replace(/\n/g, '<br>'));
}

onMounted(() => {
  loadToday();
  loadRecords();
});
</script>

<style scoped>
.sleep-page {
  padding: 8px;
}

.star-btn {
  cursor: pointer;
  font-size: 20px;
  color: #484848;
  transition: color 0.15s;
  user-select: none;
}

.star-btn.active {
  color: #f5a623;
}

.star-btn:hover {
  color: #f5a623;
}
</style>
