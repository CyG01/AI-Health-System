<template>
  <div class="dashboard">
    <!-- 健康总览卡片 -->
    <el-row :gutter="20" class="stats-cards">
      <el-col :xs="12" :sm="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon" style="background:#e6f7ff"><el-icon size="28" color="#1890ff"><Monitor /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ healthSummary.weight ?? '--' }}</div>
            <div class="stat-label">体重 (kg)</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon" style="background:#f6ffed"><el-icon size="28" color="#52c41a"><TrendCharts /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ healthSummary.bmi ?? '--' }}</div>
            <div class="stat-label">BMI</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon" style="background:#fff7e6"><el-icon size="28" color="#fa8c16"><Sunny /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ healthSummary.exerciseMinutes ?? '--' }}<span style="font-size:14px"> min</span></div>
            <div class="stat-label">今日运动</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon" style="background:#fff1f0"><el-icon size="28" color="#ff4d4f"><Odometer /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ healthSummary.calories ?? '--' }}<span style="font-size:14px"> kcal</span></div>
            <div class="stat-label">今日热量</div>
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

    <!-- 计划进度与健康评估 -->
    <el-row :gutter="20" class="bottom-row">
      <el-col :span="12">
        <el-card class="progress-card" shadow="hover">
          <template #header><span>计划执行进度</span></template>
          <div v-if="planProgress" class="progress-content">
            <div class="progress-item">
              <span>完成率</span>
              <el-progress :percentage="planProgress.completionRate ?? 0" :stroke-width="16" :color="progressColor" />
            </div>
            <div class="progress-detail">
              <div class="detail-item">已完成 <b>{{ planProgress.completedDays ?? 0 }}</b> 天</div>
              <div class="detail-item">总天数 <b>{{ planProgress.totalDays ?? '--' }}</b> 天</div>
              <div class="detail-item">连续打卡 <b>{{ planProgress.consecutiveDays ?? 0 }}</b> 天</div>
            </div>
          </div>
          <el-empty v-else description="暂无计划" :image-size="80" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="assessment-card" shadow="hover">
          <template #header><span>健康评估</span></template>
          <div v-if="assessment" class="assessment-content">
            <el-tag :type="assessment.weightStatus === '正常' ? 'success' : 'warning'" size="default" effect="plain">
              体重：{{ assessment.weightStatus ?? '--' }}
            </el-tag>
            <el-tag :type="assessment.bmiStatus === '正常' ? 'success' : 'warning'" size="default" effect="plain">
              BMI：{{ assessment.bmiStatus ?? '--' }}
            </el-tag>
            <el-tag type="info" size="default" effect="plain">
              标准体重：{{ assessment.standardWeight }} kg
            </el-tag>
          </div>
          <el-empty v-else description="请先完善健康档案" :image-size="80" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { getLatestHealth, getHealthAssessment } from '@/api/health'
import { getWeightTrend, getCheckinTrend, getProgress } from '@/api/statistics'
import * as echarts from 'echarts'

const healthSummary = ref({})
const assessment = ref(null)
const planProgress = ref(null)
const weightChartRef = ref(null)
const checkinChartRef = ref(null)
let weightChart = null
let checkinChart = null

const progressColor = computed(() => {
  const rate = planProgress.value?.completionRate ?? 0
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
  const dates = data.map(item => item.date)
  const weights = data.map(item => item.weight)
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
  const dates = data.map(item => item.date)
  const counts = data.map(item => item.count)
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
}

onMounted(async () => {
  try {
    // 并发请求所有数据
    const [healthRes, assessmentRes, progressRes, weightRes, checkinRes] = await Promise.allSettled([
      getLatestHealth(),
      getHealthAssessment(),
      getProgress(),
      getWeightTrend({ days: 30 }),
      getCheckinTrend({ days: 30 })
    ])

    if (healthRes.status === 'fulfilled' && healthRes.value.data) {
      healthSummary.value = healthRes.value.data
    }
    if (assessmentRes.status === 'fulfilled' && assessmentRes.value.data) {
      assessment.value = assessmentRes.value.data
    }
    if (progressRes.status === 'fulfilled' && progressRes.value.data) {
      planProgress.value = progressRes.value.data
    }

    await nextTick()
    if (weightRes.status === 'fulfilled' && weightRes.value.data) {
      initWeightChart(weightRes.value.data)
    }
    if (checkinRes.status === 'fulfilled' && checkinRes.value.data) {
      initCheckinChart(checkinRes.value.data)
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
})
</script>

<style scoped>
.dashboard { padding: 20px 0; }

.stats-cards { margin-bottom: 20px; }
.stat-card {
  :deep(.el-card__body) {
    display: flex; align-items: center; gap: 16px; padding: 20px;
  }
}
.stat-icon {
  width: 56px; height: 56px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
}
.stat-info .stat-value { font-size: 24px; font-weight: 700; color: #262626; }
.stat-info .stat-label { font-size: 13px; color: #8c8c8c; margin-top: 2px; }

.chart-row { margin-bottom: 20px; }
.chart-container { height: 280px; }

.bottom-row { margin-bottom: 20px; }

.progress-content { padding: 8px 0; }
.progress-item { margin-bottom: 20px; }
.progress-item span { display: block; font-size: 13px; color: #8c8c8c; margin-bottom: 8px; }
.progress-detail { display: flex; gap: 24px; }
.detail-item { font-size: 13px; color: #595959; }
.detail-item b { color: #262626; }

.assessment-content { display: flex; gap: 12px; flex-wrap: wrap; padding: 16px 0; }
</style>