<template>
  <div class="blood-sugar-page">
    <div class="page-header">
      <h2>血糖监测</h2>
    </div>

    <!-- 提交血糖记录 -->
    <el-card class="submit-card" shadow="hover">
      <template #header><span>记录血糖</span></template>
      <el-form :model="form" label-width="80px" @submit.prevent="handleSubmit">
        <el-row :gutter="20">
          <el-col :span="4">
            <el-form-item label="日期">
              <el-date-picker
                v-model="form.recordDate"
                type="date"
                placeholder="选择日期"
                value-format="YYYY-MM-DD"
                style="width:100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item label="时间">
              <el-time-picker
                v-model="form.recordTime"
                placeholder="选择时间"
                format="HH:mm"
                value-format="HH:mm"
                style="width:100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item label="类型">
              <el-select v-model="form.measureType" style="width:100%">
                <el-option label="空腹" value="fasting" />
                <el-option label="餐前" value="before_meal" />
                <el-option label="餐后" value="after_meal" />
                <el-option label="睡前" value="bedtime" />
                <el-option label="随机" value="random" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item label="血糖值">
              <el-input-number v-model="form.glucoseValue" :min="0.1" :max="50" :precision="1" style="width:100%">
                <template #suffix>mmol/L</template>
              </el-input-number>
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item label="备注">
              <el-input v-model="form.note" placeholder="可选备注" />
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item>
              <el-button type="primary" native-type="submit" :loading="submitting">提交记录</el-button>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <!-- 血糖趋势图 -->
    <el-row :gutter="20" class="chart-row" v-if="trendData.length > 0">
      <el-col :span="24">
        <el-card class="chart-card" shadow="hover">
          <template #header><span>血糖趋势 (近14天)</span></template>
          <div class="chart-container" ref="trendChartRef"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 记录列表 -->
    <el-card class="list-card" shadow="hover">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>血糖记录</span>
          <el-pagination
            v-model:current-page="page"
            small layout="prev, pager, next"
            :total="total" :page-size="10"
            @current-change="loadRecords"
          />
        </div>
      </template>
      <el-table :data="records" stripe v-loading="recordsLoading">
        <el-table-column label="日期" width="110">
          <template #default="{ row }">{{ row.recordDate }}</template>
        </el-table-column>
        <el-table-column label="时间" width="80">
          <template #default="{ row }">{{ row.recordTime?.substring(0,5) || '-' }}</template>
        </el-table-column>
        <el-table-column label="类型" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.measureType === 'fasting'" size="small">空腹</el-tag>
            <el-tag v-else-if="row.measureType === 'before_meal'" type="info" size="small">餐前</el-tag>
            <el-tag v-else-if="row.measureType === 'after_meal'" type="success" size="small">餐后</el-tag>
            <el-tag v-else-if="row.measureType === 'bedtime'" type="warning" size="small">睡前</el-tag>
            <el-tag v-else type="info" size="small">随机</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="血糖值" width="120">
          <template #default="{ row }">
            <span :style="{ color: row.abnormalFlag === 1 ? '#ff4d4f' : row.abnormalFlag === 2 ? '#fa8c16' : '#52c41a', fontWeight: 600 }">
              {{ row.glucoseValue }} mmol/L
            </span>
            <el-tag v-if="row.abnormalFlag === 1" type="danger" size="small" style="margin-left:6px">偏高</el-tag>
            <el-tag v-if="row.abnormalFlag === 2" type="warning" size="small" style="margin-left:6px">偏低</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="note" label="备注" min-width="120" />
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button type="danger" size="small" text @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { submitBloodSugar, getBloodSugarRecords, getBloodSugarTrend, deleteBloodSugar } from '@/api/bloodSugar'
import echarts from '@/utils/echarts'

const form = ref({
  recordDate: new Date().toISOString().substring(0, 10),
  recordTime: new Date().toTimeString().substring(0, 5),
  measureType: 'fasting',
  glucoseValue: 5.5,
  note: ''
})

const records = ref([])
const recordsLoading = ref(false)
const submitting = ref(false)
const page = ref(1)
const total = ref(0)
const trendData = ref([])
const trendChartRef = ref(null)
let trendChart = null

async function handleSubmit() {
  if (!form.value.recordDate || !form.value.measureType || !form.value.glucoseValue) {
    ElMessage.warning('请填写必填字段')
    return
  }
  submitting.value = true
  try {
    const payload = {
      recordDate: form.value.recordDate,
      recordTime: form.value.recordTime,
      measureType: form.value.measureType,
      glucoseValue: form.value.glucoseValue,
      note: form.value.note
    }
    const res = await submitBloodSugar(payload)
    if (res.code === 200) {
      ElMessage.success('血糖记录成功')
      form.value.note = ''
      form.value.glucoseValue = 5.5
      loadRecords()
      loadTrend()
    }
  } finally {
    submitting.value = false
  }
}

async function loadRecords() {
  recordsLoading.value = true
  try {
    const res = await getBloodSugarRecords({ page: page.value, size: 10 })
    if (res.data) {
      records.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } finally {
    recordsLoading.value = false
  }
}

async function loadTrend() {
  try {
    const res = await getBloodSugarTrend({ days: 14 })
    if (res.data) {
      trendData.value = res.data
      await nextTick()
      initTrendChart()
    }
  } catch { /* ignore */ }
}

function initTrendChart() {
  if (!trendChartRef.value || trendData.value.length === 0) return
  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value)
  }
  const dates = trendData.value.map(r => r.recordDate)
  const values = trendData.value.map(r => Number(r.glucoseValue))

  // 标记异常点
  const markPoints = trendData.value
    .map((r, i) => r.abnormalFlag > 0 ? { coord: [r.recordDate, Number(r.glucoseValue)], value: r.abnormalFlag === 1 ? '高' : '低' } : null)
    .filter(Boolean)

  trendChart.setOption({
    tooltip: { trigger: 'axis', formatter: (params) => {
      const p = params[0]
      return `${p.axisValue}<br/>血糖: ${p.value} mmol/L`
    }},
    grid: { left: 50, right: 20, top: 20, bottom: 30 },
    xAxis: { type: 'category', data: dates, axisLabel: { color: '#8b949e' } },
    yAxis: {
      type: 'value', name: 'mmol/L',
      axisLabel: { color: '#8b949e' },
      min: (min) => Math.max(0, min.value - 1),
      max: (max) => max.value + 2,
      // 添加参考区域：正常范围 3.9-11.1
      markArea: {
        silent: true,
        data: [[
          { yAxis: 3.9, itemStyle: { color: 'rgba(82,196,26,0.05)' } },
          { yAxis: 11.1 }
        ]]
      }
    },
    series: [{
      type: 'line', data: values, smooth: true,
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
      }
    }]
  })
}

async function handleDelete(id) {
  try {
    await ElMessageBox.confirm('确定要删除这条血糖记录吗？', '提示', { type: 'warning' })
    await deleteBloodSugar(id)
    ElMessage.success('删除成功')
    loadRecords()
    loadTrend()
  } catch { /* cancelled */ }
}

function handleResize() {
  trendChart?.resize()
}

onMounted(() => {
  loadRecords()
  loadTrend()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
})
</script>

<style scoped>
.blood-sugar-page { padding: 20px 0; }
.page-header { margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 20px; font-weight: 600; color: #e6edf3; }
.submit-card { margin-bottom: 20px; }
.chart-row { margin-bottom: 20px; }
.chart-container { height: 300px; }
.list-card { margin-bottom: 20px; }

:deep(.el-card) { background: #161b22; border-color: #30363d; }
:deep(.el-card__header) { border-color: #21262d; color: #e6edf3; }
</style>