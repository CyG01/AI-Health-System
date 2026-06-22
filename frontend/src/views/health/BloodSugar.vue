<template>
  <div class="py-5">
    <div class="mb-5">
      <h2 class="text-xl font-semibold m-0">血糖监测</h2>
    </div>

    <!-- Submit Blood Sugar Record -->
    <n-card class="mb-5">
      <template #header><span>记录血糖</span></template>
      <n-form ref="formRef" :model="form" :rules="formRules" label-placement="left" label-width="80" @submit.prevent="handleSubmit">
        <div class="flex flex-wrap gap-4">
          <div class="w-[160px]">
            <n-form-item label="日期" path="recordDate">
              <n-date-picker
                v-model:formatted-value="form.recordDate"
                type="date"
                value-format="yyyy-MM-dd"
                class="w-full"
              />
            </n-form-item>
          </div>
          <div class="w-[140px]">
            <n-form-item label="时间" path="recordTime">
              <n-time-picker
                v-model:formatted-value="form.recordTime"
                format="HH:mm"
                value-format="HH:mm"
                class="w-full"
              />
            </n-form-item>
          </div>
          <div class="w-[140px]">
            <n-form-item label="类型" path="measureType">
              <n-select
                v-model:value="form.measureType"
                :options="measureTypeOptions"
                class="w-full"
              />
            </n-form-item>
          </div>
          <div class="w-[160px]">
            <n-form-item label="血糖值" path="glucoseValue">
              <n-input-number
                v-model:value="form.glucoseValue"
                :min="0.1"
                :max="50"
                :precision="1"
                class="w-full"
              >
                <template #suffix>mmol/L</template>
              </n-input-number>
            </n-form-item>
          </div>
          <div class="w-[160px]">
            <n-form-item label="备注" path="note">
              <n-input v-model:value="form.note" placeholder="可选备注" />
            </n-form-item>
          </div>
          <div>
            <n-button type="primary" attr-type="submit" :loading="submitting">提交记录</n-button>
          </div>
        </div>
      </n-form>
    </n-card>

    <!-- Date Filter -->
    <div class="flex items-center gap-3 mb-5 p-3 bg-gray-800/50 border border-gray-700 rounded-md">
      <span class="text-sm text-gray-400 whitespace-nowrap">按日期查询：</span>
      <n-date-picker
        v-model:formatted-value="filterDate"
        type="date"
        value-format="yyyy-MM-dd"
        clearable
        class="w-[200px]"
        @update:formatted-value="handleDateFilter"
      />
      <n-button v-if="filterDate" quaternary type="info" size="small" @click="clearDateFilter">
        清除筛选
      </n-button>
    </div>

    <!-- Trend Chart -->
    <n-card v-if="trendData.length > 0" class="mb-5">
      <template #header><span>血糖趋势 (近14天)</span></template>
      <div ref="trendChartRef" class="h-[300px]"></div>
    </n-card>

    <!-- Empty State -->
    <div v-if="records.length === 0 && !recordsLoading" class="text-center py-12 mb-5">
      <div class="text-5xl mb-3">🩸</div>
      <div class="text-lg font-semibold mb-2">暂无血糖记录</div>
      <div class="text-sm text-gray-400">记录你的第一次血糖数据，开始跟踪健康状况</div>
    </div>

    <!-- Records Table -->
    <n-card>
      <template #header>
        <div class="flex justify-between items-center">
          <span>血糖记录</span>
          <n-pagination
            v-model:page="page"
            :page-count="Math.ceil(total / 10)"
            :page-slot="5"
            size="small"
            @update:page="loadRecords"
          />
        </div>
      </template>
      <n-spin :show="recordsLoading">
        <n-data-table
          :columns="tableColumns"
          :data="records"
          :bordered="true"
          :single-line="true"
        />
      </n-spin>
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { ref, h, onMounted, onUnmounted, nextTick } from 'vue';
import {
  NButton, NCard, NDatePicker, NDataTable, NForm, NFormItem,
  NInput, NInputNumber, NPagination, NSelect, NSpin, NTag, NTimePicker,
  useDialog
} from 'naive-ui';
import type { FormInst, FormRules, DataTableColumns } from 'naive-ui';
import {
  fetchSubmitBloodSugar,
  fetchGetBloodSugarRecords,
  fetchGetBloodSugarByDate,
  fetchGetBloodSugarTrend,
  fetchDeleteBloodSugar
} from '@/service/api';
import * as echarts from 'echarts';

defineOptions({ name: 'BloodSugar' });

interface BloodSugarRecord {
  id: number | string;
  recordDate: string;
  recordTime?: string;
  measureType: string;
  glucoseValue: number;
  note?: string;
  abnormalFlag?: number;
}

interface BloodSugarForm {
  recordDate: string;
  recordTime: string;
  measureType: string;
  glucoseValue: number;
  note: string;
}

const dialog = useDialog();

const formRef = ref<FormInst | null>(null);
const records = ref<BloodSugarRecord[]>([]);
const recordsLoading = ref(false);
const submitting = ref(false);
const page = ref(1);
const total = ref(0);
const filterDate = ref<string | null>(null);
const trendData = ref<BloodSugarRecord[]>([]);
const trendChartRef = ref<HTMLElement | null>(null);
let trendChart: echarts.ECharts | null = null;

const form = ref<BloodSugarForm>({
  recordDate: new Date().toISOString().substring(0, 10),
  recordTime: new Date().toTimeString().substring(0, 5),
  measureType: 'fasting',
  glucoseValue: 5.5,
  note: ''
});

const measureTypeOptions = [
  { label: '空腹', value: 'fasting' },
  { label: '餐前', value: 'before_meal' },
  { label: '餐后', value: 'after_meal' },
  { label: '睡前', value: 'bedtime' },
  { label: '随机', value: 'random' }
];

const formRules: FormRules = {
  recordDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  measureType: [{ required: true, message: '请选择测量类型', trigger: 'change' }],
  glucoseValue: [{ required: true, type: 'number', message: '请输入血糖值', trigger: ['blur', 'change'] }]
};

function getMeasureTypeLabel(type: string): string {
  const map: Record<string, string> = {
    fasting: '空腹',
    before_meal: '餐前',
    after_meal: '餐后',
    bedtime: '睡前',
    random: '随机'
  };
  return map[type] || type;
}

function getMeasureTypeTagType(type: string): 'info' | 'default' | 'success' | 'warning' {
  switch (type) {
    case 'fasting': return 'default';
    case 'before_meal': return 'info';
    case 'after_meal': return 'success';
    case 'bedtime': return 'warning';
    default: return 'info';
  }
}

const tableColumns: DataTableColumns<BloodSugarRecord> = [
  {
    title: '日期',
    width: 110,
    key: 'recordDate',
    render: (row) => row.recordDate
  },
  {
    title: '时间',
    width: 80,
    key: 'recordTime',
    render: (row) => row.recordTime?.substring(0, 5) || '-'
  },
  {
    title: '类型',
    width: 80,
    key: 'measureType',
    render: (row) =>
      h(NTag, { size: 'small', type: getMeasureTypeTagType(row.measureType) }, { default: () => getMeasureTypeLabel(row.measureType) })
  },
  {
    title: '血糖值',
    width: 160,
    key: 'glucoseValue',
    render: (row) => {
      const color = row.abnormalFlag === 1 ? '#ff4d4f' : row.abnormalFlag === 2 ? '#fa8c16' : '#52c41a';
      const children: any[] = [
        h('span', { style: { color, fontWeight: 600 } }, `${row.glucoseValue} mmol/L`)
      ];
      if (row.abnormalFlag === 1) {
        children.push(h(NTag, { size: 'small', type: 'error', style: 'margin-left:6px' }, { default: () => '偏高' }));
      }
      if (row.abnormalFlag === 2) {
        children.push(h(NTag, { size: 'small', type: 'warning', style: 'margin-left:6px' }, { default: () => '偏低' }));
      }
      return h('span', {}, children);
    }
  },
  {
    title: '备注',
    key: 'note',
    minWidth: 120
  },
  {
    title: '操作',
    width: 80,
    key: 'actions',
    fixed: 'right',
    render: (row) =>
      h(
        NButton,
        { size: 'small', quaternary: true, type: 'error', onClick: () => handleDelete(row.id) },
        { default: () => '删除' }
      )
  }
];

async function handleSubmit(): Promise<void> {
  if (formRef.value) {
    try {
      await formRef.value.validate();
    } catch {
      return;
    }
  }
  submitting.value = true;
  try {
    const payload = {
      recordDate: form.value.recordDate,
      recordTime: form.value.recordTime,
      measureType: form.value.measureType,
      glucoseValue: form.value.glucoseValue,
      note: form.value.note
    };
    const { data, error } = await fetchSubmitBloodSugar(payload);
    if (!error) {
      window.$message?.success('血糖记录成功');
      form.value.note = '';
      form.value.glucoseValue = 5.5;
      loadRecords();
      loadTrend();
    }
  } finally {
    submitting.value = false;
  }
}

async function loadRecords(): Promise<void> {
  recordsLoading.value = true;
  try {
    if (filterDate.value) {
      const { data, error } = await fetchGetBloodSugarByDate(filterDate.value);
      if (data && !error) {
        const list = Array.isArray(data) ? data : ((data as any).records || []);
        records.value = list;
        total.value = Array.isArray(data) ? data.length : ((data as any).total || list.length);
      }
    } else {
      const { data, error } = await fetchGetBloodSugarRecords({ page: page.value, size: 10 });
      if (data && !error) {
        records.value = (data as any).records || [];
        total.value = (data as any).total || 0;
      }
    }
  } finally {
    recordsLoading.value = false;
  }
}

async function loadTrend(): Promise<void> {
  try {
    const { data, error } = await fetchGetBloodSugarTrend({ days: 14 });
    if (data && !error) {
      trendData.value = data;
      await nextTick();
      initTrendChart();
    }
  } catch { /* ignore */ }
}

function initTrendChart(): void {
  if (!trendChartRef.value || trendData.value.length === 0) return;
  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value);
  }
  const dates = trendData.value.map(r => r.recordDate);
  const values = trendData.value.map(r => Number(r.glucoseValue));

  const markPoints = trendData.value
    .map((r, i) =>
      r.abnormalFlag && r.abnormalFlag > 0
        ? { coord: [r.recordDate, Number(r.glucoseValue)], value: r.abnormalFlag === 1 ? '高' : '低' }
        : null
    )
    .filter(Boolean);

  trendChart.setOption({
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const p = params[0];
        return `${p.axisValue}<br/>血糖: ${p.value} mmol/L`;
      }
    },
    grid: { left: 50, right: 20, top: 20, bottom: 30 },
    xAxis: { type: 'category', data: dates, axisLabel: { color: '#8b949e' } },
    yAxis: {
      type: 'value',
      name: 'mmol/L',
      axisLabel: { color: '#8b949e' },
      min: (min: any) => Math.max(0, min.value - 1),
      max: (max: any) => max.value + 2
    },
    series: [{
      type: 'line',
      data: values,
      smooth: true,
      lineStyle: { color: '#58a6ff', width: 2 },
      itemStyle: { color: '#58a6ff' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(88,166,255,0.25)' },
          { offset: 1, color: 'rgba(88,166,255,0.03)' }
        ])
      },
      markLine: {
        silent: true,
        lineStyle: { color: '#ff4d4f', type: 'dashed' },
        data: [{ yAxis: 11.1, label: { formatter: '偏高\n11.1', position: 'end', color: '#e6edf3', fontSize: 10 } }]
      },
      markPoint: markPoints.length > 0 ? {
        data: markPoints.map(p => ({
          coord: (p as any).coord,
          value: (p as any).value,
          symbol: 'circle',
          symbolSize: 10,
          itemStyle: { color: (p as any).value === '高' ? '#f85149' : '#fa8c16' },
          label: { show: true, formatter: (p as any).value, color: '#e6edf3', fontSize: 10 }
        }))
      } : undefined
    }]
  });
}

function handleDateFilter(_date: string | null): void {
  page.value = 1;
  loadRecords();
}

function clearDateFilter(): void {
  filterDate.value = null;
  page.value = 1;
  loadRecords();
}

async function handleDelete(id: number | string): Promise<void> {
  const confirmed = await new Promise<boolean>((resolve) => {
    dialog.warning({
      title: '提示',
      content: '确定要删除这条血糖记录吗？',
      positiveText: '确定',
      negativeText: '取消',
      onPositiveClick: () => resolve(true),
      onNegativeClick: () => resolve(false)
    });
  });
  if (!confirmed) return;

  try {
    await fetchDeleteBloodSugar(Number(id));
    window.$message?.success('删除成功');
    loadRecords();
    loadTrend();
  } catch { /* handled by interceptor */ }
}

function handleResize(): void {
  trendChart?.resize();
}

onMounted(() => {
  loadRecords();
  loadTrend();
  window.addEventListener('resize', handleResize);
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
  trendChart?.dispose();
});
</script>
