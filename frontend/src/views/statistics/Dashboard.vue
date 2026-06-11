<template>
  <div class="statistics-page" v-loading="pageLoading">
    <div class="progress-bar glass-card">
      <div class="progress-item">
        <span class="progress-value text-green">{{ progress.totalCheckinRate }}%</span>
        <span class="progress-label">总完成率</span>
      </div>
      <div class="progress-divider" />
      <div class="progress-item">
        <span class="progress-value text-blue">{{ progress.exerciseCompleteRate }}%</span>
        <span class="progress-label">运动完成率</span>
      </div>
      <div class="progress-divider" />
      <div class="progress-item">
        <span class="progress-value text-amber">{{ progress.dietCompleteRate }}%</span>
        <span class="progress-label">饮食完成率</span>
      </div>
      <div class="progress-divider" />
      <div class="progress-item">
        <span class="progress-value" :class="weightChangeClass">{{ weightChangeDisplay }}</span>
        <span class="progress-label">体重变化(近30天)</span>
      </div>
      <div v-if="progress.goal" class="progress-divider" />
      <div v-if="progress.goal" class="progress-item">
        <span class="progress-value">{{ progress.goal }}</span>
        <span class="progress-label">当前目标</span>
      </div>
      <div class="progress-divider" />
      <div class="progress-item export-btn-wrapper">
        <el-dropdown @command="handleExport">
          <el-button size="small" type="default">
            导出数据 <el-icon><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="csv">导出 CSV</el-dropdown-item>
              <el-dropdown-item command="excel">导出 Excel</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <div class="charts-container">
      <div class="glass-card chart-card chart-full">
        <div class="chart-header">
          <span class="chart-title">体重变化趋势</span>
          <el-radio-group v-model="days" size="small" @change="loadAll">
            <el-radio-button :value="7">7天</el-radio-button>
            <el-radio-button :value="30">30天</el-radio-button>
            <el-radio-button :value="90">90天</el-radio-button>
          </el-radio-group>
        </div>
        <div class="chart-body">
          <BaseChart :option="weightOption" />
        </div>
      </div>

      <div class="glass-card chart-card chart-full">
        <div class="chart-header">
          <span class="chart-title">BMI 变化趋势</span>
        </div>
        <div class="chart-body">
          <BaseChart :option="bmiOption" />
        </div>
      </div>

      <div class="glass-card chart-card chart-half">
        <div class="chart-header">
          <span class="chart-title">打卡完成率</span>
        </div>
        <div class="chart-body">
          <BaseChart :option="checkinOption" />
        </div>
      </div>

      <div class="glass-card chart-card chart-half">
        <div class="chart-header">
          <span class="chart-title">每日运动时长</span>
        </div>
        <div class="chart-body">
          <BaseChart :option="exerciseOption" />
        </div>
      </div>

      <div class="glass-card chart-card chart-full">
        <div class="chart-header">
          <span class="chart-title">每日推荐热量摄入</span>
        </div>
        <div class="chart-body">
          <BaseChart :option="calorieOption" />
        </div>
      </div>

      <div class="glass-card chart-card chart-full">
        <div class="chart-header">
          <span class="chart-title">热量缺口分析（摄入 vs 消耗）</span>
        </div>
        <div class="chart-body">
          <BaseChart :option="calorieDeficitOption" />
        </div>
      </div>

      <div class="glass-card chart-card chart-half">
        <div class="chart-header">
          <span class="chart-title">营养素占比（近{{ days }}天）</span>
        </div>
        <div class="chart-body">
          <BaseChart :option="nutrientRatioOption" />
        </div>
      </div>

      <div class="glass-card chart-card chart-half">
        <div class="chart-header">
          <span class="chart-title">运动类型分布（近{{ days }}天）</span>
        </div>
        <div class="chart-body">
          <BaseChart :option="exerciseDistributionOption" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import echarts from '@/utils/echarts'
import BaseChart from '@/components/BaseChart.vue'
import {
  getWeightTrend,
  getBmiTrend,
  getCheckinTrend,
  getExerciseTrend,
  getCalorieTrend,
  getProgress,
  getCalorieDeficit,
  getNutrientRatio,
  getExerciseDistribution
} from '@/api/statistics'

const pageLoading = ref(false)
const days = ref(30)

const CHART_COLORS = {
  green: '#3fb950',
  blue: '#58a6ff',
  amber: '#d29922',
  red: '#f85149'
}

const CHART_AXIS = {
  textStyle: { color: '#8b949e' },
  axisLine: { lineStyle: { color: '#30363d' } },
  splitLine: { lineStyle: { color: 'rgba(48,54,61,0.5)' } }
}

const progress = reactive({
  totalCheckinRate: 0,
  exerciseCompleteRate: 0,
  dietCompleteRate: 0,
  weightChange: 0,
  goal: ''
})

const weightOption = ref({})
const bmiOption = ref({})
const checkinOption = ref({})
const exerciseOption = ref({})
const calorieOption = ref({})
const calorieDeficitOption = ref({})
const nutrientRatioOption = ref({})
const exerciseDistributionOption = ref({})

const weightChangeDisplay = computed(() => {
  const v = progress.weightChange
  if (v == null) return '--'
  return v > 0 ? `+${v} kg` : `${v} kg`
})

const weightChangeClass = computed(() => {
  const v = progress.weightChange
  if (v == null || v === 0) return ''
  return v > 0 ? 'text-red' : 'text-green'
})

function darkTooltip() {
  return {
    backgroundColor: 'rgba(22,27,34,0.95)',
    borderColor: '#30363d',
    textStyle: { color: '#c9d1d9', fontSize: 12 },
    extraCssText: 'backdrop-filter: blur(12px); border-radius: 6px; box-shadow: 0 4px 16px rgba(0,0,0,0.5);'
  }
}

async function loadAll() {
  pageLoading.value = true
  try {
    const params = { days: days.value }
    const [weightRes, bmiRes, checkinRes, exerciseRes, calorieRes, progressRes,
           deficitRes, nutrientRes, exDistributionRes] = await Promise.all([
      getWeightTrend(params),
      getBmiTrend(params),
      getCheckinTrend(params),
      getExerciseTrend(params),
      getCalorieTrend(params),
      getProgress(),
      getCalorieDeficit(params),
      getNutrientRatio(params),
      getExerciseDistribution(params)
    ])

    const w = weightRes.data || {}
    weightOption.value = buildLineOption(w.xAxis || [], w.yAxis || [], '体重 (kg)', CHART_COLORS.green)

    const b = bmiRes.data || {}
    bmiOption.value = buildLineOption(b.xAxis || [], b.yAxis || [], 'BMI', CHART_COLORS.blue)

    const c = checkinRes.data || {}
    checkinOption.value = buildBarOption(
      c.xAxis || [],
      (c.completeRate || []).map(v => `${v}%`),
      '完成率 (%)',
      CHART_COLORS.green
    )

    const e = exerciseRes.data || {}
    exerciseOption.value = buildLineOption(
      e.xAxis || [],
      e.minutesPerDay || [],
      '分钟/天',
      CHART_COLORS.green
    )

    const cal = calorieRes.data || {}
    calorieOption.value = buildLineOption(cal.xAxis || [], cal.dailyCalories || [], '千卡/天', CHART_COLORS.amber)

    const df = deficitRes.data || {}
    calorieDeficitOption.value = buildCalorieDeficitOption(
      df.xAxis || [], df.consumed || [], df.burned || [], df.net || []
    )

    const nr = nutrientRes.data || {}
    nutrientRatioOption.value = buildPieOption(
      nr.names || [], nr.values || [], '营养素占比 (g)'
    )

    const ed = exDistributionRes.data || {}
    exerciseDistributionOption.value = buildPieOption(
      ed.names || [], (ed.values || []).map(v => Number(v)), '运动次数'
    )

    const p = progressRes.data || {}
    Object.assign(progress, {
      totalCheckinRate: p.totalCheckinRate ?? 0,
      exerciseCompleteRate: p.exerciseCompleteRate ?? 0,
      dietCompleteRate: p.dietCompleteRate ?? 0,
      weightChange: p.weightChange ?? null,
      goal: p.goal || ''
    })
  } finally {
    pageLoading.value = false
  }
}

function buildLineOption(xAxisData, yData, unitLabel, color) {
  return {
    tooltip: {
      ...darkTooltip(),
      trigger: 'axis',
      formatter: (p) => {
        const item = Array.isArray(p) ? p[0] : p
        if (!item) return ''
        const val = item.value != null ? `${item.value} ${unitLabel}` : '无数据'
        return `<div style="font-size:12px;color:#8b949e;margin-bottom:4px">${item.axisValue}</div><div style="font-size:14px;color:#c9d1d9">${val}</div>`
      }
    },
    grid: { top: 12, right: 20, bottom: 28, left: 48 },
    xAxis: {
      type: 'category',
      data: xAxisData,
      ...CHART_AXIS
    },
    yAxis: {
      type: 'value',
      name: unitLabel,
      nameTextStyle: { color: '#8b949e', fontSize: 11 },
      ...CHART_AXIS,
      min: (val) => val.min,
      max: (val) => val.max
    },
    series: [{
      type: 'line',
      data: yData,
      smooth: true,
      symbol: 'circle',
      symbolSize: 4,
      lineStyle: { color, width: 2 },
      itemStyle: { color },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: color + '30' },
          { offset: 1, color: color + '05' }
        ])
      },
      connectNulls: true
    }]
  }
}

function buildBarOption(xAxisData, yData, unitLabel, color) {
  return {
    tooltip: {
      ...darkTooltip(),
      trigger: 'axis',
      formatter: (p) => {
        const item = Array.isArray(p) ? p[0] : p
        if (!item) return ''
        const val = item.value != null ? item.value : '无数据'
        return `<div style="font-size:12px;color:#8b949e;margin-bottom:4px">${item.axisValue}</div><div style="font-size:14px;color:#c9d1d9">${val}</div>`
      }
    },
    grid: { top: 12, right: 20, bottom: 28, left: 48 },
    xAxis: {
      type: 'category',
      data: xAxisData,
      ...CHART_AXIS
    },
    yAxis: {
      type: 'value',
      name: unitLabel,
      nameTextStyle: { color: '#8b949e', fontSize: 11 },
      ...CHART_AXIS,
      min: 0,
      max: 100
    },
    series: [{
      type: 'bar',
      data: yData,
      barWidth: '60%',
      itemStyle: {
        color,
        borderRadius: [4, 4, 0, 0],
        opacity: 0.85
      }
    }]
  }
}

function buildCalorieDeficitOption(xAxisData, consumed, burned, net) {
  return {
    tooltip: {
      ...darkTooltip(),
      trigger: 'axis'
    },
    legend: {
      data: ['摄入', '消耗', '净差值'],
      textStyle: { color: '#8b949e', fontSize: 11 },
      top: 0
    },
    grid: { top: 32, right: 20, bottom: 28, left: 48 },
    xAxis: { type: 'category', data: xAxisData, ...CHART_AXIS },
    yAxis: { type: 'value', name: 'kcal', nameTextStyle: { color: '#8b949e', fontSize: 11 }, ...CHART_AXIS },
    series: [
      { name: '摄入', type: 'bar', data: consumed, barGap: '10%', itemStyle: { color: '#f85149', borderRadius: [4, 4, 0, 0], opacity: 0.75 } },
      { name: '消耗', type: 'bar', data: burned, itemStyle: { color: '#58a6ff', borderRadius: [4, 4, 0, 0], opacity: 0.75 } },
      { name: '净差值', type: 'line', data: net, smooth: true, symbol: 'circle', symbolSize: 4, lineStyle: { color: '#3fb950', width: 2 }, itemStyle: { color: '#3fb950' } }
    ]
  }
}

function buildPieOption(names, values, title) {
  return {
    tooltip: { ...darkTooltip(), trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { orient: 'vertical', right: 8, top: 'center', textStyle: { color: '#8b949e', fontSize: 11 } },
    series: [{
      name: title,
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['35%', '50%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 4, borderColor: '#0d1117', borderWidth: 2 },
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
      data: names.map((n, i) => ({ name: n, value: values[i] || 0 }))
    }]
  }
}

onMounted(() => {
  loadAll()
})

const API_BASE = import.meta.env.VITE_API_BASE_URL || ''
function handleExport(type) {
  const token = localStorage.getItem('token')
  const url = `${API_BASE}/api/export/${type}`
  // Create a hidden link to trigger download with auth
  const link = document.createElement('a')
  link.href = url
  link.download = `health-data-${new Date().toISOString().split('T')[0]}.${type === 'excel' ? 'xlsx' : 'csv'}`
  // Add token as query param for auth
  link.href = `${url}?token=${encodeURIComponent(token)}`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  ElMessage.success(`正在下载${type.toUpperCase()}文件...`)
}
</script>

<style scoped lang="scss">
.statistics-page {
  padding: 4px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.progress-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 24px;
  padding: 16px 32px;
}

.progress-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.progress-value {
  font-size: 22px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: var(--text-primary);
}

.progress-value.text-green {
  color: #3fb950;
}

.progress-value.text-blue {
  color: #58a6ff;
}

.progress-value.text-amber {
  color: #d29922;
}

.progress-value.text-red {
  color: #f85149;
}

.progress-label {
  font-size: 12px;
  color: var(--text-secondary);
}

.progress-divider {
  width: 1px;
  height: 36px;
  background: #30363d;
}

.charts-container {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.chart-card {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chart-full {
  width: 100%;
}

.chart-half {
  width: calc(50% - 8px);
}

.chart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px 0;
}

.chart-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.chart-body {
  flex: 1;
  min-height: 220px;
  padding: 8px 8px 12px 4px;
}

.export-btn-wrapper {
  display: flex;
  align-items: center;
}
</style>
