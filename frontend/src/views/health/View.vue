<template>
  <div class="p-1">
    <n-spin :show="pageLoading">
      <div v-if="hasRecord" class="flex flex-col gap-4 max-w-[860px]">
        <!-- Profile Card -->
        <n-card class="px-8 py-7">
          <div class="flex items-center justify-between mb-5">
            <h2 class="text-xl font-semibold">健康档案</h2>
            <n-button type="primary" @click="router.push('/health/form')">编辑档案</n-button>
          </div>

          <div class="grid gap-4 mb-6" style="grid-template-columns: repeat(auto-fill, minmax(140px, 1fr))">
            <div class="flex flex-col gap-1 p-3 bg-blue-500/6 rounded-lg">
              <span class="text-xs text-gray-400">身高</span>
              <span class="text-[22px] font-bold">{{ record.height }} <small class="text-xs font-normal text-gray-400">cm</small></span>
            </div>
            <div class="flex flex-col gap-1 p-3 bg-blue-500/6 rounded-lg">
              <span class="text-xs text-gray-400">体重</span>
              <span class="text-[22px] font-bold">{{ record.weight }} <small class="text-xs font-normal text-gray-400">kg</small></span>
            </div>
            <div class="flex flex-col gap-1 p-3 bg-blue-500/6 rounded-lg">
              <span class="text-xs text-gray-400">BMI</span>
              <span class="text-[22px] font-bold" :style="{ color: bmiColor }">{{ record.bmi }}</span>
            </div>
            <div class="flex flex-col gap-1 p-3 bg-blue-500/6 rounded-lg">
              <span class="text-xs text-gray-400">基础代谢率(BMR)</span>
              <span class="text-[22px] font-bold">{{ record.bmr }} <small class="text-xs font-normal text-gray-400">kcal/天</small></span>
            </div>
            <div class="flex flex-col gap-1 p-3 bg-blue-500/6 rounded-lg">
              <span class="text-xs text-gray-400">每日所需热量</span>
              <span class="text-[22px] font-bold">{{ record.dailyCalorie }} <small class="text-xs font-normal text-gray-400">kcal</small></span>
            </div>
          </div>

          <div v-if="record.goal" class="flex flex-col gap-1 py-2.5 border-b border-gray-700/25">
            <span class="text-xs text-gray-400">健康目标</span>
            <span class="text-sm">{{ record.goal }}</span>
          </div>
          <div v-if="record.diseaseHistory" class="flex flex-col gap-1 py-2.5 border-b border-gray-700/25">
            <span class="text-xs text-gray-400">既往病史</span>
            <span class="text-sm">{{ record.diseaseHistory }}</span>
          </div>
          <div v-if="record.allergyHistory" class="flex flex-col gap-1 py-2.5 border-b border-gray-700/25">
            <span class="text-xs text-gray-400">过敏史</span>
            <span class="text-sm">{{ record.allergyHistory }}</span>
          </div>
          <div v-if="record.exerciseHabit" class="flex flex-col gap-1 py-2.5 border-b border-gray-700/25">
            <span class="text-xs text-gray-400">运动习惯</span>
            <span class="text-sm">{{ record.exerciseHabit }}</span>
          </div>
          <div v-if="record.dietHabit" class="flex flex-col gap-1 py-2.5 border-b border-gray-700/25">
            <span class="text-xs text-gray-400">饮食习惯</span>
            <span class="text-sm">{{ record.dietHabit }}</span>
          </div>
        </n-card>

        <!-- BMI Gauge -->
        <n-card class="px-8 py-7">
          <h2 class="text-xl font-semibold mb-4">BMI 仪表盘</h2>
          <div ref="bmiChartRef" class="w-full h-[300px]"></div>
        </n-card>

        <!-- Health Risk Assessment -->
        <n-card v-if="assessment" class="px-8 py-7">
          <h2 class="text-xl font-semibold mb-4">健康风险评估</h2>
          <div
            class="inline-flex items-center px-5 py-1.5 rounded-full text-base font-semibold text-white mb-4"
            :style="{ background: bmiLevelBg }"
          >
            {{ assessment.bmiLevel }}
          </div>

          <ul class="flex flex-col gap-2.5 list-none">
            <li v-for="(risk, idx) in assessment.risks" :key="idx" class="flex items-start gap-2.5 text-sm leading-relaxed">
              <n-icon color="#58a6ff" :size="16" class="mt-0.5 shrink-0">
                <WarningOutline />
              </n-icon>
              <span>{{ risk }}</span>
            </li>
          </ul>

          <!-- Health Score -->
          <div v-if="assessment.healthScore != null" class="mt-4 pt-3 border-t border-gray-700/25">
            <div class="flex items-baseline gap-1">
              <span
                class="text-[40px] font-bold leading-none"
                :style="{ color: assessment.healthScore >= 80 ? '#3fb950' : assessment.healthScore >= 60 ? '#d29922' : '#f85149' }"
              >
                {{ assessment.healthScore }}
              </span>
              <span class="text-[13px] text-gray-400">/100 健康评分</span>
            </div>
          </div>

          <!-- AI Suggestion -->
          <div v-if="assessment.aiSuggestion" class="mt-4 pt-3 border-t border-gray-700/25">
            <h3 class="text-sm text-blue-400 mb-2">AI 改善建议</h3>
            <p class="text-[13px] leading-relaxed m-0">{{ assessment.aiSuggestion }}</p>
          </div>

          <!-- Comprehensive Assessment Metrics -->
          <div
            v-if="assessment.estimatedBodyFatRate || assessment.bmrAssessment || assessment.cardiovascularRisk || assessment.exerciseAbility"
            class="grid gap-4 mt-4"
            style="grid-template-columns: repeat(auto-fill, minmax(140px, 1fr))"
          >
            <div v-if="assessment.estimatedBodyFatRate" class="flex flex-col gap-1 p-3 bg-blue-500/6 rounded-lg">
              <span class="text-xs text-gray-400">估算体脂率</span>
              <span class="text-[22px] font-bold">
                {{ assessment.estimatedBodyFatRate }}%
                <n-tag v-if="assessment.bodyFatLevel" size="small">{{ assessment.bodyFatLevel }}</n-tag>
              </span>
            </div>
            <div v-if="assessment.bmrAssessment" class="flex flex-col gap-1 p-3 bg-blue-500/6 rounded-lg">
              <span class="text-xs text-gray-400">基础代谢评估</span>
              <span class="text-[22px] font-bold">{{ assessment.bmrAssessment }}</span>
            </div>
            <div v-if="assessment.cardiovascularRisk" class="flex flex-col gap-1 p-3 bg-blue-500/6 rounded-lg">
              <span class="text-xs text-gray-400">心血管风险</span>
              <span class="text-[22px] font-bold">{{ assessment.cardiovascularRisk }}</span>
            </div>
            <div v-if="assessment.exerciseAbility" class="flex flex-col gap-1 p-3 bg-blue-500/6 rounded-lg">
              <span class="text-xs text-gray-400">运动能力</span>
              <span class="text-[22px] font-bold">{{ assessment.exerciseAbility }}</span>
            </div>
          </div>
        </n-card>

        <!-- Trend Chart -->
        <n-card v-if="historyRecords.length > 0" class="px-8 py-7">
          <h2 class="text-xl font-semibold mb-3">健康趋势</h2>
          <div ref="trendChartRef" class="w-full h-[300px]"></div>
        </n-card>

        <!-- Health Progress -->
        <n-card v-if="healthProgress" class="px-8 py-7">
          <h2 class="text-xl font-semibold mb-4">健康目标进度</h2>
          <n-descriptions :column="2" bordered label-placement="left">
            <n-descriptions-item v-if="healthProgress.weightGoal" label="目标体重">
              {{ healthProgress.weightGoal }} kg
            </n-descriptions-item>
            <n-descriptions-item v-if="healthProgress.currentWeight" label="当前体重">
              {{ healthProgress.currentWeight }} kg
            </n-descriptions-item>
            <n-descriptions-item v-if="healthProgress.weightChange != null" label="体重变化">
              <span :style="{ color: healthProgress.weightChange <= 0 ? '#3fb950' : '#d29922' }">
                {{ healthProgress.weightChange > 0 ? '+' : '' }}{{ healthProgress.weightChange }} kg
              </span>
            </n-descriptions-item>
            <n-descriptions-item v-if="healthProgress.daysElapsed != null" label="已执行天数">
              {{ healthProgress.daysElapsed }} 天
            </n-descriptions-item>
          </n-descriptions>
        </n-card>
      </div>

      <!-- Empty State -->
      <div v-else class="flex justify-center py-16">
        <n-card class="max-w-[860px] w-full">
          <n-empty description="暂无健康档案">
            <template #extra>
              <n-button type="primary" @click="router.push('/health/create')">立即创建</n-button>
            </template>
          </n-empty>
        </n-card>
      </div>
    </n-spin>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue';
import { useRouter } from 'vue-router';
import { NButton, NCard, NDescriptions, NDescriptionsItem, NEmpty, NIcon, NSpin, NTag } from 'naive-ui';
import { WarningOutline } from '@vicons/ionicons5';
import { fetchGetLatestHealth, fetchGetHealthAssessment, fetchGetHealthHistory, fetchGetHealthProgress } from '@/service/api';
import * as echarts from 'echarts';

defineOptions({ name: 'HealthView' });

interface HealthRecord {
  height?: number;
  weight?: number;
  bmi?: number;
  bmr?: number;
  dailyCalorie?: number;
  goal?: string;
  diseaseHistory?: string;
  allergyHistory?: string;
  exerciseHabit?: string;
  dietHabit?: string;
  recordDate?: string;
  createTime?: string;
}

interface HealthAssessment {
  bmiLevel?: string;
  risks?: string[];
  healthScore?: number;
  aiSuggestion?: string;
  estimatedBodyFatRate?: number;
  bodyFatLevel?: string;
  bmrAssessment?: string;
  cardiovascularRisk?: string;
  exerciseAbility?: string;
}

interface HealthProgress {
  weightGoal?: number;
  currentWeight?: number;
  weightChange?: number;
  daysElapsed?: number;
}

const router = useRouter();
const pageLoading = ref(false);
const record = ref<HealthRecord>({});
const assessment = ref<HealthAssessment | null>(null);
const hasRecord = ref(false);
const bmiChartRef = ref<HTMLElement | null>(null);
const trendChartRef = ref<HTMLElement | null>(null);
let bmiChartInstance: echarts.ECharts | null = null;
let trendChartInstance: echarts.ECharts | null = null;
const historyRecords = ref<HealthRecord[]>([]);
const healthProgress = ref<HealthProgress | null>(null);

const bmiColor = computed(() => {
  const bmi = record.value.bmi;
  if (!bmi) return '#8b949e';
  if (bmi < 18.5) return '#d29922';
  if (bmi < 24) return '#3fb950';
  if (bmi < 28) return '#d29922';
  return '#f85149';
});

const bmiLevelBg = computed(() => {
  const level = assessment.value?.bmiLevel;
  if (level === '正常') return '#3fb950';
  if (level === '偏瘦' || level === '偏胖') return '#d29922';
  if (level === '肥胖') return '#f85149';
  return '#30363d';
});

function renderBmiGauge(): void {
  if (!bmiChartRef.value) return;
  if (!bmiChartInstance) {
    bmiChartInstance = echarts.init(bmiChartRef.value);
  }

  const bmi = record.value.bmi || 0;
  const option: echarts.EChartsOption = {
    series: [
      {
        type: 'gauge',
        startAngle: 210,
        endAngle: -30,
        min: 10,
        max: 40,
        center: ['50%', '55%'],
        radius: '85%',
        axisLine: {
          lineStyle: {
            width: 20,
            color: [
              [0.25, '#3fb950'],
              [0.5, '#d29922'],
              [0.75, '#f85149'],
              [1, '#f85149']
            ] as [number, string][]
          }
        },
        axisTick: { show: false },
        splitLine: { show: false },
        axisLabel: { show: false },
        pointer: {
          length: '70%',
          width: 6,
          itemStyle: { color: '#58a6ff' }
        },
        detail: {
          valueAnimation: true,
          formatter: '{value}',
          color: '#e6edf3',
          fontSize: 36,
          fontWeight: 700,
          offsetCenter: [0, '75%']
        },
        title: {
          color: '#8b949e',
          fontSize: 12,
          offsetCenter: [0, '95%']
        },
        data: [{ value: bmi, name: 'BMI 指数' }]
      }
    ]
  };

  bmiChartInstance.setOption(option);
}

function renderTrendChart(): void {
  if (!trendChartRef.value || historyRecords.value.length === 0) return;
  if (!trendChartInstance) {
    trendChartInstance = echarts.init(trendChartRef.value);
  }
  const dates = historyRecords.value.map(r => r.recordDate || r.createTime?.substring(0, 10) || '');
  const weights = historyRecords.value.map(r => r.weight ?? null);
  const bmis = historyRecords.value.map(r => r.bmi ?? null);

  const option: echarts.EChartsOption = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['体重(kg)', 'BMI'], textStyle: { color: '#8b949e' } },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: dates, axisLabel: { color: '#8b949e' } },
    yAxis: [
      { type: 'value', name: '体重(kg)', nameTextStyle: { color: '#8b949e' }, axisLabel: { color: '#8b949e' } },
      { type: 'value', name: 'BMI', nameTextStyle: { color: '#8b949e' }, axisLabel: { color: '#8b949e' } }
    ],
    series: [
      { name: '体重(kg)', type: 'line', data: weights, smooth: true, itemStyle: { color: '#58a6ff' } },
      { name: 'BMI', type: 'line', yAxisIndex: 1, data: bmis, smooth: true, itemStyle: { color: '#3fb950' } }
    ]
  };
  trendChartInstance.setOption(option);
}

function loadData(): void {
  pageLoading.value = true;
  Promise.all([
    fetchGetLatestHealth().then(({ data, error }: any) => {
      if (data && !error) {
        record.value = data;
        hasRecord.value = true;
      } else {
        throw new Error('no record');
      }
    }),
    fetchGetHealthAssessment().then(({ data, error }: any) => {
      if (data && !error) {
        assessment.value = data;
      }
    }),
    fetchGetHealthHistory({ page: 1, size: 30 }).then(({ data, error }: any) => {
      if (data && !error) {
        historyRecords.value = Array.isArray(data) ? data : [];
      }
    }).catch(() => { historyRecords.value = []; }),
    fetchGetHealthProgress().then(({ data, error }: any) => {
      if (data && !error) {
        healthProgress.value = data;
      }
    }).catch(() => { healthProgress.value = null; })
  ])
    .catch(() => {
      hasRecord.value = false;
    })
    .finally(() => {
      pageLoading.value = false;
    });
}

watch(hasRecord, async (val) => {
  if (val) {
    await nextTick();
    renderBmiGauge();
  }
});

watch(historyRecords, async (val) => {
  if (val && val.length > 0) {
    await nextTick();
    renderTrendChart();
  }
});

onMounted(() => {
  loadData();
});

onBeforeUnmount(() => {
  bmiChartInstance?.dispose();
  trendChartInstance?.dispose();
});
</script>
