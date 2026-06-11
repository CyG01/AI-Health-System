<template>
  <div class="dashboard">
    <!-- 健康总览卡片 -->
    <el-row :gutter="20" class="stats-cards">
      <el-col :xs="12" :sm="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon" style="background:#e6f7ff"><el-icon size="28" color="#1890ff"><Monitor /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ latestHealth.weight ?? '--' }}</div>
            <div class="stat-label">体重 (kg)</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon" style="background:#f6ffed"><el-icon size="28" color="#52c41a"><TrendCharts /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ latestHealth.bmi ?? '--' }}</div>
            <div class="stat-label">BMI</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon" style="background:#fff7e6"><el-icon size="28" color="#fa8c16"><Sunny /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ today.exerciseCaloriesBurned ?? '--' }}<span style="font-size:14px"> kcal</span></div>
            <div class="stat-label">今日运动消耗</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon" style="background:#fff1f0"><el-icon size="28" color="#ff4d4f"><Odometer /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ today.dietCaloriesConsumed ?? '--' }}<span style="font-size:14px"> kcal</span></div>
            <div class="stat-label">今日饮食热量</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 标签页 -->
    <el-tabs v-model="activeTab" class="dashboard-tabs" @tab-change="handleTabChange">
      <el-tab-pane label="今日" name="today">
        <!-- 打卡状态与连续天数 -->
        <el-row :gutter="20" class="stats-cards" v-if="today.isCheckedIn !== undefined">
          <el-col :span="24">
            <el-card shadow="hover">
              <div style="display:flex;align-items:center;gap:12px">
                <el-icon size="24" :color="today.isCheckedIn ? '#52c41a' : '#fa8c16'"><Calendar /></el-icon>
                <span style="font-size:16px;font-weight:600">
                  {{ today.isCheckedIn ? '今日已打卡' : '今日尚未打卡' }}
                </span>
                <el-tag v-if="today.streakDays" type="success" effect="plain" round>
                  连续打卡 {{ today.streakDays }} 天
                </el-tag>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <!-- 图表区 -->
        <el-row :gutter="20" class="chart-row">
          <el-col :span="12">
            <el-card class="chart-card" shadow="hover">
              <template #header><span>体重趋势 (近30天)</span></template>
              <div class="chart-container" ref="weightChartRef"></div>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card class="chart-card" shadow="hover">
              <template #header><span>打卡统计 (近30天)</span></template>
              <div class="chart-container" ref="checkinChartRef"></div>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <el-tab-pane label="本周" name="week">
        <el-row :gutter="20" class="stats-cards" v-if="weekData">
          <el-col :span="6">
            <el-card class="stat-card" shadow="hover">
              <div class="stat-info">
                <div class="stat-value">{{ weekData.checkinDays ?? 0 }}<span style="font-size:14px"> 天</span></div>
                <div class="stat-label">本周打卡</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card class="stat-card" shadow="hover">
              <div class="stat-info">
                <div class="stat-value">{{ weekData.exerciseCalories ?? 0 }}<span style="font-size:14px"> kcal</span></div>
                <div class="stat-label">运动消耗</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card class="stat-card" shadow="hover">
              <div class="stat-info">
                <div class="stat-value">{{ weekData.dietCalories ?? 0 }}<span style="font-size:14px"> kcal</span></div>
                <div class="stat-label">饮食摄入</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card class="stat-card" shadow="hover">
              <div class="stat-info">
                <div class="stat-value">{{ weekData.exerciseRecordsCount ?? 0 }}/{{ weekData.dietRecordsCount ?? 0 }}</div>
                <div class="stat-label">运动/饮食记录</div>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <!-- 每日明细 -->
        <el-card v-if="weekData?.dailySummary" class="chart-card" shadow="hover">
          <template #header><span>每日明细</span></template>
          <el-table :data="weekData.dailySummary" style="width:100%" stripe>
            <el-table-column prop="date" label="日期" width="100" />
            <el-table-column prop="checkedIn" label="打卡" width="80">
              <template #default="{ row }">
                <el-tag :type="row.checkedIn ? 'success' : 'info'" size="small">{{ row.checkedIn ? '已打卡' : '未打卡' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="exerciseCalories" label="运动消耗 (kcal)" />
            <el-table-column prop="dietCalories" label="饮食摄入 (kcal)" />
            <el-table-column prop="exerciseCount" label="运动次数" />
            <el-table-column prop="dietCount" label="饮食次数" />
          </el-table>
        </el-card>

        <!-- 饮食热量周对比 -->
        <el-row :gutter="20" class="chart-row">
          <el-col :span="24">
            <el-card class="chart-card" shadow="hover">
              <template #header>
                <span>饮食热量周对比</span>
                <span v-if="dietComparison" style="font-size:12px;color:#8b949e;margin-left:12px">
                  本周 {{ dietComparison.currentTotalCalories }}kcal vs 上周 {{ dietComparison.previousTotalCalories }}kcal
                  <el-tag :type="dietComparison.calorieChangePercent > 0 ? 'warning' : 'success'" size="small" style="margin-left:8px">
                    {{ dietComparison.calorieChangePercent > 0 ? '+' : '' }}{{ dietComparison.calorieChangePercent }}%
                  </el-tag>
                </span>
              </template>
              <div class="chart-container" ref="dietCompChartRef"></div>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <el-tab-pane label="本月" name="month">
        <el-row :gutter="20" class="stats-cards" v-if="monthData">
          <el-col :span="6">
            <el-card class="stat-card" shadow="hover">
              <div class="stat-info">
                <div class="stat-value">{{ monthData.checkinDays ?? 0 }} / {{ monthData.totalDays ?? 0 }}<span style="font-size:14px"> 天</span></div>
                <div class="stat-label">本月打卡 / 已过天数</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card class="stat-card" shadow="hover">
              <div class="stat-info">
                <div class="stat-value">{{ monthData.checkinRate ?? 0 }}<span style="font-size:14px">%</span></div>
                <div class="stat-label">打卡率</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card class="stat-card" shadow="hover">
              <div class="stat-info">
                <div class="stat-value">{{ monthData.exerciseCalories ?? 0 }}<span style="font-size:14px"> kcal</span></div>
                <div class="stat-label">运动消耗</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card class="stat-card" shadow="hover">
              <div class="stat-info">
                <div class="stat-value">{{ monthData.dietCalories ?? 0 }}<span style="font-size:14px"> kcal</span></div>
                <div class="stat-label">饮食摄入</div>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <!-- 按周汇总 -->
        <el-card v-if="monthData?.weeklySummary?.length" class="chart-card" shadow="hover">
          <template #header><span>按周汇总</span></template>
          <el-table :data="monthData.weeklySummary" style="width:100%" stripe>
            <el-table-column prop="weekLabel" label="周" />
            <el-table-column prop="checkinDays" label="打卡天数" />
            <el-table-column prop="exerciseCalories" label="运动消耗 (kcal)" />
            <el-table-column prop="dietCalories" label="饮食摄入 (kcal)" />
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 计划进度与健康评估（所有标签页共用） -->
    <el-row :gutter="20" class="bottom-row">
      <el-col :span="12">
        <el-card class="progress-card" shadow="hover">
          <template #header><span>健康目标进度</span></template>
          <div v-if="onProgress" class="progress-content">
            <div class="progress-item">
              <span>目标进度</span>
              <el-progress :percentage="onProgress.progressPercent ?? 0" :stroke-width="16" :color="progressColor" />
            </div>
            <div class="progress-detail">
              <div class="detail-item">打卡率 <b>{{ onProgress.checkinRate ?? '--' }}%</b></div>
              <div class="detail-item">运动完成率 <b>{{ onProgress.exerciseRate ?? '--' }}%</b></div>
              <div class="detail-item">饮食完成率 <b>{{ onProgress.dietRate ?? '--' }}%</b></div>
              <div class="detail-item">体重变化 <b>{{ onProgress.weightChange ?? '--' }} kg</b></div>
            </div>
          </div>
          <el-empty v-else description="暂无数据" :image-size="80" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="assessment-card" shadow="hover">
          <template #header><span>健康评估</span></template>
          <div v-if="assessment" class="assessment-content">
            <el-tag :type="assessment.bmiLevel === '正常' ? 'success' : 'warning'" size="default" effect="plain">
              BMI：{{ assessment.bmiLevel ?? '--' }}
            </el-tag>
            <el-tag type="info" size="default" effect="plain">
              健康评分：{{ assessment.healthScore ?? '--' }} 分
            </el-tag>
            <el-tag v-if="assessment.risks && assessment.risks.length > 0" type="danger" size="default" effect="plain">
              风险：{{ assessment.risks[0] }}
            </el-tag>
          </div>
          <el-empty v-else description="请先完善健康档案" :image-size="80" />
        </el-card>
      </el-col>
    </el-row>

    <!-- AI个性化推荐 -->
    <el-row :gutter="20" class="bottom-row">
      <el-col :span="12">
        <el-card class="recommend-card" shadow="hover">
          <template #header>
            <span><el-icon><MagicStick /></el-icon> AI推荐运动</span>
          </template>
          <div v-if="recommends.exercises?.length" class="recommend-list">
            <div v-for="ex in recommends.exercises.slice(0, 4)" :key="ex.id" class="recommend-item">
              <el-tag size="small" effect="plain" :type="ex.type === '有氧' ? 'success' : 'warning'">{{ ex.type }}</el-tag>
              <span class="item-name">{{ ex.name }}</span>
              <span class="item-meta">~{{ ex.caloriePerHour }}kcal/h</span>
              <el-tag v-if="ex.targetMuscle" size="small" effect="plain">{{ ex.targetMuscle }}</el-tag>
            </div>
          </div>
          <el-empty v-else description="暂无推荐" :image-size="60" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="recommend-card" shadow="hover">
          <template #header>
            <span><el-icon><Dish /></el-icon> 推荐健康饮食</span>
          </template>
          <div v-if="recommends.foods?.length" class="recommend-list">
            <div v-for="f in recommends.foods.slice(0, 4)" :key="f.id" class="recommend-item">
              <el-tag size="small" effect="plain">{{ f.category }}</el-tag>
              <span class="item-name">{{ f.name }}</span>
              <span class="item-meta">{{ f.caloriePer100g }}kcal/100g</span>
              <span v-if="f.proteinPer100g" class="item-meta">蛋白{{ f.proteinPer100g }}g</span>
            </div>
          </div>
          <el-empty v-else description="暂无推荐" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>

    <!-- AI健康建议 -->
    <el-row :gutter="20" v-if="recommends.aiSuggestions">
      <el-col :span="24">
        <el-card class="ai-suggestion-card" shadow="hover">
          <template #header>
            <span><el-icon><ChatDotSquare /></el-icon> AI个性化建议</span>
          </template>
          <p class="ai-suggestion-text" v-html="formatSuggestions(recommends.aiSuggestions)"></p>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { getLatestHealth, getHealthAssessment } from '@/api/health'
import { getDashboardToday, getDashboardWeek, getDashboardMonth } from '@/api/dashboard'
import { getWeightTrend, getCheckinTrend, getProgress, getDietTrendComparison } from '@/api/statistics'
import { getRecommendations } from '@/api/recommend'
import echarts from '@/utils/echarts'
import { sanitizeHtml } from '@/utils/sanitize'

const latestHealth = ref({})
const today = ref({})
const assessment = ref(null)
const onProgress = ref(null)
const recommends = ref({})
const weightChartRef = ref(null)
const checkinChartRef = ref(null)
const dietCompChartRef = ref(null)
let weightChart = null
let checkinChart = null
let dietCompChart = null

const activeTab = ref('today')
const weekData = ref(null)
const monthData = ref(null)
const dietComparison = ref(null)

const progressColor = computed(() => {
  const rate = onProgress.value?.progressPercent ?? 0
  if (rate >= 80) return '#52c41a'
  if (rate >= 50) return '#1890ff'
  return '#fa8c16'
})

// 初始化体重趋势图表
function initWeightChart(data) {
  if (!weightChartRef.value) return
  if (!weightChart) {
    weightChart = echarts.init(weightChartRef.value)
  }
  const dates = data.xAxis || []
  const weights = data.yAxis || []
  weightChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 10, bottom: 30 },
    xAxis: { type: 'category', data: dates, boundaryGap: false },
    yAxis: { type: 'value', name: 'kg' },
    series: [{
      data: weights, type: 'line', smooth: true,
      lineStyle: { color: '#1890ff', width: 2 },
      areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
        { offset: 0, color: 'rgba(24,144,255,0.3)' },
        { offset: 1, color: 'rgba(24,144,255,0.05)' }
      ]) },
      itemStyle: { color: '#1890ff' }
    }]
  })
}

// 初始化打卡统计图表
function initCheckinChart(data) {
  if (!checkinChartRef.value) return
  if (!checkinChart) {
    checkinChart = echarts.init(checkinChartRef.value)
  }
  const dates = data.xAxis || []
  const counts = data.totalDays || []
  checkinChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 10, bottom: 30 },
    xAxis: { type: 'category', data: dates },
    yAxis: { type: 'value', name: '次' },
    series: [{
      data: counts, type: 'bar',
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#52c41a' },
          { offset: 1, color: '#b7eb8f' }
        ]),
        borderRadius: [4, 4, 0, 0]
      }
    }]
  })
}

function handleResize() {
  weightChart?.resize()
  checkinChart?.resize()
  dietCompChart?.resize()
}

function formatSuggestions(text) {
  if (!text) return ''
  return sanitizeHtml(text.replace(/\n/g, '<br>'))
}

async function handleTabChange(tabName) {
  // 切换标签页后延迟 resize 图表，确保容器尺寸已更新
  await nextTick()
  setTimeout(() => handleResize(), 50)

  if (tabName === 'week' && !weekData.value) {
    try {
      const res = await getDashboardWeek()
      weekData.value = res.data
    } catch { /* ignore */ }
    // 加载饮食对比数据
    try {
      const compRes = await getDietTrendComparison()
      dietComparison.value = compRes.data
      await nextTick()
      initDietComparisonChart()
    } catch { /* ignore */ }
  } else if (tabName === 'month' && !monthData.value) {
    try {
      const res = await getDashboardMonth()
      monthData.value = res.data
    } catch { /* ignore */ }
  }
}

/** 初始化饮食热量周对比图表 */
function initDietComparisonChart() {
  if (!dietCompChartRef.value || !dietComparison.value) return
  if (!dietCompChart) {
    dietCompChart = echarts.init(dietCompChartRef.value)
  }
  const data = dietComparison.value
  const days = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
  const xLabels = data.currentDaily?.map(d => d.dayLabel) || days
  const currentData = data.currentDaily?.map(d => d.calories) || []
  const previousData = data.previousDaily?.map(d => d.calories) || []

  dietCompChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: [data.currentPeriodLabel, data.previousPeriodLabel], textStyle: { color: '#8b949e' } },
    grid: { left: 50, right: 20, top: 40, bottom: 30 },
    xAxis: {
      type: 'category', data: xLabels,
      axisLabel: { color: '#8b949e' }
    },
    yAxis: {
      type: 'value', name: 'kcal',
      axisLabel: { color: '#8b949e' }
    },
    series: [
      {
        name: data.currentPeriodLabel,
        type: 'line', smooth: true, data: currentData,
        lineStyle: { color: '#58a6ff', width: 2 },
        itemStyle: { color: '#58a6ff' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(88,166,255,0.3)' },
            { offset: 1, color: 'rgba(88,166,255,0.03)' }
          ])
        }
      },
      {
        name: data.previousPeriodLabel,
        type: 'line', smooth: true, data: previousData,
        lineStyle: { color: '#8b949e', width: 2, type: 'dashed' },
        itemStyle: { color: '#8b949e' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(139,148,158,0.2)' },
            { offset: 1, color: 'rgba(139,148,158,0.02)' }
          ])
        }
      }
    ]
  })
}

onMounted(async () => {
  try {
    // 并发请求所有数据
    const [healthRes, todayRes, assessmentRes, progressRes, weightRes, checkinRes, recommendRes] = await Promise.allSettled([
      getLatestHealth(),
      getDashboardToday(),
      getHealthAssessment(),
      getProgress(),
      getWeightTrend({ days: 30 }),
      getCheckinTrend({ days: 30 }),
      getRecommendations()
    ])

    if (healthRes.status === 'fulfilled' && healthRes.value.data) {
      latestHealth.value = healthRes.value.data
    }
    if (todayRes.status === 'fulfilled' && todayRes.value.data) {
      today.value = todayRes.value.data
    }
    if (assessmentRes.status === 'fulfilled' && assessmentRes.value.data) {
      assessment.value = assessmentRes.value.data
    }
    if (progressRes.status === 'fulfilled' && progressRes.value.data) {
      const p = progressRes.value.data
      onProgress.value = {
        progressPercent: p.targetProgressPercent ? Number(p.targetProgressPercent) : 0,
        checkinRate: p.totalCheckinRate ? Number(p.totalCheckinRate) : 0,
        exerciseRate: p.exerciseCompleteRate ? Number(p.exerciseCompleteRate) : 0,
        dietRate: p.dietCompleteRate ? Number(p.dietCompleteRate) : 0,
        weightChange: p.weightChange ? Number(p.weightChange) : 0
      }
    }

    await nextTick()
    if (weightRes.status === 'fulfilled' && weightRes.value.data) {
      initWeightChart(weightRes.value.data)
    }
    if (checkinRes.status === 'fulfilled' && checkinRes.value.data) {
      initCheckinChart(checkinRes.value.data)
    }
    if (recommendRes.status === 'fulfilled' && recommendRes.value.data) {
      recommends.value = recommendRes.value.data
    }
  } catch {
    // 静默处理
  }

  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  weightChart?.dispose()
  checkinChart?.dispose()
  dietCompChart?.dispose()
})
</script>

<style scoped>
.dashboard { padding: 20px 0; }

.stats-cards { margin-bottom: 20px; }
.stat-card {
  background: #161b22; border-color: #30363d;
  :deep(.el-card__body) {
    display: flex; align-items: center; gap: 16px; padding: 20px;
  }
  :deep(.el-card__header) { border-color: #21262d; color: #e6edf3; }
}
.stat-icon {
  width: 56px; height: 56px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
}
.stat-info .stat-value { font-size: 24px; font-weight: 700; color: #e6edf3; }
.stat-info .stat-label { font-size: 13px; color: #8b949e; margin-top: 2px; }

.chart-row { margin-bottom: 20px; }
.chart-container { height: 280px; }

.bottom-row { margin-bottom: 20px; }

.progress-content { padding: 8px 0; }
.progress-item { margin-bottom: 20px; }
.progress-item span { display: block; font-size: 13px; color: #8b949e; margin-bottom: 8px; }
.progress-detail { display: flex; gap: 24px; flex-wrap: wrap; }
.detail-item { font-size: 13px; color: #8b949e; }
.detail-item b { color: #e6edf3; }

.assessment-content { display: flex; gap: 12px; flex-wrap: wrap; padding: 16px 0; }

/* 推荐样式 */
.recommend-card {
  background: #161b22; border-color: #30363d;
  :deep(.el-card__header) { border-color: #21262d; color: #e6edf3; display: flex; align-items: center; gap: 6px; }
}

.recommend-list { display: flex; flex-direction: column; gap: 10px; }

.recommend-item {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 12px; border-radius: 8px;
  background: #0d1117; border: 1px solid #21262d;
  font-size: 13px;
}

.recommend-item .item-name {
  color: #e6edf3; font-weight: 500; flex: 1;
}

.recommend-item .item-meta {
  color: #8b949e; font-size: 12px;
}

/* AI建议 */
.ai-suggestion-card {
  background: #161b22; border-color: rgba(88, 166, 255, 0.25);
  :deep(.el-card__header) {
    border-color: #21262d; color: #58a6ff; display: flex; align-items: center; gap: 6px;
  }
}

.ai-suggestion-text {
  color: #c9d1d9; font-size: 14px; line-height: 1.8; margin: 0;
}

/* 深色卡片统一 */
:deep(.el-card) { background: #161b22; border-color: #30363d; }
:deep(.el-card__header) { border-color: #21262d; color: #e6edf3; }
</style>