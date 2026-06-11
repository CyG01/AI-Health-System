<template>
  <div class="body-page" v-loading="pageLoading">
    <div class="page-header">
      <h2>身体围度测量</h2>
      <p class="page-desc">定期记录围度数据，比单纯体重更能反映体型变化</p>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row" v-if="latest">
      <el-col :span="3">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-label">腰围</div>
          <div class="stat-value">{{ latest.waist ?? '--' }}<span class="stat-unit">cm</span></div>
        </el-card>
      </el-col>
      <el-col :span="3">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-label">臀围</div>
          <div class="stat-value">{{ latest.hip ?? '--' }}<span class="stat-unit">cm</span></div>
        </el-card>
      </el-col>
      <el-col :span="3">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-label">腰臀比</div>
          <div class="stat-value" :class="whrClass">{{ latest.waistHipRatio ?? '--' }}</div>
          <div class="stat-whr-tag">
            <el-tag :type="whrTagType" size="small">{{ whrLabel }}</el-tag>
          </div>
        </el-card>
      </el-col>
      <el-col :span="3">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-label">胸围</div>
          <div class="stat-value">{{ latest.chest ?? '--' }}<span class="stat-unit">cm</span></div>
        </el-card>
      </el-col>
      <el-col :span="3">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-label">大腿围</div>
          <div class="stat-value">{{ latest.thigh ?? '--' }}<span class="stat-unit">cm</span></div>
        </el-card>
      </el-col>
      <el-col :span="3">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-label">臂围</div>
          <div class="stat-value">{{ latest.arm ?? '--' }}<span class="stat-unit">cm</span></div>
        </el-card>
      </el-col>
      <el-col :span="3">
        <el-card class="stat-card" shadow="hover" :body-style="{ background: latest.bodyFatRate ? '#fff7e6' : '' }">
          <div class="stat-label">体脂率</div>
          <div class="stat-value">{{ latest.bodyFatRate ?? '--' }}<span class="stat-unit">%</span></div>
        </el-card>
      </el-col>
      <el-col :span="3">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-label">测量日期</div>
          <div class="stat-value date-value">{{ latest.recordDate }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 提交表单 -->
    <div class="submit-card glass-card">
      <h3 class="card-title">{{ latest ? '更新围度数据' : '首次记录围度' }}</h3>
      <el-form :model="form" label-width="80px" @submit.prevent="handleSubmit">
        <el-row :gutter="16">
          <el-col :span="3">
            <el-form-item label="日期">
              <el-date-picker v-model="form.recordDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="3">
            <el-form-item label="腰围(cm)">
              <el-input-number v-model="form.waist" :min="30" :max="200" :precision="1" :step="0.5" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="3">
            <el-form-item label="臀围(cm)">
              <el-input-number v-model="form.hip" :min="30" :max="200" :precision="1" :step="0.5" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="3">
            <el-form-item label="胸围(cm)">
              <el-input-number v-model="form.chest" :min="30" :max="200" :precision="1" :step="0.5" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="3">
            <el-form-item label="大腿围(cm)">
              <el-input-number v-model="form.thigh" :min="20" :max="120" :precision="1" :step="0.5" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="3">
            <el-form-item label="臂围(cm)">
              <el-input-number v-model="form.arm" :min="15" :max="80" :precision="1" :step="0.5" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="3">
            <el-form-item label="体脂率(%)">
              <el-input-number v-model="form.bodyFatRate" :min="1" :max="60" :precision="1" :step="0.5" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="3">
            <el-form-item label="">
              <el-button type="primary" native-type="submit" :loading="submitting">保存</el-button>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="form.note" placeholder="测量备注..." maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
    </div>

    <!-- 趋势图 -->
    <el-row :gutter="20" class="chart-row" v-if="trend.length > 1">
      <el-col :span="12">
        <el-card class="chart-card" shadow="hover">
          <template #header><span>围度趋势</span></template>
          <div class="chart-container" ref="waistChartRef"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="chart-card" shadow="hover">
          <template #header><span>腰臀比趋势</span></template>
          <div class="chart-container" ref="whrChartRef"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 历史记录 -->
    <div class="history-card glass-card">
      <h3 class="card-title">历史记录</h3>
      <el-table :data="history" stripe v-loading="pageLoading">
        <el-table-column prop="recordDate" label="日期" width="120" />
        <el-table-column label="腰围" width="90">
          <template #default="{ row }">{{ row.waist }}cm</template>
        </el-table-column>
        <el-table-column label="臀围" width="90">
          <template #default="{ row }">{{ row.hip }}cm</template>
        </el-table-column>
        <el-table-column label="腰臀比" width="100">
          <template #default="{ row }">{{ row.waistHipRatio ?? '--' }}</template>
        </el-table-column>
        <el-table-column label="胸围" width="90">
          <template #default="{ row }">{{ row.chest ?? '--' }}cm</template>
        </el-table-column>
        <el-table-column label="大腿围" width="90">
          <template #default="{ row }">{{ row.thigh ?? '--' }}cm</template>
        </el-table-column>
        <el-table-column label="臂围" width="90">
          <template #default="{ row }">{{ row.arm ?? '--' }}cm</template>
        </el-table-column>
        <el-table-column label="体脂率" width="90">
          <template #default="{ row }">{{ row.bodyFatRate ?? '--' }}%</template>
        </el-table-column>
        <el-table-column prop="note" label="备注" min-width="150" show-overflow-tooltip />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { submitBodyMeasurement, getLatestBodyMeasurement, getBodyMeasurementHistory, getBodyMeasurementTrend } from '@/api/bodyMeasurement'
import echarts from '@/utils/echarts'

const pageLoading = ref(false)
const submitting = ref(false)
const latest = ref(null)
const history = ref([])
const trend = ref([])
const waistChartRef = ref(null)
const whrChartRef = ref(null)
let waistChart = null
let whrChart = null

const form = reactive({
  recordDate: new Date().toISOString().slice(0, 10),
  waist: null,
  hip: null,
  chest: null,
  thigh: null,
  arm: null,
  bodyFatRate: null,
  note: ''
})

// 腰臀比评估
const whrLabel = computed(() => {
  const ratio = latest.value?.waistHipRatio
  if (!ratio) return '--'
  if (ratio < 0.85) return '健康'
  if (ratio < 0.9) return '亚健康'
  return '偏高'
})

const whrTagType = computed(() => {
  const ratio = latest.value?.waistHipRatio
  if (!ratio) return 'info'
  if (ratio < 0.85) return 'success'
  if (ratio < 0.9) return 'warning'
  return 'danger'
})

const whrClass = computed(() => {
  const ratio = latest.value?.waistHipRatio
  if (!ratio) return ''
  if (ratio < 0.85) return 'text-success'
  if (ratio < 0.9) return 'text-warning'
  return 'text-danger'
})

async function loadLatest() {
  try {
    const res = await getLatestBodyMeasurement()
    latest.value = res.data
    if (latest.value) {
      form.recordDate = latest.value.recordDate
      form.waist = latest.value.waist
      form.hip = latest.value.hip
      form.chest = latest.value.chest
      form.thigh = latest.value.thigh
      form.arm = latest.value.arm
      form.bodyFatRate = latest.value.bodyFatRate
      form.note = latest.value.note || ''
    }
  } catch { /* no data */ }
}

async function loadHistory() {
  pageLoading.value = true
  try {
    const res = await getBodyMeasurementHistory(20)
    history.value = res.data || []
  } finally {
    pageLoading.value = false
  }
}

async function loadTrend() {
  try {
    const res = await getBodyMeasurementTrend(6)
    trend.value = res.data || []
  } catch { /* ignore */ }
}

async function handleSubmit() {
  if (!form.waist || !form.hip || !form.chest) {
    ElMessage.warning('腰围、臀围和胸围为必填项')
    return
  }
  submitting.value = true
  try {
    await submitBodyMeasurement({ ...form })
    ElMessage.success('围度数据已保存')
    await loadLatest()
    await loadHistory()
    await loadTrend()
    await nextTick()
    initCharts()
  } finally {
    submitting.value = false
  }
}

function initCharts() {
  if (trend.value.length < 2) return

  if (!waistChart && waistChartRef.value) {
    waistChart = echarts.init(waistChartRef.value)
  }
  if (!whrChart && whrChartRef.value) {
    whrChart = echarts.init(whrChartRef.value)
  }

  const dates = trend.value.map(r => r.recordDate)

  if (waistChart) {
    waistChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['腰围', '臀围', '胸围'], bottom: 0 },
      grid: { left: 50, right: 20, top: 20, bottom: 30 },
      xAxis: { type: 'category', data: dates },
      yAxis: { type: 'value', name: 'cm' },
      series: [
        { name: '腰围', type: 'line', data: trend.value.map(r => r.waist), smooth: true, symbol: 'circle' },
        { name: '臀围', type: 'line', data: trend.value.map(r => r.hip), smooth: true, symbol: 'circle' },
        { name: '胸围', type: 'line', data: trend.value.map(r => r.chest), smooth: true, symbol: 'circle' }
      ]
    })
  }

  if (whrChart) {
    whrChart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 50, right: 20, top: 20, bottom: 20 },
      xAxis: { type: 'category', data: dates },
      yAxis: { type: 'value', name: '比率' },
      series: [
        {
          name: '腰臀比', type: 'line',
          data: trend.value.map(r => r.waistHipRatio),
          smooth: true, areaStyle: { opacity: 0.15 },
          markLine: {
            data: [
              { yAxis: 0.85, label: { formatter: '健康线 0.85' }, lineStyle: { color: '#52c41a' } },
              { yAxis: 0.9, label: { formatter: '警戒线 0.9' }, lineStyle: { color: '#fa8c16' } }
            ],
            silent: true
          }
        }
      ]
    })
  }
}

onMounted(async () => {
  await loadLatest()
  await loadHistory()
  await loadTrend()
  await nextTick()
  initCharts()
})

onUnmounted(() => {
  waistChart?.dispose()
  whrChart?.dispose()
})
</script>

<style scoped lang="scss">
.body-page { padding: 0 4px; }

.page-header {
  margin-bottom: 20px;
  h2 { margin: 0 0 6px; font-size: 22px; color: var(--text-primary); }
  .page-desc { margin: 0; font-size: 14px; color: var(--text-secondary); }
}

.stats-row { margin-bottom: 20px; }

.stat-card {
  text-align: center;
  .stat-label { font-size: 12px; color: var(--text-secondary); margin-bottom: 6px; }
  .stat-value { font-size: 24px; font-weight: 700; color: var(--text-primary); }
  .stat-unit { font-size: 13px; font-weight: 400; color: var(--text-secondary); margin-left: 2px; }
  .date-value { font-size: 15px !important; }
  .stat-whr-tag { margin-top: 4px; }
}

.text-success { color: #52c41a; }
.text-warning { color: #fa8c16; }
.text-danger { color: #ff4d4f; }

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

.chart-row { margin-bottom: 20px; }

.chart-card {
  .chart-container { width: 100%; height: 280px; }
}

.history-card {
  padding: 20px 24px;
}
</style>