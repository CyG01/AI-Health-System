<script setup lang="ts">
import { computed, h, nextTick, onMounted, onUnmounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { NDataTable, NTag } from 'naive-ui';
import { $t } from '@/locales';
import {
  fetchGetLatestHealth,
  fetchGetHealthAssessment,
  fetchDashboardToday,
  fetchDashboardWeek,
  fetchDashboardMonth,
  fetchGreeting,
  fetchGetWeightTrend,
  fetchGetCheckinTrend,
  fetchGetProgress,
  fetchGetDietTrendComparison,
  fetchGetRecommendations
} from '@/service/api';
import echarts from '@/utils/echarts';

defineOptions({ name: 'Dashboard' });

const router = useRouter();

// ===================== State =====================
const activeTab = ref('today');
const dataLoading = ref(true);
const tabError = ref<string | null>(null);

const latestHealth = ref<Record<string, unknown>>({});
const today = ref<Record<string, unknown>>({});
const weekData = ref<Record<string, unknown> | null>(null);
const monthData = ref<Record<string, unknown> | null>(null);
const assessment = ref<Record<string, unknown> | null>(null);
const onProgress = ref<Record<string, unknown> | null>(null);
const dietComparison = ref<Record<string, unknown> | null>(null);
const recommends = ref<Record<string, unknown>>({});
const greetingCard = ref<Record<string, unknown> | null>(null);

// Chart refs
const weightChartRef = ref<HTMLElement | null>(null);
const checkinChartRef = ref<HTMLElement | null>(null);
const dietCompChartRef = ref<HTMLElement | null>(null);

let weightChart: echarts.ECharts | null = null;
let checkinChart: echarts.ECharts | null = null;
let dietCompChart: echarts.ECharts | null = null;

// ===================== Computed =====================
const progressPercent = computed(() => {
  return onProgress.value?.progressPercent ? Number(onProgress.value.progressPercent) : 0;
});

const progressColor = computed(() => {
  const rate = progressPercent.value;
  if (rate >= 80) return '#18a058';
  if (rate >= 50) return '#2080f0';
  return '#f0a020';
});

// ===================== Chart Helpers =====================
function initWeightChart(data: Record<string, unknown>) {
  if (!weightChartRef.value) return;
  if (!weightChart) {
    weightChart = echarts.init(weightChartRef.value);
  }
  const dates = (data.xAxis as string[]) || [];
  const weights = (data.yAxis as number[]) || [];
  weightChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 10, bottom: 30 },
    xAxis: { type: 'category', data: dates, boundaryGap: false },
    yAxis: { type: 'value', name: 'kg' },
    series: [{
      data: weights,
      type: 'line',
      smooth: true,
      lineStyle: { color: '#2080f0', width: 2 },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(32,128,240,0.3)' },
            { offset: 1, color: 'rgba(32,128,240,0.05)' }
          ]
        }
      },
      itemStyle: { color: '#2080f0' }
    }]
  });
}

function initCheckinChart(data: Record<string, unknown>) {
  if (!checkinChartRef.value) return;
  if (!checkinChart) {
    checkinChart = echarts.init(checkinChartRef.value);
  }
  const dates = (data.xAxis as string[]) || [];
  const counts = (data.completeRate as number[]) || [];
  checkinChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 10, bottom: 30 },
    xAxis: { type: 'category', data: dates },
    yAxis: { type: 'value', name: '%', max: 100 },
    series: [{
      data: counts,
      type: 'bar',
      itemStyle: {
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: '#18a058' },
            { offset: 1, color: '#b7eb8f' }
          ]
        },
        borderRadius: [4, 4, 0, 0]
      }
    }]
  });
}

function initDietComparisonChart() {
  if (!dietCompChartRef.value || !dietComparison.value) return;
  if (!dietCompChart) {
    dietCompChart = echarts.init(dietCompChartRef.value);
  }
  const data = dietComparison.value;
  const days = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
  const currentDaily = (data.currentDaily as Array<{ dayLabel: string; calories: number }>) || [];
  const previousDaily = (data.previousDaily as Array<{ dayLabel: string; calories: number }>) || [];
  const xLabels = currentDaily.map(d => d.dayLabel).length ? currentDaily.map(d => d.dayLabel) : days;
  const currentData = currentDaily.map(d => d.calories);
  const previousData = previousDaily.map(d => d.calories);

  dietCompChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: [data.currentPeriodLabel as string, data.previousPeriodLabel as string] },
    grid: { left: 50, right: 20, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: xLabels },
    yAxis: { type: 'value', name: 'kcal' },
    series: [
      {
        name: data.currentPeriodLabel as string,
        type: 'line',
        smooth: true,
        data: currentData,
        lineStyle: { color: '#2080f0', width: 2 },
        itemStyle: { color: '#2080f0' },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(32,128,240,0.3)' },
              { offset: 1, color: 'rgba(32,128,240,0.03)' }
            ]
          }
        }
      },
      {
        name: data.previousPeriodLabel as string,
        type: 'line',
        smooth: true,
        data: previousData,
        lineStyle: { color: '#999', width: 2, type: 'dashed' },
        itemStyle: { color: '#999' },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(139,148,158,0.2)' },
              { offset: 1, color: 'rgba(139,148,158,0.02)' }
            ]
          }
        }
      }
    ]
  });
}

function handleResize() {
  weightChart?.resize();
  checkinChart?.resize();
  dietCompChart?.resize();
}

// ===================== Tab Change =====================
async function handleTabChange(tabName: string) {
  tabError.value = null;
  await nextTick();
  setTimeout(() => handleResize(), 50);

  if (tabName === 'week' && !weekData.value) {
    try {
      const { data, error } = await fetchDashboardWeek();
      if (data && !error) {
        weekData.value = data as unknown as Record<string, unknown>;
      }
    } catch {
      tabError.value = $t('page.dashboard.dataLoadFailed');
    }
    try {
      const { data, error } = await fetchGetDietTrendComparison();
      if (data && !error) {
        dietComparison.value = data as unknown as Record<string, unknown>;
        await nextTick();
        initDietComparisonChart();
      }
    } catch {
      tabError.value = $t('page.dashboard.dataLoadFailed');
    }
  } else if (tabName === 'month' && !monthData.value) {
    try {
      const { data, error } = await fetchDashboardMonth();
      if (data && !error) {
        monthData.value = data as unknown as Record<string, unknown>;
      }
    } catch {
      tabError.value = $t('page.dashboard.dataLoadFailed');
    }
  }
}

// ===================== Greeting Actions =====================
function handleGreetingAction(action: Record<string, unknown>) {
  if (action.url) {
    router.push(action.url as string);
  }
}

// ===================== Format Helpers =====================
function formatSuggestions(text: string) {
  if (!text) return '';
  return text.replace(/\n/g, '<br>');
}

// ===================== Lifecycle =====================
onMounted(async () => {
  try {
    const [healthRes, todayRes, assessmentRes, progressRes, weightRes, checkinRes, recommendRes, greetingRes] =
      await Promise.allSettled([
        fetchGetLatestHealth(),
        fetchDashboardToday(),
        fetchGetHealthAssessment(),
        fetchGetProgress(),
        fetchGetWeightTrend({ days: 30 }),
        fetchGetCheckinTrend({ days: 30 }),
        fetchGetRecommendations(),
        fetchGreeting()
      ]);

    if (healthRes.status === 'fulfilled' && healthRes.value.data && !healthRes.value.error) {
      latestHealth.value = healthRes.value.data as unknown as Record<string, unknown>;
    }
    if (todayRes.status === 'fulfilled' && todayRes.value.data && !todayRes.value.error) {
      today.value = todayRes.value.data as unknown as Record<string, unknown>;
    }
    if (assessmentRes.status === 'fulfilled' && assessmentRes.value.data && !assessmentRes.value.error) {
      assessment.value = assessmentRes.value.data as unknown as Record<string, unknown>;
    }
    if (progressRes.status === 'fulfilled' && progressRes.value.data && !progressRes.value.error) {
      const p = progressRes.value.data as unknown as Record<string, unknown>;
      onProgress.value = {
        progressPercent: p.targetProgressPercent ? Number(p.targetProgressPercent) : 0,
        checkinRate: p.totalCheckinRate ? Number(p.totalCheckinRate) : 0,
        exerciseRate: p.exerciseCompleteRate ? Number(p.exerciseCompleteRate) : 0,
        dietRate: p.dietCompleteRate ? Number(p.dietCompleteRate) : 0,
        weightChange: p.weightChange ? Number(p.weightChange) : 0
      };
    }

    await nextTick();
    if (weightRes.status === 'fulfilled' && weightRes.value.data && !weightRes.value.error) {
      initWeightChart(weightRes.value.data as unknown as Record<string, unknown>);
    }
    if (checkinRes.status === 'fulfilled' && checkinRes.value.data && !checkinRes.value.error) {
      initCheckinChart(checkinRes.value.data as unknown as Record<string, unknown>);
    }
    if (recommendRes.status === 'fulfilled' && recommendRes.value.data && !recommendRes.value.error) {
      recommends.value = recommendRes.value.data as unknown as Record<string, unknown>;
    }
    if (greetingRes.status === 'fulfilled' && greetingRes.value.data && !greetingRes.value.error) {
      greetingCard.value = greetingRes.value.data as unknown as Record<string, unknown>;
    }
  } catch {
    // silent
  }
  dataLoading.value = false;

  window.addEventListener('resize', handleResize);
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
  weightChart?.dispose();
  checkinChart?.dispose();
  dietCompChart?.dispose();
});
</script>

<template>
  <div class="dashboard-page">
    <NSpin :show="dataLoading">
      <!-- Greeting Card -->
      <NCard v-if="greetingCard" class="greeting-card" :class="`card-${greetingCard.type || 'default'}`">
        <div class="greeting-content">
          <div class="greeting-header">
            <span class="greeting-emoji">{{ greetingCard.icon }}</span>
            <span class="greeting-time">{{ greetingCard.greeting }}</span>
            <NTag v-if="greetingCard.type === 'reminder'" type="error" size="small" round>
              {{ $t('page.dashboard.notCheckedIn') }}
            </NTag>
            <NTag v-if="greetingCard.type === 'celebration'" type="success" size="small" round>
              {{ $t('page.dashboard.isCheckedIn') }}
            </NTag>
          </div>
          <h3 class="greeting-message">{{ greetingCard.message }}</h3>
          <p v-if="greetingCard.detail" class="greeting-detail">{{ greetingCard.detail }}</p>
          <div v-if="(greetingCard.actions as unknown[])?.length" class="greeting-actions">
            <NButton
              v-for="action in (greetingCard.actions as Array<Record<string, unknown>>)"
              :key="action.label as string"
              :type="action.primary ? 'primary' : 'default'"
              size="small"
              @click="handleGreetingAction(action)"
            >
              {{ action.label }}
            </NButton>
          </div>
        </div>
        <div v-if="greetingCard.progress != null" class="greeting-progress">
          <NProgress
            type="line"
            :percentage="Number(greetingCard.progress)"
            :show-indicator="false"
            :color="progressColor"
          />
          <span class="progress-label">{{ greetingCard.progress }}{{ $t('page.dashboard.percent') }}</span>
        </div>
      </NCard>

      <!-- Health Overview Stats -->
      <NGrid :cols="4" :x-gap="16" :y-gap="16" responsive="screen" item-responsive class="stats-grid">
        <NGi span="4 m:2 l:1">
          <NCard class="stat-card" hoverable>
            <NStatistic :label="$t('page.dashboard.weight')">
              <span class="stat-value">{{ latestHealth.weight ?? '--' }}</span>
              <template #suffix>{{ $t('page.dashboard.kg') }}</template>
            </NStatistic>
          </NCard>
        </NGi>
        <NGi span="4 m:2 l:1">
          <NCard class="stat-card" hoverable>
            <NStatistic :label="$t('page.dashboard.bmi')">
              <span class="stat-value">{{ latestHealth.bmi ?? '--' }}</span>
            </NStatistic>
          </NCard>
        </NGi>
        <NGi span="4 m:2 l:1">
          <NCard class="stat-card" hoverable>
            <NStatistic :label="$t('page.dashboard.exerciseCaloriesBurned')">
              <span class="stat-value">{{ today.exerciseCaloriesBurned ?? '--' }}</span>
              <template #suffix>{{ $t('page.dashboard.kcal') }}</template>
            </NStatistic>
          </NCard>
        </NGi>
        <NGi span="4 m:2 l:1">
          <NCard class="stat-card" hoverable>
            <NStatistic :label="$t('page.dashboard.dietCaloriesConsumed')">
              <span class="stat-value">{{ today.dietCaloriesConsumed ?? '--' }}</span>
              <template #suffix>{{ $t('page.dashboard.kcal') }}</template>
            </NStatistic>
          </NCard>
        </NGi>
      </NGrid>

      <!-- Tab Section -->
      <NTabs v-model:value="activeTab" type="line" animated @update:value="handleTabChange">
        <!-- TODAY TAB -->
        <NTabPane name="today" :tab="$t('page.dashboard.today')">
          <!-- Checkin Status -->
          <NCard v-if="today.isCheckedIn !== undefined" class="section-card">
            <div class="checkin-row">
              <NTag :type="today.isCheckedIn ? 'success' : 'warning'" size="large" round>
                {{ today.isCheckedIn ? $t('page.dashboard.isCheckedIn') : $t('page.dashboard.notCheckedIn') }}
              </NTag>
              <NTag v-if="today.streakDays" type="success" round>
                {{ $t('page.dashboard.streakDays', { days: today.streakDays }) }}
              </NTag>
            </div>
          </NCard>

          <!-- Plan Progress -->
          <NGrid :cols="2" :x-gap="16" :y-gap="16" responsive="screen" item-responsive v-if="today.planName || today.totalTasks">
            <NGi v-if="today.planName" span="2 m:1">
              <NCard class="section-card">
                <div class="plan-progress">
                  <div class="plan-header">
                    <strong>{{ today.planName as string }}</strong>
                  </div>
                  <NProgress
                    type="line"
                    :percentage="today.totalTasks ? Math.round(((today.completedTasks as number) || 0) / (today.totalTasks as number) * 100) : 0"
                    :color="progressColor"
                    :rail-color="'rgba(0,0,0,0.08)'"
                  />
                  <div class="plan-hint">
                    {{ $t('page.dashboard.completedTasks', { completed: today.completedTasks ?? 0, total: today.totalTasks ?? 0 }) }}
                  </div>
                </div>
              </NCard>
            </NGi>
            <NGi span="2 m:1">
              <NCard class="section-card">
                <NGrid :cols="2" :x-gap="16">
                  <NGi>
                    <NStatistic :label="$t('page.dashboard.exerciseRecords')" :value="(today.exerciseRecordsCount as number) ?? 0">
                      <template #suffix>{{ $t('page.dashboard.items') }}</template>
                    </NStatistic>
                  </NGi>
                  <NGi>
                    <NStatistic :label="$t('page.dashboard.dietRecords')" :value="(today.dietRecordsCount as number) ?? 0">
                      <template #suffix>{{ $t('page.dashboard.items') }}</template>
                    </NStatistic>
                  </NGi>
                </NGrid>
              </NCard>
            </NGi>
          </NGrid>

          <!-- Today Tasks Table -->
          <NCard v-if="(today.tasks as unknown[])?.length" :title="$t('page.dashboard.todayTasks')" class="section-card">
            <NDataTable
              :data="(today.tasks as Array<Record<string, unknown>>)"
              :columns="[
                { title: $t('page.dashboard.taskName'), key: 'itemName' },
                { title: $t('page.dashboard.taskType'), key: 'itemType', width: 100,
                  render: (row: Record<string, unknown>) => h(NTag, { size: 'small', type: row.itemType === 'sport' ? 'success' : 'info' }, () => row.itemType === 'sport' ? $t('page.dashboard.sportType') : $t('page.dashboard.dietType'))
                },
                { title: $t('page.dashboard.taskTarget'), key: 'targetAmount', width: 100 },
                { title: $t('page.dashboard.taskStatus'), key: 'status', width: 100,
                  render: (row: Record<string, unknown>) => h(NTag, { size: 'small', type: row.status === 1 ? 'success' : 'default' }, () => row.status === 1 ? $t('page.dashboard.completed') : $t('page.dashboard.uncompleted'))
                }
              ]"
              size="small"
              striped
            />
          </NCard>

          <!-- Charts -->
          <NGrid :cols="2" :x-gap="16" :y-gap="16" responsive="screen" item-responsive>
            <NGi span="2 m:1">
              <NCard :title="`${$t('page.dashboard.weightTrend')} (${$t('page.dashboard.recent30Days')})`" class="chart-card">
                <div ref="weightChartRef" class="chart-container" />
              </NCard>
            </NGi>
            <NGi span="2 m:1">
              <NCard :title="`${$t('page.dashboard.checkinChart')} (${$t('page.dashboard.recent30Days')})`" class="chart-card">
                <div ref="checkinChartRef" class="chart-container" />
              </NCard>
            </NGi>
          </NGrid>
        </NTabPane>

        <!-- WEEK TAB -->
        <NTabPane name="week" :tab="$t('page.dashboard.week')">
          <template v-if="weekData">
            <NGrid :cols="4" :x-gap="16" :y-gap="16" responsive="screen" item-responsive>
              <NGi span="4 m:2 l:1">
                <NCard class="stat-card" hoverable>
                  <NStatistic :label="$t('page.dashboard.checkinDaysWeek')">
                    {{ weekData.checkinDays ?? 0 }}
                    <template #suffix>{{ $t('page.dashboard.days') }}</template>
                  </NStatistic>
                </NCard>
              </NGi>
              <NGi span="4 m:2 l:1">
                <NCard class="stat-card" hoverable>
                  <NStatistic :label="$t('page.dashboard.exerciseCaloriesWeek')">
                    {{ weekData.exerciseCalories ?? 0 }}
                    <template #suffix>{{ $t('page.dashboard.kcal') }}</template>
                  </NStatistic>
                </NCard>
              </NGi>
              <NGi span="4 m:2 l:1">
                <NCard class="stat-card" hoverable>
                  <NStatistic :label="$t('page.dashboard.dietCaloriesWeek')">
                    {{ weekData.dietCalories ?? 0 }}
                    <template #suffix>{{ $t('page.dashboard.kcal') }}</template>
                  </NStatistic>
                </NCard>
              </NGi>
              <NGi span="4 m:2 l:1">
                <NCard class="stat-card" hoverable>
                  <NStatistic :label="$t('page.dashboard.recordsCount')">
                    {{ weekData.exerciseRecordsCount ?? 0 }}/{{ weekData.dietRecordsCount ?? 0 }}
                  </NStatistic>
                </NCard>
              </NGi>
            </NGrid>

            <!-- Daily Detail Table -->
            <NCard v-if="weekData.dailySummary" :title="$t('page.dashboard.dailyDetail')" class="section-card">
              <NDataTable
                :data="(weekData.dailySummary as Array<Record<string, unknown>>)"
                :columns="[
                  { title: $t('page.dashboard.date'), key: 'date', width: 100 },
                  { title: $t('page.dashboard.checkinStatus'), key: 'checkedIn', width: 100,
                    render: (row: Record<string, unknown>) => h(NTag, { size: 'small', type: row.checkedIn ? 'success' : 'default' }, () => row.checkedIn ? $t('page.dashboard.checkedIn') : $t('page.dashboard.notChecked'))
                  },
                  { title: `${$t('page.dashboard.exerciseCaloriesWeek')} (${ $t('page.dashboard.kcal')})`, key: 'exerciseCalories' },
                  { title: `${$t('page.dashboard.dietCaloriesWeek')} (${ $t('page.dashboard.kcal')})`, key: 'dietCalories' },
                  { title: $t('page.dashboard.exerciseCount'), key: 'exerciseCount' },
                  { title: $t('page.dashboard.dietCount'), key: 'dietCount' }
                ]"
                size="small"
                striped
              />
            </NCard>

            <!-- Diet Comparison Chart -->
            <NCard :title="$t('page.dashboard.dietComparisonWeek')" class="chart-card">
              <div ref="dietCompChartRef" class="chart-container" />
            </NCard>
          </template>
        </NTabPane>

        <!-- MONTH TAB -->
        <NTabPane name="month" :tab="$t('page.dashboard.month')">
          <template v-if="monthData">
            <NGrid :cols="4" :x-gap="16" :y-gap="16" responsive="screen" item-responsive>
              <NGi span="4 m:2 l:1">
                <NCard class="stat-card" hoverable>
                  <NStatistic :label="$t('page.dashboard.checkinDaysMonth')">
                    {{ monthData.checkinDays ?? 0 }} / {{ monthData.totalDays ?? 0 }}
                    <template #suffix>{{ $t('page.dashboard.days') }}</template>
                  </NStatistic>
                </NCard>
              </NGi>
              <NGi span="4 m:2 l:1">
                <NCard class="stat-card" hoverable>
                  <NStatistic :label="$t('page.dashboard.checkinRate')">
                    {{ monthData.checkinRate ?? 0 }}
                    <template #suffix>{{ $t('page.dashboard.percent') }}</template>
                  </NStatistic>
                </NCard>
              </NGi>
              <NGi span="4 m:2 l:1">
                <NCard class="stat-card" hoverable>
                  <NStatistic :label="$t('page.dashboard.monthlyExerciseCalories')">
                    {{ monthData.exerciseCalories ?? 0 }}
                    <template #suffix>{{ $t('page.dashboard.kcal') }}</template>
                  </NStatistic>
                </NCard>
              </NGi>
              <NGi span="4 m:2 l:1">
                <NCard class="stat-card" hoverable>
                  <NStatistic :label="$t('page.dashboard.monthlyDietCalories')">
                    {{ monthData.dietCalories ?? 0 }}
                    <template #suffix>{{ $t('page.dashboard.kcal') }}</template>
                  </NStatistic>
                </NCard>
              </NGi>
            </NGrid>

            <NGrid :cols="2" :x-gap="16" :y-gap="16" responsive="screen" item-responsive>
              <NGi span="2 m:1">
                <NCard class="stat-card" hoverable>
                  <NStatistic :label="$t('page.dashboard.monthlyExerciseRecords')">
                    {{ monthData.exerciseRecordsCount ?? 0 }}
                    <template #suffix>{{ $t('page.dashboard.items') }}</template>
                  </NStatistic>
                </NCard>
              </NGi>
              <NGi span="2 m:1">
                <NCard class="stat-card" hoverable>
                  <NStatistic :label="$t('page.dashboard.monthlyDietRecords')">
                    {{ monthData.dietRecordsCount ?? 0 }}
                    <template #suffix>{{ $t('page.dashboard.items') }}</template>
                  </NStatistic>
                </NCard>
              </NGi>
            </NGrid>

            <!-- Weekly Summary -->
            <NCard v-if="(monthData.weeklySummary as unknown[])?.length" :title="$t('page.dashboard.weeklySummary')" class="section-card">
              <NDataTable
                :data="(monthData.weeklySummary as Array<Record<string, unknown>>)"
                :columns="[
                  { title: $t('page.dashboard.weekLabel'), key: 'weekLabel' },
                  { title: $t('page.dashboard.checkinDaysCount'), key: 'checkinDays' },
                  { title: `${$t('page.dashboard.exerciseCaloriesWeek')} (${ $t('page.dashboard.kcal')})`, key: 'exerciseCalories' },
                  { title: `${$t('page.dashboard.dietCaloriesWeek')} (${ $t('page.dashboard.kcal')})`, key: 'dietCalories' }
                ]"
                size="small"
                striped
              />
            </NCard>
          </template>
        </NTabPane>
      </NTabs>

      <!-- Error Alert -->
      <NAlert v-if="tabError" type="error" closable class="error-alert">
        <div class="flex items-center gap-2">
          <span>{{ tabError }}</span>
          <NButton size="small" type="primary" quaternary @click="handleTabChange(activeTab)">
            {{ $t('page.dashboard.retry') }}
          </NButton>
        </div>
      </NAlert>

      <!-- Bottom Section: Progress + Assessment -->
      <NGrid :cols="2" :x-gap="16" :y-gap="16" responsive="screen" item-responsive class="bottom-section">
        <!-- Health Goal Progress -->
        <NGi span="2 m:1">
          <NCard :title="$t('page.dashboard.healthGoalProgress')" class="section-card">
            <template v-if="onProgress">
              <div class="progress-content">
                <div class="progress-item">
                  <span>{{ $t('page.dashboard.goalRate') }}</span>
                  <NProgress type="line" :percentage="progressPercent" :color="progressColor" :rail-color="'rgba(0,0,0,0.08)'" />
                </div>
                <div class="progress-detail">
                  <div class="detail-item">
                    {{ $t('page.dashboard.checkinRateLabel') }} <strong>{{ onProgress.checkinRate ?? '--' }}{{ $t('page.dashboard.percent') }}</strong>
                  </div>
                  <div class="detail-item">
                    {{ $t('page.dashboard.exerciseRate') }} <strong>{{ onProgress.exerciseRate ?? '--' }}{{ $t('page.dashboard.percent') }}</strong>
                  </div>
                  <div class="detail-item">
                    {{ $t('page.dashboard.dietRate') }} <strong>{{ onProgress.dietRate ?? '--' }}{{ $t('page.dashboard.percent') }}</strong>
                  </div>
                  <div class="detail-item">
                    {{ $t('page.dashboard.weightChange') }} <strong>{{ onProgress.weightChange ?? '--' }} {{ $t('page.dashboard.kg') }}</strong>
                  </div>
                </div>
              </div>
            </template>
            <NEmpty v-else :description="$t('page.dashboard.noProgressData')" />
          </NCard>
        </NGi>

        <!-- Health Assessment -->
        <NGi span="2 m:1">
          <NCard :title="$t('page.dashboard.healthAssessment')" class="section-card">
            <template v-if="assessment">
              <div class="assessment-tags">
                <NTag :type="assessment.bmiLevel === '正常' ? 'success' : 'warning'" round>
                  {{ $t('page.dashboard.bmiLevel') }}: {{ assessment.bmiLevel ?? '--' }}
                </NTag>
                <NTag type="info" round>
                  {{ $t('page.dashboard.healthScore') }}: {{ assessment.healthScore ?? '--' }} {{ $t('page.dashboard.score') }}
                </NTag>
                <NTag v-if="(assessment.risks as string[])?.length" type="error" round>
                  {{ $t('page.dashboard.risk') }}: {{ (assessment.risks as string[])[0] }}
                </NTag>
              </div>
            </template>
            <NEmpty v-else :description="$t('page.dashboard.noHealthData')" />
          </NCard>
        </NGi>
      </NGrid>

      <!-- AI Recommendations -->
      <NGrid :cols="2" :x-gap="16" :y-gap="16" responsive="screen" item-responsive class="bottom-section">
        <!-- AI Exercise -->
        <NGi span="2 m:1">
          <NCard :title="$t('page.dashboard.aiExercise')" class="section-card">
            <div v-if="(recommends.exercises as unknown[])?.length" class="recommend-list">
              <div
                v-for="ex in (recommends.exercises as Array<Record<string, unknown>>).slice(0, 4)"
                :key="(ex.id as string)"
                class="recommend-item"
              >
                <NTag size="small" :type="ex.type === '有氧' ? 'success' : 'warning'" round>
                  {{ ex.type }}
                </NTag>
                <span class="item-name">{{ ex.name }}</span>
                <span class="item-meta">{{ $t('page.dashboard.caloriesPerHour', { cal: ex.caloriePerHour }) }}</span>
                <NTag v-if="ex.targetMuscle" size="small" round>{{ ex.targetMuscle }}</NTag>
              </div>
            </div>
            <NEmpty v-else :description="$t('page.dashboard.noRecommend')" size="small" />
          </NCard>
        </NGi>

        <!-- AI Food -->
        <NGi span="2 m:1">
          <NCard :title="$t('page.dashboard.aiFood')" class="section-card">
            <div v-if="(recommends.foods as unknown[])?.length" class="recommend-list">
              <div
                v-for="f in (recommends.foods as Array<Record<string, unknown>>).slice(0, 4)"
                :key="(f.id as string)"
                class="recommend-item"
              >
                <NTag size="small" round>{{ f.category }}</NTag>
                <span class="item-name">{{ f.name }}</span>
                <span class="item-meta">{{ $t('page.dashboard.caloriesPer100g', { cal: f.caloriePer100g }) }}</span>
                <span v-if="f.proteinPer100g" class="item-meta">{{ $t('page.dashboard.proteinPer100g', { protein: f.proteinPer100g }) }}</span>
              </div>
            </div>
            <NEmpty v-else :description="$t('page.dashboard.noRecommend')" size="small" />
          </NCard>
        </NGi>
      </NGrid>

      <!-- Health Tips -->
      <NCard v-if="(recommends.healthTips as string[])?.length" :title="$t('page.dashboard.healthTips')" class="section-card">
        <div class="tips-list">
          <div v-for="(tip, i) in (recommends.healthTips as string[])" :key="i" class="tip-item">
            <NTag size="tiny" type="info" round>Tip</NTag>
            <span>{{ tip }}</span>
          </div>
        </div>
      </NCard>

      <!-- AI Suggestions -->
      <NCard v-if="recommends.aiSuggestions" :title="$t('page.dashboard.aiSuggestions')" class="section-card ai-suggestion-card">
        <p class="ai-suggestion-text" v-html="formatSuggestions(recommends.aiSuggestions as string)" />
      </NCard>
    </NSpin>
  </div>
</template>

<style scoped>
.dashboard-page {
  padding: 16px 0;
}

/* Greeting Card */
.greeting-card {
  border-radius: 14px;
  margin-bottom: 16px;
  overflow: hidden;
  transition: transform 0.2s, box-shadow 0.2s;
}

.greeting-card:hover {
  transform: translateY(-2px);
}

.card-morning {
  background: linear-gradient(135deg, #1a237e 0%, #0d47a1 50%, #01579b 100%) !important;
  color: #fff;
}

.card-noon {
  background: linear-gradient(135deg, #1b5e20 0%, #2e7d32 50%, #388e3c 100%) !important;
  color: #fff;
}

.card-reminder {
  background: linear-gradient(135deg, #b71c1c 0%, #c62828 50%, #d32f2f 100%) !important;
  color: #fff;
}

.card-celebration {
  background: linear-gradient(135deg, #e65100 0%, #f57c00 50%, #ff9800 100%) !important;
  color: #fff;
}

.card-afternoon {
  background: linear-gradient(135deg, #4a148c 0%, #6a1b9a 50%, #7b1fa2 100%) !important;
  color: #fff;
}

.card-default {
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%) !important;
  color: #fff;
}

.greeting-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.greeting-emoji {
  font-size: 28px;
}

.greeting-time {
  font-size: 16px;
  font-weight: 600;
  opacity: 0.9;
}

.greeting-message {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 6px;
  line-height: 1.5;
}

.greeting-detail {
  font-size: 14px;
  opacity: 0.7;
  margin: 0 0 14px;
  line-height: 1.6;
}

.greeting-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.greeting-progress {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.15);
  display: flex;
  align-items: center;
  gap: 12px;
}

.greeting-progress .n-progress {
  flex: 1;
}

.progress-label {
  font-size: 13px;
  opacity: 0.8;
  white-space: nowrap;
}

/* Stats */
.stats-grid {
  margin-bottom: 16px;
}

.stat-card .stat-value {
  font-size: 24px;
  font-weight: 700;
}

/* Section */
.section-card {
  margin-bottom: 16px;
}

.checkin-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* Plan Progress */
.plan-progress {
  padding: 4px 0;
}

.plan-header {
  font-size: 14px;
  margin-bottom: 12px;
}

.plan-hint {
  font-size: 12px;
  color: var(--n-text-color-3, #999);
  margin-top: 6px;
}

/* Charts */
.chart-card {
  margin-bottom: 16px;
}

.chart-container {
  height: 280px;
}

/* Error */
.error-alert {
  margin-bottom: 16px;
}

/* Bottom */
.bottom-section {
  margin-top: 16px;
  margin-bottom: 16px;
}

.progress-content {
  padding: 8px 0;
}

.progress-item {
  margin-bottom: 20px;
}

.progress-item span {
  display: block;
  font-size: 13px;
  color: var(--n-text-color-3, #999);
  margin-bottom: 8px;
}

.progress-detail {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
}

.detail-item {
  font-size: 13px;
  color: var(--n-text-color-3, #999);
}

.detail-item strong {
  color: var(--n-text-color, #333);
}

/* Assessment */
.assessment-tags {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  padding: 8px 0;
}

/* Recommendations */
.recommend-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.recommend-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  background: var(--n-color-modal, #f5f5f5);
  border: 1px solid var(--n-border-color, #e8e8e8);
  font-size: 13px;
}

.recommend-item .item-name {
  font-weight: 500;
  flex: 1;
}

.recommend-item .item-meta {
  font-size: 12px;
  color: var(--n-text-color-3, #999);
}

/* Tips */
.tips-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.tip-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 0;
  font-size: 13px;
}

/* AI Suggestions */
.ai-suggestion-card {
  border-left: 3px solid #2080f0;
}

.ai-suggestion-text {
  color: var(--n-text-color-3, #999);
  font-size: 14px;
  line-height: 1.8;
  margin: 0;
}
</style>
