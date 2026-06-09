<template>
  <div class="water-page" v-loading="pageLoading">
    <div class="page-header">
      <h2>饮水记录</h2>
      <p class="page-desc">每日建议饮水量：{{ dailyTarget }}ml</p>
    </div>

    <!-- 今日饮水概览 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <el-statistic title="今日饮水量" :value="todayAmount" suffix="ml">
            <template #suffix>
              <span class="stat-suffix">/ {{ dailyTarget }}ml</span>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <el-statistic title="完成度" :value="completionPercent" suffix="%" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <el-statistic title="近7天日均" :value="avgWeekly" suffix="ml" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <el-statistic title="总记录天数" :value="records.length" suffix="天" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 进度条 -->
    <div class="progress-card glass-card">
      <div class="progress-header">
        <span>今日饮水进度</span>
        <span class="progress-percent">{{ completionPercent }}%</span>
      </div>
      <el-progress
        :percentage="completionPercent"
        :stroke-width="20"
        :color="progressColor"
        :striped="true"
        :striped-flow="true"
      />
      <div class="progress-cups">
        <div
          v-for="i in 8"
          :key="i"
          class="cup-icon"
          :class="{ filled: todayAmount >= dailyTarget * i / 8 }"
        >
          <el-icon :size="28"><Dish /></el-icon>
          <span>{{ dailyTarget * i / 8 }}ml</span>
        </div>
      </div>
    </div>

    <!-- 快速记录 -->
    <div class="submit-card glass-card">
      <h3 class="card-title">快速记录饮水</h3>
      <div class="quick-buttons">
        <el-button
          v-for="opt in quickOptions"
          :key="opt.value"
          :type="opt.highlight ? 'primary' : 'default'"
          size="large"
          @click="quickAdd(opt.value)"
          :loading="submitting"
        >
          <el-icon v-if="opt.icon"><component :is="opt.icon" /></el-icon>
          {{ opt.label }}
        </el-button>
      </div>
      <el-divider />
      <el-form :model="form" inline @submit.prevent="handleSubmit">
        <el-form-item label="自定义">
          <el-input-number v-model="form.amountMl" :min="50" :max="1000" :step="50" />
          <span style="margin-left: 8px">ml</span>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" native-type="submit" :loading="submitting">记录</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 饮水趋势图 -->
    <div class="chart-card glass-card" v-if="records.length > 0">
      <h3 class="card-title">饮水趋势 (近7天)</h3>
      <div class="chart-container" ref="chartRef"></div>
    </div>

    <!-- 历史记录 -->
    <div class="history-card glass-card">
      <h3 class="card-title">饮水记录</h3>
      <el-table :data="records" stripe v-loading="pageLoading">
        <el-table-column prop="recordDate" label="日期" width="140" />
        <el-table-column label="饮水量" width="150">
          <template #default="{ row }">
            <b>{{ row.amountMl }}</b> ml
            <el-tag v-if="row.amountMl >= dailyTarget" type="success" size="small" effect="plain">达标</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="完成度" min-width="200">
          <template #default="{ row }">
            <el-progress
              :percentage="Math.min(100, Math.round(row.amountMl / dailyTarget * 100))"
              :stroke-width="12"
              :color="row.amountMl >= dailyTarget ? '#52c41a' : '#1890ff'"
            />
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { submitWater, getTodayWater, getWaterList } from '@/api/water'
import * as echarts from 'echarts'

const pageLoading = ref(false)
const submitting = ref(false)
const todayAmount = ref(0)
const records = ref([])
const chartRef = ref(null)
let chart = null

const dailyTarget = 2000

const form = reactive({
  amountMl: 250
})

const quickOptions = [
  { label: '100ml', value: 100 },
  { label: '200ml', value: 200 },
  { label: '250ml (一杯)', value: 250, icon: 'Dish', highlight: true },
  { label: '300ml', value: 300 },
  { label: '500ml', value: 500, icon: 'Dish' }
]

const completionPercent = computed(() => {
  return Math.min(100, Math.round(todayAmount.value / dailyTarget * 100))
})

const progressColor = computed(() => {
  const p = completionPercent.value
  if (p >= 100) return '#52c41a'
  if (p >= 50) return '#1890ff'
  if (p >= 25) return '#fa8c16'
  return '#ff4d4f'
})

const avgWeekly = computed(() => {
  if (records.value.length === 0) return 0
  return Math.round(records.value.reduce((s, r) => s + r.amountMl, 0) / Math.min(records.value.length, 7))
})

async function loadToday() {
  try {
    const res = await getTodayWater()
    todayAmount.value = res.data?.amountMl || 0
  } catch {
    todayAmount.value = 0
  }
}

async function loadRecords() {
  pageLoading.value = true
  try {
    const res = await getWaterList(7)
    records.value = res.data || []
  } finally {
    pageLoading.value = false
  }
}

async function quickAdd(amount) {
  submitting.value = true
  try {
    await submitWater({ amountMl: amount, recordDate: new Date().toISOString().slice(0, 10) })
    ElMessage.success(`已记录 ${amount}ml`)
    await loadToday()
    await loadRecords()
  } finally {
    submitting.value = false
  }
}

async function handleSubmit() {
  if (form.amountMl <= 0) {
    ElMessage.warning('请输入饮水量')
    return
  }
  submitting.value = true
  try {
    await submitWater({ amountMl: form.amountMl, recordDate: new Date().toISOString().slice(0, 10) })
    ElMessage.success('已记录')
    form.amountMl = 250
    await loadToday()
    await loadRecords()
  } finally {
    submitting.value = false
  }
}

function initChart() {
  if (!chartRef.value || records.value.length === 0) return
  if (!chart) {
    chart = echarts.init(chartRef.value)
  }
  const reversed = [...records.value].reverse()
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
          color: (params) => params.value >= dailyTarget ? '#52c41a' : '#1890ff',
          borderRadius: [4, 4, 0, 0]
        },
        markLine: {
          data: [{ yAxis: dailyTarget, name: '目标', label: { formatter: '目标 {c}ml' } }],
          lineStyle: { color: '#ff4d4f', type: 'dashed' }
        }
      }
    ]
  })
}

onMounted(async () => {
  await loadToday()
  await loadRecords()
  await nextTick()
  initChart()
})

onUnmounted(() => {
  chart?.dispose()
})
</script>

<style scoped lang="scss">
.water-page {
  padding: 0 4px;
}

.page-header {
  margin-bottom: 20px;

  h2 { margin: 0 0 6px; font-size: 22px; color: var(--text-primary); }
  .page-desc { margin: 0; font-size: 14px; color: var(--text-secondary); }
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  :deep(.el-statistic) {
    .el-statistic__head { font-size: 13px; color: var(--text-secondary); }
    .el-statistic__content { font-size: 28px; font-weight: 700; color: var(--text-primary); }
  }
}

.stat-suffix {
  font-size: 14px;
  color: var(--text-secondary);
  margin-left: 4px;
}

.progress-card {
  padding: 20px 24px;
  margin-bottom: 20px;

  .progress-header {
    display: flex;
    justify-content: space-between;
    margin-bottom: 12px;
    font-size: 15px;
    font-weight: 500;
    color: var(--text-primary);
  }
  .progress-percent { color: var(--brand-primary); font-weight: 600; }
}

.progress-cups {
  display: flex;
  justify-content: space-between;
  margin-top: 16px;

  .cup-icon {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    opacity: 0.3;
    transition: opacity 0.3s;

    &.filled { opacity: 1; color: #58a6ff; }
    span { font-size: 11px; color: var(--text-secondary); }
  }
}

.submit-card {
  padding: 20px 24px;
  margin-bottom: 20px;
}

.card-title {
  margin: 0 0 16px;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.quick-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.chart-card, .history-card {
  padding: 20px 24px;
  margin-bottom: 20px;
}

.chart-container {
  width: 100%;
  height: 280px;
}
</style>