<template>
  <div class="water-page flex flex-col gap-5">
    <div>
      <h2 class="text-xl font-semibold">{{ $t('water.record') || '饮水记录' }}</h2>
      <p class="text-sm text-secondary">{{ $t('water.dailyTarget') || '每日建议饮水量：' }}{{ dailyTarget }}ml</p>
    </div>

    <!-- 今日饮水概览 -->
    <div class="flex gap-5">
      <NCard :bordered="false" class="flex-1 text-center">
        <div class="text-2xl font-bold">{{ todayAmount }}<span class="text-sm font-normal text-secondary"> / {{ dailyTarget }}ml</span></div>
        <div class="text-[13px] text-secondary">今日饮水量</div>
      </NCard>
      <NCard :bordered="false" class="flex-1 text-center">
        <div class="text-2xl font-bold">{{ completionPercent }}%</div>
        <div class="text-[13px] text-secondary">完成度</div>
      </NCard>
      <NCard :bordered="false" class="flex-1 text-center">
        <div class="text-2xl font-bold">{{ rangeAverageDaily || avgWeekly }}</div>
        <div class="text-[13px] text-secondary">日均饮水量(ml)</div>
      </NCard>
      <NCard :bordered="false" class="flex-1 text-center">
        <div class="text-2xl font-bold">{{ rangeDaysCount || records.length }}</div>
        <div class="text-[13px] text-secondary">记录天数</div>
      </NCard>
    </div>

    <!-- 进度条 -->
    <NCard :bordered="false">
      <div class="mb-3 flex items-center justify-between text-[15px] font-medium">
        <span>{{ $t('water.todayProgress') || '今日饮水进度' }}</span>
        <span class="font-semibold text-[#58a6ff]">{{ completionPercent }}%</span>
      </div>
      <NProgress
        type="line"
        :percentage="completionPercent"
        :height="20"
        :color="progressColor"
        :show-indicator="false"
        :border-radius="4"
      />
      <div class="mt-4 flex justify-between">
        <div
          v-for="i in 8"
          :key="i"
          class="flex flex-col items-center gap-1 transition-opacity"
          :class="todayAmount >= dailyTarget * i / 8 ? 'opacity-100 text-[#58a6ff]' : 'opacity-30'"
        >
          <NIcon :size="28"><RestaurantOutline /></NIcon>
          <span class="text-[11px] text-secondary">{{ dailyTarget * i / 8 }}ml</span>
        </div>
      </div>
    </NCard>

    <!-- 快速记录 -->
    <NCard :bordered="false">
      <template #header><span>{{ $t('water.quickAdd') || '快速记录饮水' }}</span></template>
      <div class="flex flex-wrap gap-2.5">
        <NButton
          v-for="opt in quickOptions"
          :key="opt.value"
          :type="opt.highlight ? 'primary' : 'default'"
          size="large"
          @click="quickAdd(opt.value)"
          :loading="submitting"
          :disabled="submitting"
        >
          {{ opt.label }}
        </NButton>
      </div>
      <NDivider />
      <NForm :model="form" :show-feedback="false" class="flex items-center gap-3" @submit.prevent="handleSubmit">
        <NFormItem label="自定义">
          <NInputNumber v-model:value="form.amountMl" :min="50" :max="1000" :step="50" />
          <span class="ml-2">ml</span>
        </NFormItem>
        <NFormItem>
          <NButton type="primary" attr-type="submit" :loading="submitting" :disabled="submitting">记录</NButton>
        </NFormItem>
      </NForm>
    </NCard>

    <!-- 空状态引导 -->
    <div v-if="records.length === 0 && !pageLoading" class="flex flex-col items-center gap-3 py-12">
      <div class="text-5xl">&#128167;</div>
      <div class="text-lg font-semibold">还没有饮水记录</div>
      <div class="text-sm text-secondary">点击上方快速记录按钮，开始你的第一次饮水记录吧</div>
    </div>

    <!-- 时间范围选择 + 饮水趋势图 -->
    <NCard v-if="records.length > 0" :bordered="false">
      <template #header>
        <div class="flex items-center justify-between">
          <span>{{ $t('water.trend') || '饮水趋势' }}</span>
          <NRadioGroup v-model:value="rangeDays" size="small" @update:value="onRangeChange">
            <NRadioButton :value="7">7天</NRadioButton>
            <NRadioButton :value="14">14天</NRadioButton>
            <NRadioButton :value="30">30天</NRadioButton>
          </NRadioGroup>
        </div>
      </template>
      <div v-if="rangeTotal > 0" class="mb-4 flex flex-wrap gap-4 text-sm">
        <span class="text-secondary">总计：<b class="text-[#58a6ff]">{{ rangeTotal }}ml</b></span>
        <span class="text-secondary">日均：<b class="text-[#58a6ff]">{{ rangeAverageDaily }}ml</b></span>
        <span class="text-secondary">天数：<b class="text-[#58a6ff]">{{ rangeDaysCount }}天</b></span>
      </div>
      <div class="chart-container" ref="chartRef"></div>
    </NCard>

    <!-- 历史记录 -->
    <NCard :bordered="false">
      <template #header><span>{{ $t('water.records') || '饮水记录' }}</span></template>
      <NDataTable
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
import { ref, computed, onMounted, onUnmounted, nextTick, watch, h } from 'vue';
import { useMessage, useDialog, NCard, NProgress, NButton, NIcon, NForm, NFormItem, NInputNumber, NDivider, NTag, NDataTable, NRadioGroup, NRadioButton } from 'naive-ui';
import type { DataTableColumns } from 'naive-ui';
import { RestaurantOutline } from '@vicons/ionicons5';
import { fetchSubmitWater, fetchGetTodayWater, fetchGetWaterList, fetchDeleteWater } from '@/service/api';
import echarts from '@/utils/echarts';

defineOptions({ name: 'WaterRecord' });
const message = useMessage();
const dialog = useDialog();

interface WaterRecord {
  id: number;
  recordDate: string;
  amountMl: number;
}

const pageLoading = ref(false);
const submitting = ref(false);
const todayAmount = ref(0);
const records = ref<WaterRecord[]>([]);
const chartRef = ref<HTMLDivElement | null>(null);
let chart: echarts.ECharts | null = null;

const dailyTarget = 2000;
const rangeDays = ref(7);

const rangeTotal = computed(() => records.value.reduce((s, r) => s + r.amountMl, 0));
const rangeAverageDaily = computed(() => {
  if (records.value.length === 0) return 0;
  return Math.round(rangeTotal.value / records.value.length);
});
const rangeDaysCount = computed(() => records.value.length);

const form = ref({
  amountMl: 250
});

interface QuickOption {
  label: string;
  value: number;
  highlight?: boolean;
}

const quickOptions: QuickOption[] = [
  { label: '100ml', value: 100 },
  { label: '200ml', value: 200 },
  { label: '250ml (一杯)', value: 250, highlight: true },
  { label: '300ml', value: 300 },
  { label: '500ml', value: 500 }
];

const completionPercent = computed(() => {
  return Math.min(100, Math.round(todayAmount.value / dailyTarget * 100));
});

const progressColor = computed(() => {
  const p = completionPercent.value;
  if (p >= 100) return '#52c41a';
  if (p >= 50) return '#58a6ff';
  if (p >= 25) return '#fa8c16';
  return '#ff4d4f';
});

const avgWeekly = computed(() => {
  if (records.value.length === 0) return 0;
  return Math.round(records.value.reduce((s, r) => s + r.amountMl, 0) / Math.min(records.value.length, 7));
});

const tableColumns: DataTableColumns<WaterRecord> = [
  { title: '日期', key: 'recordDate', width: 140 },
  {
    title: '饮水量', key: 'amountMl', width: 150,
    render: (row) => h('div', { class: 'flex items-center gap-2' }, [
      h('b', {}, `${row.amountMl} ml`),
      row.amountMl >= dailyTarget
        ? h(NTag, { type: 'success', size: 'small', bordered: false }, { default: () => '达标' })
        : null
    ])
  },
  {
    title: '完成度', key: 'completion', minWidth: 200,
    render: (row) => h(NProgress, {
      type: 'line',
      percentage: Math.min(100, Math.round(row.amountMl / dailyTarget * 100)),
      height: 12,
      color: row.amountMl >= dailyTarget ? '#52c41a' : '#58a6ff',
      showIndicator: false,
      borderRadius: 4
    })
  },
  {
    title: '操作', key: 'actions', width: 80, fixed: 'right',
    render: (row) => h(NButton, { type: 'error', size: 'small', text: true, onClick: () => handleDeleteWater(row.id) }, { default: () => '删除' })
  }
];

async function loadToday() {
  try {
    const { data } = await fetchGetTodayWater();
    todayAmount.value = data?.amountMl || 0;
  } catch {
    todayAmount.value = 0;
  }
}

async function loadRecords() {
  pageLoading.value = true;
  try {
    const { data } = await fetchGetWaterList(rangeDays.value);
    records.value = data || [];
  } finally {
    pageLoading.value = false;
  }
}

async function onRangeChange() {
  await loadRecords();
  await nextTick();
  initChart();
}

async function quickAdd(amount: number) {
  submitting.value = true;
  try {
    await fetchSubmitWater({ amountMl: amount, recordDate: new Date().toISOString().slice(0, 10) });
    message.success(`已记录 ${amount}ml`);
    await loadToday();
    await loadRecords();
    await nextTick();
    initChart();
  } finally {
    submitting.value = false;
  }
}

async function handleSubmit() {
  if (form.value.amountMl <= 0) {
    message.warning('请输入饮水量');
    return;
  }
  submitting.value = true;
  try {
    await fetchSubmitWater({ amountMl: form.value.amountMl, recordDate: new Date().toISOString().slice(0, 10) });
    message.success('已记录');
    form.value.amountMl = 250;
    await loadToday();
    await loadRecords();
    await nextTick();
    initChart();
  } finally {
    submitting.value = false;
  }
}

function handleDeleteWater(id: number) {
  dialog.warning({
    title: '删除确认',
    content: '确定删除此条饮水记录吗？',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      const { error } = await fetchDeleteWater(id);
      if (!error) {
        message.success('已删除');
        loadToday();
        loadRecords();
      }
    }
  });
}

function initChart() {
  if (!chartRef.value || records.value.length === 0) return;
  if (!chart) {
    chart = echarts.init(chartRef.value);
  }
  const reversed = [...records.value].reverse();
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 20, bottom: 20 },
    xAxis: {
      type: 'category',
      data: reversed.map(r => r.recordDate)
    },
    yAxis: {
      type: 'value',
      name: 'ml',
      min: 0
    },
    series: [
      {
        name: '饮水量',
        type: 'bar',
        data: reversed.map(r => r.amountMl),
        itemStyle: {
          color: (params: any) => params.value >= dailyTarget ? '#52c41a' : '#58a6ff',
          borderRadius: [4, 4, 0, 0]
        },
        markLine: {
          data: [{ yAxis: dailyTarget, name: '目标', label: { formatter: '目标 {c}ml' } }],
          lineStyle: { color: '#ff4d4f', type: 'dashed' }
        }
      }
    ]
  });
}

onMounted(async () => {
  await loadToday();
  await loadRecords();
  await nextTick();
  initChart();
});

onUnmounted(() => {
  chart?.dispose();
});

watch(records, () => {
  nextTick(() => initChart());
}, { deep: true });
</script>

<style scoped>
.chart-container {
  width: 100%;
  height: 280px;
}
</style>
