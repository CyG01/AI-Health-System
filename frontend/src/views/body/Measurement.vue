<template>
  <div class="body-page">
    <NSpin :show="pageLoading">
      <div class="page-header">
        <h2>{{ $t('body.measurement') || '身体围度测量' }}</h2>
        <p class="page-desc">{{ $t('body.measurementDesc') || '定期记录围度数据，比单纯体重更能反映体型变化' }}</p>
      </div>

      <!-- 统计卡片 -->
      <div v-if="latest" class="grid grid-cols-4 xl:grid-cols-8 gap-3 mb-5">
        <NCard v-for="item in statCards" :key="item.label" size="small" class="stat-card text-center">
          <div class="stat-label">{{ item.label }}</div>
          <div class="stat-value" :class="item.classFn ? item.classFn(latest) : ''">
            {{ item.getValue(latest) }}
            <span v-if="item.unit" class="stat-unit">{{ item.unit }}</span>
          </div>
          <div v-if="item.tag" class="stat-whr-tag">
            <NTag :type="whrTagType" size="small">{{ whrLabel }}</NTag>
          </div>
        </NCard>
      </div>

      <!-- 提交表单 -->
      <NCard class="mb-5">
        <h3 class="card-title">{{ latest ? ($t('body.update') || '更新围度数据') : ($t('body.firstRecord') || '首次记录围度') }}</h3>
        <NForm :model="form" label-placement="left" label-width="80px" @submit.prevent="handleSubmit">
          <div class="grid grid-cols-2 md:grid-cols-4 xl:grid-cols-8 gap-4">
            <NFormItem label="日期">
              <NDatePicker v-model:formatted-value="form.recordDate" type="date" value-format="yyyy-MM-dd" class="w-full" />
            </NFormItem>
            <NFormItem label="腰围(cm)">
              <NInputNumber v-model:value="form.waist" :min="30" :max="200" :precision="1" :step="0.5" class="w-full" />
            </NFormItem>
            <NFormItem label="臀围(cm)">
              <NInputNumber v-model:value="form.hip" :min="30" :max="200" :precision="1" :step="0.5" class="w-full" />
            </NFormItem>
            <NFormItem label="胸围(cm)">
              <NInputNumber v-model:value="form.chest" :min="30" :max="200" :precision="1" :step="0.5" class="w-full" />
            </NFormItem>
            <NFormItem label="大腿围(cm)">
              <NInputNumber v-model:value="form.thigh" :min="20" :max="120" :precision="1" :step="0.5" class="w-full" />
            </NFormItem>
            <NFormItem label="臂围(cm)">
              <NInputNumber v-model:value="form.arm" :min="15" :max="80" :precision="1" :step="0.5" class="w-full" />
            </NFormItem>
            <NFormItem label="体脂率(%)">
              <NInputNumber v-model:value="form.bodyFatRate" :min="1" :max="60" :precision="1" :step="0.5" class="w-full" />
            </NFormItem>
            <NFormItem label=" ">
              <NButton type="primary" attr-type="submit" :loading="submitting">保存</NButton>
            </NFormItem>
          </div>
          <NFormItem label="备注">
            <NInput v-model:value="form.note" placeholder="测量备注..." :maxlength="200" show-count />
          </NFormItem>
        </NForm>
      </NCard>

      <!-- 趋势图 -->
      <div v-if="trend.length > 1" class="grid grid-cols-1 lg:grid-cols-2 gap-5 mb-5">
        <NCard :title="$t('body.measurementTrend') || '围度趋势'" size="small">
          <div ref="waistChartRef" class="h-70" />
        </NCard>
        <NCard :title="$t('body.whrTrend') || '腰臀比趋势'" size="small">
          <div ref="whrChartRef" class="h-70" />
        </NCard>
      </div>

      <!-- 历史记录 -->
      <NCard :title="$t('body.history') || '历史记录'">
        <NDataTable :columns="historyColumns" :data="history" :loading="pageLoading" :bordered="false" :row-key="(row: BodyRecord) => row.id" />
      </NCard>
    </NSpin>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, h, onMounted, onUnmounted, nextTick } from 'vue'
import {
  NCard, NButton, NForm, NFormItem, NInput, NInputNumber, NDatePicker,
  NDataTable, NTag, NSpin, NEmpty,
  useMessage, useDialog,
  type DataTableColumns
} from 'naive-ui'
import { fetchSubmitBodyMeasurement, fetchGetLatestBodyMeasurement, fetchGetBodyMeasurementHistory, fetchGetBodyMeasurementTrend, fetchDeleteBodyMeasurement } from '@/service/api'
import * as echarts from 'echarts/core'
import { LineChart } from 'echarts/charts'
import {
  GridComponent, TooltipComponent, LegendComponent, MarkLineComponent
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([LineChart, GridComponent, TooltipComponent, LegendComponent, MarkLineComponent, CanvasRenderer])

defineOptions({ name: 'BodyMeasurement' })

interface BodyRecord {
  id: number | string
  recordDate: string
  waist: number | null
  hip: number | null
  chest: number | null
  thigh: number | null
  arm: number | null
  bodyFatRate: number | null
  waistHipRatio: number | null
  note: string
}

const message = useMessage()
const dialog = useDialog()

const pageLoading = ref(false)
const submitting = ref(false)
const latest = ref<BodyRecord | null>(null)
const history = ref<BodyRecord[]>([])
const trend = ref<BodyRecord[]>([])
const waistChartRef = ref<HTMLElement | null>(null)
const whrChartRef = ref<HTMLElement | null>(null)
let waistChart: echarts.ECharts | null = null
let whrChart: echarts.ECharts | null = null

const form = reactive({
  recordDate: new Date().toISOString().slice(0, 10),
  waist: null as number | null,
  hip: null as number | null,
  chest: null as number | null,
  thigh: null as number | null,
  arm: null as number | null,
  bodyFatRate: null as number | null,
  note: ''
})

const whrLabel = computed(() => {
  const ratio = latest.value?.waistHipRatio
  if (!ratio) return '--'
  if (ratio < 0.85) return '健康'
  if (ratio < 0.9) return '亚健康'
  return '偏高'
})

const whrTagType = computed<'success' | 'warning' | 'error' | 'info'>(() => {
  const ratio = latest.value?.waistHipRatio
  if (!ratio) return 'info'
  if (ratio < 0.85) return 'success'
  if (ratio < 0.9) return 'warning'
  return 'error'
})

const whrClass = computed(() => {
  const ratio = latest.value?.waistHipRatio
  if (!ratio) return ''
  if (ratio < 0.85) return 'text-success'
  if (ratio < 0.9) return 'text-warning'
  return 'text-danger'
})

interface StatCard {
  label: string
  getValue: (r: BodyRecord) => string | number | null
  unit?: string
  classFn?: (r: BodyRecord) => string
  tag?: boolean
}

const statCards = computed<StatCard[]>(() => [
  { label: '腰围', getValue: (r) => r.waist ?? '--', unit: 'cm' },
  { label: '臀围', getValue: (r) => r.hip ?? '--', unit: 'cm' },
  { label: '腰臀比', getValue: (r) => r.waistHipRatio ?? '--', classFn: () => whrClass.value, tag: true },
  { label: '胸围', getValue: (r) => r.chest ?? '--', unit: 'cm' },
  { label: '大腿围', getValue: (r) => r.thigh ?? '--', unit: 'cm' },
  { label: '臂围', getValue: (r) => r.arm ?? '--', unit: 'cm' },
  { label: '体脂率', getValue: (r) => r.bodyFatRate ?? '--', unit: '%' },
  { label: '测量日期', getValue: (r) => r.recordDate }
])

const historyColumns = computed<DataTableColumns<BodyRecord>>(() => [
  { title: '日期', key: 'recordDate', width: 120 },
  {
    title: '腰围', key: 'waist', width: 90,
    render: (row) => h('span', {}, row.waist != null ? `${row.waist}cm` : '--')
  },
  {
    title: '臀围', key: 'hip', width: 90,
    render: (row) => h('span', {}, row.hip != null ? `${row.hip}cm` : '--')
  },
  {
    title: '腰臀比', key: 'waistHipRatio', width: 100,
    render: (row) => h('span', {}, row.waistHipRatio ?? '--')
  },
  {
    title: '胸围', key: 'chest', width: 90,
    render: (row) => h('span', {}, row.chest != null ? `${row.chest}cm` : '--')
  },
  {
    title: '大腿围', key: 'thigh', width: 90,
    render: (row) => h('span', {}, row.thigh != null ? `${row.thigh}cm` : '--')
  },
  {
    title: '臂围', key: 'arm', width: 90,
    render: (row) => h('span', {}, row.arm != null ? `${row.arm}cm` : '--')
  },
  {
    title: '体脂率', key: 'bodyFatRate', width: 90,
    render: (row) => h('span', {}, row.bodyFatRate != null ? `${row.bodyFatRate}%` : '--')
  },
  { title: '备注', key: 'note', ellipsis: { tooltip: true } },
  {
    title: '操作', key: 'actions', width: 80, fixed: 'right',
    render: (row) => h(NButton, {
      type: 'error', size: 'small', text: true,
      onClick: () => handleDeleteBody(row.id)
    }, { default: () => '删除' })
  }
])

async function loadLatest() {
  try {
    const { data } = await fetchGetLatestBodyMeasurement()
    latest.value = data as any
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
    const { data } = await fetchGetBodyMeasurementHistory(20)
    history.value = (data as any) || []
  } finally {
    pageLoading.value = false
  }
}

async function loadTrend() {
  try {
    const { data } = await fetchGetBodyMeasurementTrend(6)
    trend.value = (data as any) || []
  } catch { /* ignore */ }
}

async function handleSubmit() {
  if (!form.waist || !form.hip || !form.chest) {
    message.warning('腰围、臀围和胸围为必填项')
    return
  }
  submitting.value = true
  try {
    await fetchSubmitBodyMeasurement({ ...form } as any)
    message.success('围度数据已保存')
    await loadLatest()
    await loadHistory()
    await loadTrend()
    await nextTick()
    initCharts()
  } finally {
    submitting.value = false
  }
}

async function handleDeleteBody(id: number | string) {
  dialog.warning({
    title: '删除确认',
    content: '确定删除此条围度记录吗？',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      await fetchDeleteBodyMeasurement(id as number)
      message.success('已删除')
      loadHistory()
      loadTrend()
    }
  })
}

function initCharts() {
  if (trend.value.length < 2) return

  if (!waistChart && waistChartRef.value) {
    waistChart = echarts.init(waistChartRef.value)
  }
  if (!whrChart && whrChartRef.value) {
    whrChart = echarts.init(whrChartRef.value)
  }

  const dates = trend.value.map((r) => r.recordDate)

  if (waistChart) {
    waistChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['腰围', '臀围', '胸围'], bottom: 0 },
      grid: { left: 50, right: 20, top: 20, bottom: 30 },
      xAxis: { type: 'category', data: dates },
      yAxis: { type: 'value', name: 'cm' },
      series: [
        { name: '腰围', type: 'line', data: trend.value.map((r) => r.waist), smooth: true, symbol: 'circle' },
        { name: '臀围', type: 'line', data: trend.value.map((r) => r.hip), smooth: true, symbol: 'circle' },
        { name: '胸围', type: 'line', data: trend.value.map((r) => r.chest), smooth: true, symbol: 'circle' }
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
          data: trend.value.map((r) => r.waistHipRatio),
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

.stat-card {
  .stat-label { font-size: 12px; color: var(--text-secondary); margin-bottom: 6px; }
  .stat-value { font-size: 24px; font-weight: 700; color: var(--text-primary); }
  .stat-unit { font-size: 13px; font-weight: 400; color: var(--text-secondary); margin-left: 2px; }
  .stat-whr-tag { margin-top: 4px; }
}

.text-success { color: #52c41a; }
.text-warning { color: #fa8c16; }
.text-danger { color: #ff4d4f; }

.card-title {
  margin: 0 0 16px;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}
</style>
