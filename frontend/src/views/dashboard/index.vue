<script setup lang="ts">
import { computed, h, nextTick, onMounted, onUnmounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { NDataTable, NTag, NGrid, NGi, NIcon, NButton, NSpace } from 'naive-ui';
import { Icon } from '@iconify/vue';
import SkeletonScreen from '@/components/SkeletonScreen.vue';
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
import { sanitizeHtml } from '@/utils/sanitize';

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

// 快速入口
const quickEntries = [
  { icon: 'mdi:heart-pulse', label: '健康档案', path: '/health/view', color: '#FF6B35', bgColor: 'rgba(255,107,53,0.1)' },
  { icon: 'mdi:run', label: '运动记录', path: '/exercise/record', color: '#4ECDC4', bgColor: 'rgba(78,205,196,0.1)' },
  { icon: 'mdi:food-apple', label: '饮食记录', path: '/food/record', color: '#FFB800', bgColor: 'rgba(255,184,0,0.1)' },
  { icon: 'mdi:calendar-check', label: '健康计划', path: '/plan/list', color: '#A855F7', bgColor: 'rgba(168,85,247,0.1)' },
  { icon: 'mdi:robot', label: 'AI助手', path: '/chat/chat-bot', color: '#4ECDC4', bgColor: 'rgba(78,205,196,0.1)' },
  { icon: 'mdi:chart-line', label: '数据统计', path: '/statistics/dashboard', color: '#FF6B6B', bgColor: 'rgba(255,107,107,0.1)' },
  { icon: 'mdi:scale-bathroom', label: '身体测量', path: '/body/measurement', color: '#C7F464', bgColor: 'rgba(199,244,100,0.1)' },
  { icon: 'mdi:calendar', label: '打卡日历', path: '/checkin/calendar', color: '#38BDF8', bgColor: 'rgba(56,189,248,0.1)' }
];

// ===================== Computed =====================
const progressPercent = computed(() => {
  return onProgress.value?.progressPercent ? Number(onProgress.value.progressPercent) : 0;
});

const progressColor = computed(() => {
  const rate = progressPercent.value;
  if (rate >= 80) return '#C7F464';
  if (rate >= 50) return '#4ECDC4';
  return '#FF6B35';
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
    xAxis: { type: 'category', data: dates, boundaryGap: false, axisLine: { lineStyle: { color: '#E5E7EB' } }, axisLabel: { color: '#9CA3AF' } },
    yAxis: { type: 'value', name: 'kg', splitLine: { lineStyle: { color: '#F0F0F2' } }, axisLabel: { color: '#9CA3AF' } },
    series: [{
      data: weights,
      type: 'line',
      smooth: true,
      lineStyle: { color: '#FF6B35', width: 3 },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(255,107,53,0.25)' },
            { offset: 1, color: 'rgba(255,107,53,0.02)' }
          ]
        }
      },
      itemStyle: { color: '#FF6B35' },
      symbol: 'circle',
      symbolSize: 6
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
    xAxis: { type: 'category', data: dates, axisLine: { lineStyle: { color: '#E5E7EB' } }, axisLabel: { color: '#9CA3AF' } },
    yAxis: { type: 'value', name: '%', max: 100, splitLine: { lineStyle: { color: '#F0F0F2' } }, axisLabel: { color: '#9CA3AF' } },
    series: [{
      data: counts,
      type: 'bar',
      barWidth: '50%',
      itemStyle: {
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: '#FF6B35' },
            { offset: 1, color: '#C7F464' }
          ]
        },
        borderRadius: [6, 6, 0, 0]
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
    legend: { data: [data.currentPeriodLabel as string, data.previousPeriodLabel as string], bottom: 0 },
    grid: { left: 50, right: 20, top: 10, bottom: 40 },
    xAxis: { type: 'category', data: xLabels, axisLine: { lineStyle: { color: '#E5E7EB' } }, axisLabel: { color: '#9CA3AF' } },
    yAxis: { type: 'value', name: 'kcal', splitLine: { lineStyle: { color: '#F0F0F2' } }, axisLabel: { color: '#9CA3AF' } },
    series: [
      {
        name: data.currentPeriodLabel as string,
        type: 'line',
        smooth: true,
        data: currentData,
        lineStyle: { color: '#4ECDC4', width: 2 },
        itemStyle: { color: '#4ECDC4' },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(78,205,196,0.2)' },
              { offset: 1, color: 'rgba(78,205,196,0.02)' }
            ]
          }
        }
      },
      {
        name: data.previousPeriodLabel as string,
        type: 'line',
        smooth: true,
        data: previousData,
        lineStyle: { color: '#9CA3AF', width: 2, type: 'dashed' },
        itemStyle: { color: '#9CA3AF' },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(156,163,175,0.15)' },
              { offset: 1, color: 'rgba(156,163,175,0.02)' }
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

// 快速入口跳转
function goToEntry(path: string) {
  router.push(path);
}

// ===================== Format Helpers =====================
function formatSuggestions(text: string) {
  if (!text) return '';
  const sanitized = sanitizeHtml(text);
  return sanitized.replace(/\n/g, '<br>');
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
    <!-- Skeleton loading state -->
    <SkeletonScreen v-if="dataLoading" :rows="8" />
    <!-- Loaded content -->
    <template v-else>
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

      <!-- 快速入口 -->
      <NCard class="quick-entries-card" :title="$t('page.dashboard.quickEntries')">
        <template #header-extra>
          <NButton text size="small">
            更多
            <NIcon size="14">
              <Icon icon="mdi:chevron-right" />
            </NIcon>
          </NButton>
        </template>
        <div class="quick-entries">
          <div
            v-for="entry in quickEntries"
            :key="entry.label"
            class="entry-item"
            @click="goToEntry(entry.path)"
          >
            <div class="entry-icon" :style="{ background: entry.bgColor, color: entry.color }">
              <NIcon size="28">
                <Icon :icon="entry.icon" />
              </NIcon>
            </div>
            <span class="entry-label">{{ entry.label }}</span>
          </div>
        </div>
      </NCard>

      <!-- Health Overview Stats -->
      <NCard class="stats-card" :title="$t('page.dashboard.healthOverview')">
        <NGrid :cols="4" :x-gap="16" :y-gap="16" responsive="screen" item-responsive class="stats-grid">
          <NGi span="4 m:2 l:1">
            <div class="stat-item">
              <div class="stat-icon" style="background: rgba(255,107,53,0.1); color: #FF6B35">
                <NIcon size="22">
                  <Icon icon="mdi:scale-bathroom" />
                </NIcon>
              </div>
              <div class="stat-info">
                <div class="stat-label">{{ $t('page.dashboard.weight') }}</div>
                <div class="stat-value">
                  {{ latestHealth.weight ?? '--' }}
                  <span class="stat-unit">{{ $t('page.dashboard.kg') }}</span>
                </div>
              </div>
            </div>
          </NGi>
          <NGi span="4 m:2 l:1">
            <div class="stat-item">
              <div class="stat-icon" style="background: rgba(78,205,196,0.1); color: #4ECDC4">
                <NIcon size="22">
                  <Icon icon="mdi:heart-pulse" />
                </NIcon>
              </div>
              <div class="stat-info">
                <div class="stat-label">{{ $t('page.dashboard.bmi') }}</div>
                <div class="stat-value">
                  {{ latestHealth.bmi ?? '--' }}
                </div>
              </div>
            </div>
          </NGi>
          <NGi span="4 m:2 l:1">
            <div class="stat-item">
              <div class="stat-icon" style="background: rgba(255,184,0,0.1); color: #FFB800">
                <NIcon size="22">
                  <Icon icon="mdi:fire" />
                </NIcon>
              </div>
              <div class="stat-info">
                <div class="stat-label">{{ $t('page.dashboard.exerciseCaloriesBurned') }}</div>
                <div class="stat-value">
                  {{ today.exerciseCaloriesBurned ?? '--' }}
                  <span class="stat-unit">{{ $t('page.dashboard.kcal') }}</span>
                </div>
              </div>
            </div>
          </NGi>
          <NGi span="4 m:2 l:1">
            <div class="stat-item">
              <div class="stat-icon" style="background: rgba(168,85,247,0.1); color: #A855F7">
                <NIcon size="22">
                  <Icon icon="mdi:food-apple" />
                </NIcon>
              </div>
              <div class="stat-info">
                <div class="stat-label">{{ $t('page.dashboard.dietCaloriesConsumed') }}</div>
                <div class="stat-value">
                  {{ today.dietCaloriesConsumed ?? '--' }}
                  <span class="stat-unit">{{ $t('page.dashboard.kcal') }}</span>
                </div>
              </div>
            </div>
          </NGi>
        </NGrid>
      </NCard>

      <!-- Tab Section -->
      <NCard class="tabs-card">
        <NTabs v-model:value="activeTab" type="line" animated @update:value="handleTabChange">
          <!-- TODAY TAB -->
          <NTabPane name="today" :tab="$t('page.dashboard.today')">
            <!-- Checkin Status -->
            <div v-if="today.isCheckedIn !== undefined" class="checkin-row">
              <NTag :type="today.isCheckedIn ? 'success' : 'warning'" size="large" round>
                {{ today.isCheckedIn ? $t('page.dashboard.isCheckedIn') : $t('page.dashboard.notCheckedIn') }}
              </NTag>
              <NTag v-if="today.streakDays" type="success" round>
                {{ $t('page.dashboard.streakDays', { days: today.streakDays }) }}
              </NTag>
            </div>

            <!-- Plan Progress -->
            <NGrid :cols="2" :x-gap="16" :y-gap="16" responsive="screen" item-responsive v-if="today.planName || today.totalTasks">
              <NGi v-if="today.planName" span="2 m:1">
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
              </NGi>
              <NGi span="2 m:1">
                <NGrid :cols="2" :x-gap="16">
                  <NGi>
                    <div class="mini-stat">
                      <div class="mini-stat-value">{{ (today.exerciseRecordsCount as number) ?? 0 }}</div>
                      <div class="mini-stat-label">{{ $t('page.dashboard.exerciseRecords') }}</div>
                    </div>
                  </NGi>
                  <NGi>
                    <div class="mini-stat">
                      <div class="mini-stat-value">{{ (today.dietRecordsCount as number) ?? 0 }}</div>
                      <div class="mini-stat-label">{{ $t('page.dashboard.dietRecords') }}</div>
                    </div>
                  </NGi>
                </NGrid>
              </NGi>
            </NGrid>

            <!-- Today Tasks Table -->
            <div v-if="(today.tasks as unknown[])?.length" class="section-title">{{ $t('page.dashboard.todayTasks') }}</div>
            <NDataTable
              v-if="(today.tasks as unknown[])?.length"
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
              :bordered="false"
              :scroll-x="1100"
            />

            <!-- Charts -->
            <NGrid :cols="2" :x-gap="16" :y-gap="16" responsive="screen" item-responsive class="charts-grid">
              <NGi span="2 m:1">
                <div class="chart-card">
                  <div class="chart-title">
                    {{ $t('page.dashboard.weightTrend') }}
                    <span class="chart-subtitle">{{ $t('page.dashboard.recent30Days') }}</span>
                  </div>
                  <div ref="weightChartRef" class="chart-container" />
                </div>
              </NGi>
              <NGi span="2 m:1">
                <div class="chart-card">
                  <div class="chart-title">
                    {{ $t('page.dashboard.checkinChart') }}
                    <span class="chart-subtitle">{{ $t('page.dashboard.recent30Days') }}</span>
                  </div>
                  <div ref="checkinChartRef" class="chart-container" />
                </div>
              </NGi>
            </NGrid>
          </NTabPane>

          <!-- WEEK TAB -->
          <NTabPane name="week" :tab="$t('page.dashboard.week')">
            <template v-if="weekData">
              <NGrid :cols="4" :x-gap="16" :y-gap="16" responsive="screen" item-responsive>
                <NGi span="4 m:2 l:1">
                  <div class="mini-stat">
                    <div class="mini-stat-value">{{ weekData.checkinDays ?? 0 }}</div>
                    <div class="mini-stat-label">{{ $t('page.dashboard.checkinDaysWeek') }}</div>
                  </div>
                </NGi>
                <NGi span="4 m:2 l:1">
                  <div class="mini-stat">
                    <div class="mini-stat-value">{{ weekData.exerciseCalories ?? 0 }}</div>
                    <div class="mini-stat-label">{{ $t('page.dashboard.exerciseCaloriesWeek') }} ({{ $t('page.dashboard.kcal') }})</div>
                  </div>
                </NGi>
                <NGi span="4 m:2 l:1">
                  <div class="mini-stat">
                    <div class="mini-stat-value">{{ weekData.dietCalories ?? 0 }}</div>
                    <div class="mini-stat-label">{{ $t('page.dashboard.dietCaloriesWeek') }} ({{ $t('page.dashboard.kcal') }})</div>
                  </div>
                </NGi>
                <NGi span="4 m:2 l:1">
                  <div class="mini-stat">
                    <div class="mini-stat-value">{{ weekData.exerciseRecordsCount ?? 0 }}/{{ weekData.dietRecordsCount ?? 0 }}</div>
                    <div class="mini-stat-label">{{ $t('page.dashboard.recordsCount') }}</div>
                  </div>
                </NGi>
              </NGrid>

              <!-- Daily Detail Table -->
              <div v-if="weekData.dailySummary" class="section-title">{{ $t('page.dashboard.dailyDetail') }}</div>
              <NDataTable
                v-if="weekData.dailySummary"
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
                :bordered="false"
                :scroll-x="1100"
              />

              <!-- Diet Comparison Chart -->
              <div class="chart-card">
                <div class="chart-title">{{ $t('page.dashboard.dietComparisonWeek') }}</div>
                <div ref="dietCompChartRef" class="chart-container" />
              </div>
            </template>
          </NTabPane>

          <!-- MONTH TAB -->
          <NTabPane name="month" :tab="$t('page.dashboard.month')">
            <template v-if="monthData">
              <NGrid :cols="4" :x-gap="16" :y-gap="16" responsive="screen" item-responsive>
                <NGi span="4 m:2 l:1">
                  <div class="mini-stat">
                    <div class="mini-stat-value">{{ monthData.checkinDays ?? 0 }} / {{ monthData.totalDays ?? 0 }}</div>
                    <div class="mini-stat-label">{{ $t('page.dashboard.checkinDaysMonth') }}</div>
                  </div>
                </NGi>
                <NGi span="4 m:2 l:1">
                  <div class="mini-stat">
                    <div class="mini-stat-value">{{ monthData.checkinRate ?? 0 }}{{ $t('page.dashboard.percent') }}</div>
                    <div class="mini-stat-label">{{ $t('page.dashboard.checkinRate') }}</div>
                  </div>
                </NGi>
                <NGi span="4 m:2 l:1">
                  <div class="mini-stat">
                    <div class="mini-stat-value">{{ monthData.exerciseCalories ?? 0 }}</div>
                    <div class="mini-stat-label">{{ $t('page.dashboard.monthlyExerciseCalories') }} ({{ $t('page.dashboard.kcal') }})</div>
                  </div>
                </NGi>
                <NGi span="4 m:2 l:1">
                  <div class="mini-stat">
                    <div class="mini-stat-value">{{ monthData.dietCalories ?? 0 }}</div>
                    <div class="mini-stat-label">{{ $t('page.dashboard.monthlyDietCalories') }} ({{ $t('page.dashboard.kcal') }})</div>
                  </div>
                </NGi>
              </NGrid>

              <NGrid :cols="2" :x-gap="16" :y-gap="16" responsive="screen" item-responsive>
                <NGi span="2 m:1">
                  <div class="mini-stat">
                    <div class="mini-stat-value">{{ monthData.exerciseRecordsCount ?? 0 }}</div>
                    <div class="mini-stat-label">{{ $t('page.dashboard.monthlyExerciseRecords') }}</div>
                  </div>
                </NGi>
                <NGi span="2 m:1">
                  <div class="mini-stat">
                    <div class="mini-stat-value">{{ monthData.dietRecordsCount ?? 0 }}</div>
                    <div class="mini-stat-label">{{ $t('page.dashboard.monthlyDietRecords') }}</div>
                  </div>
                </NGi>
              </NGrid>

              <!-- Weekly Summary -->
              <div v-if="(monthData.weeklySummary as unknown[])?.length" class="section-title">{{ $t('page.dashboard.weeklySummary') }}</div>
              <NDataTable
                v-if="(monthData.weeklySummary as unknown[])?.length"
                :data="(monthData.weeklySummary as Array<Record<string, unknown>>)"
                :columns="[
                  { title: $t('page.dashboard.weekLabel'), key: 'weekLabel' },
                  { title: $t('page.dashboard.checkinDaysCount'), key: 'checkinDays' },
                  { title: `${$t('page.dashboard.exerciseCaloriesWeek')} (${ $t('page.dashboard.kcal')})`, key: 'exerciseCalories' },
                  { title: `${$t('page.dashboard.dietCaloriesWeek')} (${ $t('page.dashboard.kcal')})`, key: 'dietCalories' }
                ]"
                size="small"
                striped
                :bordered="false"
                :scroll-x="1100"
              />
            </template>
          </NTabPane>
        </NTabs>
      </NCard>

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
    </template>
  </div>
</template>

<style scoped lang="scss">
.dashboard-page {
  padding: 16px 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* Greeting Card */
.greeting-card {
  border-radius: var(--radius-base);
  overflow: hidden;
  transition: transform 0.3s, box-shadow 0.3s;
  border: none;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
  }
}

.card-morning {
  background: var(--gradient-warm) !important;
  color: #fff;
}

.card-noon {
  background: var(--gradient-energy) !important;
  color: #fff;
}

.card-reminder {
  background: linear-gradient(135deg, #FF6B6B, #FFB800) !important;
  color: #fff;
}

.card-celebration {
  background: var(--gradient-vivid) !important;
  color: #fff;
}

.card-afternoon {
  background: var(--gradient-cool) !important;
  color: #fff;
}

.card-default {
  background: var(--gradient-warm) !important;
  color: #fff;
}

.greeting-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.greeting-emoji {
  font-size: 32px;
}

.greeting-time {
  font-size: 16px;
  font-weight: 600;
  opacity: 0.9;
}

.greeting-message {
  font-size: 20px;
  font-weight: 700;
  margin: 0 0 6px;
  line-height: 1.5;
}

.greeting-detail {
  font-size: 14px;
  opacity: 0.85;
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
  border-top: 1px solid rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  gap: 12px;
}

.greeting-progress :deep(.n-progress) {
  flex: 1;
}

.progress-label {
  font-size: 13px;
  opacity: 0.9;
  white-space: nowrap;
  font-weight: 500;
}

/* Quick Entries */
.quick-entries-card {
  border-radius: var(--radius-base);
  border: none;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.quick-entries {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 16px;
}

.entry-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: transform 0.2s;
  padding: 8px;
  border-radius: var(--radius-sm);

  &:hover {
    transform: translateY(-2px);
    background: var(--sport-primary-subtle);
  }
}

.entry-icon {
  width: 56px;
  height: 56px;
  border-radius: var(--radius-base);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
}

.entry-label {
  font-size: 13px;
  color: var(--sport-text-secondary);
  font-weight: 500;
  text-align: center;
}

/* Stats Card */
.stats-card {
  border-radius: var(--radius-base);
  border: none;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.stats-grid {
  margin-top: 8px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-info {
  flex: 1;
  min-width: 0;
}

.stat-label {
  font-size: 13px;
  color: var(--sport-text-secondary);
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: var(--sport-text-primary);
  line-height: 1.2;
}

.stat-unit {
  font-size: 13px;
  font-weight: 400;
  color: var(--sport-text-secondary);
  margin-left: 2px;
}

/* Tabs Card */
.tabs-card {
  border-radius: var(--radius-base);
  border: none;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.checkin-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--sport-text-primary);
  margin: 16px 0 12px;
}

/* Plan Progress */
.plan-progress {
  padding: 4px 0;
}

.plan-header {
  font-size: 14px;
  margin-bottom: 12px;
  color: var(--sport-text-primary);
}

.plan-hint {
  font-size: 12px;
  color: var(--sport-text-secondary);
  margin-top: 6px;
}

/* Mini Stat */
.mini-stat {
  text-align: center;
  padding: 12px 0;
}

.mini-stat-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--sport-primary);
  margin-bottom: 4px;
}

.mini-stat-label {
  font-size: 12px;
  color: var(--sport-text-secondary);
}

/* Charts */
.charts-grid {
  margin-top: 16px;
}

.chart-card {
  padding: 4px 0;
}

.chart-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--sport-text-primary);
  margin-bottom: 12px;
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.chart-subtitle {
  font-size: 12px;
  font-weight: 400;
  color: var(--sport-text-secondary);
}

.chart-container {
  height: 260px;
}

/* Error */
.error-alert {
  margin-bottom: 16px;
}

/* Bottom */
.bottom-section {
  margin-top: 0;
}

.section-card {
  border-radius: var(--radius-base);
  border: none;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
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
  color: var(--sport-text-secondary);
  margin-bottom: 8px;
  font-weight: 500;
}

.progress-detail {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
}

.detail-item {
  font-size: 13px;
  color: var(--sport-text-secondary);
}

.detail-item strong {
  color: var(--sport-text-primary);
  font-weight: 600;
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
  padding: 12px 14px;
  border-radius: var(--radius-sm);
  background: var(--sport-bg-elevated);
  border: 1px solid var(--sport-border-subtle);
  font-size: 13px;
  transition: all 0.2s;

  &:hover {
    background: var(--sport-bg-elevated);
    border-color: var(--sport-border);
  }
}

.recommend-item .item-name {
  font-weight: 500;
  flex: 1;
  color: var(--sport-text-primary);
}

.recommend-item .item-meta {
  font-size: 12px;
  color: var(--sport-text-secondary);
}

/* Tips */
.tips-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.tip-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: var(--radius-sm);
  background: var(--sport-primary-subtle);
  font-size: 13px;
  color: var(--sport-text-primary);
}

/* AI Suggestions */
.ai-suggestion-card {
  border-left: 4px solid var(--sport-primary) !important;
}

.ai-suggestion-text {
  color: var(--sport-text-secondary);
  font-size: 14px;
  line-height: 1.8;
  margin: 0;
}

/* Responsive */
@media (max-width: 1200px) {
  .quick-entries {
    grid-template-columns: repeat(4, 1fr);
  }
}

@media (max-width: 768px) {
  .quick-entries {
    grid-template-columns: repeat(4, 1fr);
    gap: 12px;
  }

  .entry-icon {
    width: 48px;
    height: 48px;
  }

  .entry-label {
    font-size: 12px;
  }

  .stat-value {
    font-size: 18px;
  }

  .chart-container {
    height: 220px;
  }

  .greeting-message {
    font-size: 18px;
  }
}

@media (max-width: 480px) {
  .quick-entries {
    grid-template-columns: repeat(3, 1fr);
  }
}
</style>
