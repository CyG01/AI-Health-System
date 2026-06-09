<template>
  <div class="checkin-page" v-loading="pageLoading">
    <div class="stats-bar glass-card">
      <div class="stat-item">
        <span class="stat-value text-green">{{ stats.consecutiveDays }}</span>
        <span class="stat-label">连续打卡</span>
      </div>
      <div class="stat-divider" />
      <div class="stat-item">
        <span class="stat-value">{{ stats.totalDays }}</span>
        <span class="stat-label">累计天数</span>
      </div>
      <div class="stat-divider" />
      <div class="stat-item">
        <span class="stat-value">{{ stats.currentWeekDays }}</span>
        <span class="stat-label">本周打卡</span>
      </div>
      <div class="stat-divider" />
      <div class="stat-item">
        <span class="stat-value">{{ stats.currentMonthDays }}</span>
        <span class="stat-label">本月打卡</span>
      </div>
      <div class="stat-divider" />
      <div class="stat-item">
        <span class="stat-value text-green">{{ stats.exerciseCompleteRate }}%</span>
        <span class="stat-label">运动完成率</span>
      </div>
      <div class="stat-divider" />
      <div class="stat-item">
        <span class="stat-value text-blue">{{ stats.dietCompleteRate }}%</span>
        <span class="stat-label">饮食完成率</span>
      </div>
    </div>

    <!-- 成就徽章 -->
    <div class="badges-section glass-card" v-if="badges.length > 0">
      <div class="section-title">打卡成就</div>
      <div class="badges-row">
        <div v-for="badge in badges" :key="badge.name" class="badge-item" :class="{ unlocked: badge.unlocked }">
          <span class="badge-icon">{{ badge.icon }}</span>
          <span class="badge-name">{{ badge.name }}</span>
          <span class="badge-desc">{{ badge.desc }}</span>
        </div>
      </div>
    </div>

    <!-- 打卡热力图 -->
    <div class="heatmap-card glass-card">
      <div class="section-title">打卡热力图（近一年）</div>
      <div class="heatmap-container" ref="heatmapRef"></div>
    </div>

    <div class="main-row">
      <div class="calendar-card glass-card">
        <el-calendar v-model="calendarDate" ref="calendarRef">
          <template #date-cell="{ data }">
            <div
              class="date-cell"
              :class="getDateCellClass(data.date)"
              @click="handleDateClick(data.date)"
            >
              <span class="day-number">{{ data.day.split('-')[2] }}</span>
              <span class="dot-row">
                <span
                  class="dot"
                  :class="getExerciseDot(data.date)"
                  v-if="getExerciseDot(data.date)"
                />
                <span
                  class="dot"
                  :class="getDietDot(data.date)"
                  v-if="getDietDot(data.date)"
                />
              </span>
            </div>
          </template>
        </el-calendar>
      </div>

      <div class="today-card glass-card">
        <div class="today-header">
          <span class="today-label">{{ isTodayCheckedIn ? '今日打卡记录' : '今日打卡' }}</span>
          <el-tag v-if="todayRecord" :type="todayTagType" size="small" effect="dark">
            {{ todayTagText }}
          </el-tag>
        </div>

        <el-form
          v-if="!isTodayCheckedIn"
          ref="submitFormRef"
          :model="submitForm"
          :rules="submitRules"
          label-position="top"
          size="default"
          @submit.prevent="handleSubmit"
        >
          <el-form-item label="运动完成" prop="exerciseStatus">
            <el-radio-group v-model="submitForm.exerciseStatus">
              <el-radio-button :value="0">未完成</el-radio-button>
              <el-radio-button :value="1">部分完成</el-radio-button>
              <el-radio-button :value="2">全部完成</el-radio-button>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="饮食完成" prop="dietStatus">
            <el-radio-group v-model="submitForm.dietStatus">
              <el-radio-button :value="0">未完成</el-radio-button>
              <el-radio-button :value="1">部分完成</el-radio-button>
              <el-radio-button :value="2">全部完成</el-radio-button>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="当前体重 (kg)">
            <el-input-number
              v-model="submitForm.currentWeight"
              :min="30"
              :max="300"
              :precision="1"
              :controls="false"
              placeholder="选填"
            />
          </el-form-item>

          <el-form-item label="心情">
            <el-select v-model="submitForm.mood" placeholder="选填" clearable>
              <el-option label="😄 开心" value="开心" />
              <el-option label="😊 不错" value="不错" />
              <el-option label="😐 一般" value="一般" />
              <el-option label="😞 低落" value="低落" />
              <el-option label="😤 烦躁" value="烦躁" />
            </el-select>
          </el-form-item>

          <el-form-item label="备注">
            <el-input
              v-model="submitForm.note"
              type="textarea"
              :rows="2"
              maxlength="200"
              show-word-limit
              placeholder="记录今天的感受..."
            />
          </el-form-item>

          <el-button type="primary" size="large" native-type="submit" :loading="submitting" class="submit-btn">
            提交打卡
          </el-button>
        </el-form>

        <div v-else class="today-record">
          <div class="record-row">
            <span class="record-key">运动</span>
            <el-tag :type="todayRecord.exerciseStatus === 2 ? 'success' : todayRecord.exerciseStatus === 1 ? 'warning' : 'info'" size="small" effect="dark">
              {{ statusLabel(todayRecord.exerciseStatus) }}
            </el-tag>
          </div>
          <div class="record-row">
            <span class="record-key">饮食</span>
            <el-tag :type="todayRecord.dietStatus === 2 ? 'success' : todayRecord.dietStatus === 1 ? 'warning' : 'info'" size="small" effect="dark">
              {{ statusLabel(todayRecord.dietStatus) }}
            </el-tag>
          </div>
          <div v-if="todayRecord.currentWeight" class="record-row">
            <span class="record-key">体重</span>
            <span class="record-val">{{ todayRecord.currentWeight }} kg</span>
          </div>
          <div v-if="todayRecord.mood" class="record-row">
            <span class="record-key">心情</span>
            <span class="record-val">{{ todayRecord.mood }}</span>
          </div>
          <div v-if="todayRecord.note" class="record-row">
            <span class="record-key">备注</span>
            <span class="record-val">{{ todayRecord.note }}</span>
          </div>
        </div>
      </div>
    </div>

    <el-dialog
      v-model="supplementVisible"
      title="补卡"
      width="440px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div class="supplement-date">
        <span class="supplement-label">补卡日期</span>
        <span class="supplement-val">{{ supplementDate }}</span>
      </div>
      <el-form
        ref="supplementFormRef"
        :model="supplementForm"
        :rules="supplementRules"
        label-position="top"
        size="default"
      >
        <el-form-item label="运动完成" prop="exerciseStatus">
          <el-radio-group v-model="supplementForm.exerciseStatus">
            <el-radio-button :value="0">未完成</el-radio-button>
            <el-radio-button :value="1">部分完成</el-radio-button>
            <el-radio-button :value="2">全部完成</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="饮食完成" prop="dietStatus">
          <el-radio-group v-model="supplementForm.dietStatus">
            <el-radio-button :value="0">未完成</el-radio-button>
            <el-radio-button :value="1">部分完成</el-radio-button>
            <el-radio-button :value="2">全部完成</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="当前体重 (kg)">
          <el-input-number
            v-model="supplementForm.currentWeight"
            :min="30"
            :max="300"
            :precision="1"
            :controls="false"
            placeholder="选填"
          />
        </el-form-item>

        <el-form-item label="心情">
          <el-select v-model="supplementForm.mood" placeholder="选填" clearable>
            <el-option label="😄 开心" value="开心" />
            <el-option label="😊 不错" value="不错" />
            <el-option label="😐 一般" value="一般" />
            <el-option label="😞 低落" value="低落" />
            <el-option label="😤 烦躁" value="烦躁" />
          </el-select>
        </el-form-item>

        <el-form-item label="备注">
          <el-input
            v-model="supplementForm.note"
            type="textarea"
            :rows="2"
            maxlength="200"
            show-word-limit
            placeholder="记录当天的感受..."
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="supplementVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSupplementSubmit">确认补卡</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import echarts from '@/utils/echarts'
import { submitCheckin, supplementCheckin, getCheckinList, getCheckinStats } from '@/api/checkin'

const PAGE_LOADING_KEY = 'checkin-page'
const today = new Date().toISOString().split('T')[0]
const calendarDate = ref(new Date())
const calendarRef = ref(null)
const pageLoading = ref(false)
const submitting = ref(false)

const submitFormRef = ref(null)
const supplementFormRef = ref(null)

const stats = reactive({
  consecutiveDays: 0,
  totalDays: 0,
  currentWeekDays: 0,
  currentMonthDays: 0,
  exerciseCompleteRate: 0,
  dietCompleteRate: 0
})

const records = ref({})
const todayRecord = ref(null)
const supplementVisible = ref(false)
const supplementDate = ref('')
const heatmapRef = ref(null)

// 成就徽章
const badges = computed(() => {
  const list = []
  const total = stats.totalDays
  const streak = stats.consecutiveDays
  const conditions = [
    { days: 3, icon: '🌱', name: '新手上路', desc: '累计打卡3天' },
    { days: 7, icon: '🔥', name: '一周打卡', desc: '累计打卡7天' },
    { days: 14, icon: '💪', name: '坚持两周', desc: '累计打卡14天' },
    { days: 30, icon: '⭐', name: '月度之星', desc: '累计打卡30天' },
    { days: 60, icon: '🏆', name: '健身达人', desc: '累计打卡60天' },
    { days: 100, icon: '👑', name: '百炼成钢', desc: '累计打卡100天' },
    { days: 180, icon: '💎', name: '半年坚持', desc: '累计打卡180天' },
    { days: 365, icon: '🌟', name: '年度传奇', desc: '累计打卡365天' }
  ]
  const streakConditions = [
    { days: 3, icon: '🔥', name: '连击3天', desc: '连续3天不中断' },
    { days: 7, icon: '🔥🔥', name: '一周连击', desc: '连续打卡7天' },
    { days: 14, icon: '🚀', name: '双周王者', desc: '连续打卡14天' },
    { days: 30, icon: '⚡', name: '月度全勤', desc: '连续打卡30天' },
    { days: 60, icon: '💥', name: '铁人模式', desc: '连续打卡60天' }
  ]
  conditions.forEach(c => {
    list.push({ ...c, unlocked: total >= c.days })
  })
  streakConditions.forEach(c => {
    list.push({ ...c, unlocked: streak >= c.days })
  })
  return list
})

const submitForm = reactive({
  planId: null,
  exerciseStatus: 0,
  dietStatus: 0,
  currentWeight: null,
  mood: '',
  note: ''
})

const supplementForm = reactive({
  planId: null,
  checkDate: '',
  exerciseStatus: 0,
  dietStatus: 0,
  currentWeight: null,
  mood: '',
  note: ''
})

const submitRules = {
  exerciseStatus: [{ required: true, message: '请选择运动完成状态', trigger: 'change' }],
  dietStatus: [{ required: true, message: '请选择饮食完成状态', trigger: 'change' }]
}

const supplementRules = {
  exerciseStatus: [{ required: true, message: '请选择运动完成状态', trigger: 'change' }],
  dietStatus: [{ required: true, message: '请选择饮食完成状态', trigger: 'change' }]
}

const isTodayCheckedIn = computed(() => todayRecord.value !== null)
const todayTagType = computed(() => {
  if (!todayRecord.value) return 'info'
  const e = todayRecord.value.exerciseStatus
  const d = todayRecord.value.dietStatus
  if (e === 2 && d === 2) return 'success'
  if (e >= 1 || d >= 1) return 'warning'
  return 'info'
})
const todayTagText = computed(() => {
  if (!todayRecord.value) return ''
  const e = todayRecord.value.exerciseStatus
  const d = todayRecord.value.dietStatus
  if (e === 2 && d === 2) return '全部完成'
  if (e >= 1 || d >= 1) return '部分完成'
  return '未完成'
})

async function loadData() {
  pageLoading.value = true
  try {
    const [listRes, statsRes] = await Promise.all([
      getCheckinList(),
      getCheckinStats()
    ])

    const list = listRes.data || []
    const statsData = statsRes.data || {}

    records.value = {}
    list.forEach((item) => {
      records.value[item.checkDate] = item
    })

    Object.assign(stats, {
      consecutiveDays: statsData.consecutiveDays ?? 0,
      totalDays: statsData.totalDays ?? 0,
      currentWeekDays: statsData.currentWeekDays ?? 0,
      currentMonthDays: statsData.currentMonthDays ?? 0,
      exerciseCompleteRate: statsData.exerciseCompleteRate ?? 0,
      dietCompleteRate: statsData.dietCompleteRate ?? 0
    })

    todayRecord.value = records.value[today] || null
    nextTick(() => renderHeatmap())
  } finally {
    pageLoading.value = false
  }
}

function statusLabel(val) {
  if (val === 2) return '全部完成'
  if (val === 1) return '部分完成'
  return '未完成'
}

function getDateCellClass(dateStr) {
  const dateVal = toDateStr(dateStr)
  const record = records.value[dateVal]

  if (record) {
    if (record.exerciseStatus === 2 && record.dietStatus === 2) return 'cell-full'
    if (record.exerciseStatus >= 1 || record.dietStatus >= 1) return 'cell-partial'
    return 'cell-incomplete'
  }

  if (dateVal === today) return 'cell-today'
  if (dateVal < today) return 'cell-missed'
  return 'cell-future'
}

function getExerciseDot(dateStr) {
  const dateVal = toDateStr(dateStr)
  const record = records.value[dateVal]
  if (!record) return ''
  if (record.exerciseStatus === 2) return 'dot-full'
  if (record.exerciseStatus === 1) return 'dot-partial'
  return 'dot-incomplete'
}

function getDietDot(dateStr) {
  const dateVal = toDateStr(dateStr)
  const record = records.value[dateVal]
  if (!record) return ''
  if (record.dietStatus === 2) return 'dot-full'
  if (record.dietStatus === 1) return 'dot-partial'
  return 'dot-incomplete'
}

function toDateStr(date) {
  if (typeof date === 'string') return date
  const d = new Date(date)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

const DAY_MS = 86400000

function handleDateClick(date) {
  const dateVal = toDateStr(date)
  if (dateVal > today) return
  if (records.value[dateVal]) return

  if (dateVal === today) return

  const daysBetween = Math.floor((new Date(today).getTime() - new Date(dateVal).getTime()) / DAY_MS)
  if (daysBetween < 0 || daysBetween > 7) {
    ElMessage.warning('仅支持补卡过去7天内的日期')
    return
  }

  supplementDate.value = dateVal
  supplementForm.planId = submitForm.planId
  supplementForm.checkDate = dateVal
  supplementForm.exerciseStatus = 0
  supplementForm.dietStatus = 0
  supplementForm.currentWeight = null
  supplementForm.mood = ''
  supplementForm.note = ''
  supplementVisible.value = true
}

async function handleSubmit() {
  const valid = await submitFormRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const payload = {}
    if (submitForm.planId) payload.planId = submitForm.planId
    payload.exerciseStatus = submitForm.exerciseStatus
    payload.dietStatus = submitForm.dietStatus
    if (submitForm.currentWeight != null) payload.currentWeight = submitForm.currentWeight
    if (submitForm.mood) payload.mood = submitForm.mood
    if (submitForm.note) payload.note = submitForm.note

    await submitCheckin(payload)
    ElMessage.success('打卡成功')
    submitForm.exerciseStatus = 0
    submitForm.dietStatus = 0
    submitForm.currentWeight = null
    submitForm.mood = ''
    submitForm.note = ''
    await loadData()
  } catch {
    // handled by interceptor
  } finally {
    submitting.value = false
  }
}

async function handleSupplementSubmit() {
  const valid = await supplementFormRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const payload = {
      planId: supplementForm.planId,
      checkDate: supplementForm.checkDate,
      exerciseStatus: supplementForm.exerciseStatus,
      dietStatus: supplementForm.dietStatus
    }
    if (supplementForm.currentWeight != null) payload.currentWeight = supplementForm.currentWeight
    if (supplementForm.mood) payload.mood = supplementForm.mood
    if (supplementForm.note) payload.note = supplementForm.note

    await supplementCheckin(payload)
    ElMessage.success('补卡成功')
    supplementVisible.value = false
    await loadData()
  } catch {
    // handled by interceptor
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadData()
})

// 打卡热力图渲染
function getDaysInYear() {
  const days = []
  const now = new Date()
  const startDate = new Date(now.getFullYear(), now.getMonth() - 11, 1)
  for (let d = new Date(startDate); d <= now; d.setDate(d.getDate() + 1)) {
    days.push(toDateStr(d))
  }
  return days
}

function getHeatmapLevel(dateStr) {
  const r = records.value[dateStr]
  if (!r) return 0
  if (r.exerciseStatus === 2 && r.dietStatus === 2) return 3
  if (r.exerciseStatus >= 1 || r.dietStatus >= 1) return 2
  return 1
}

function renderHeatmap() {
  if (!heatmapRef.value) return
  const chart = echarts.init(heatmapRef.value)
  const days = getDaysInYear()
  const data = days.map(d => [d, getHeatmapLevel(d)])
  chart.setOption({
    tooltip: { backgroundColor: 'rgba(22,27,34,0.95)', borderColor: '#30363d', textStyle: { color: '#c9d1d9' }, formatter: (p) => `${p.data[0]}: ${['未打卡','部分','不错','完美'][p.data[1]]}` },
    visualMap: { min: 0, max: 3, orient: 'horizontal', left: 'center', bottom: 0, calculable: false, inRange: { color: ['#161b22', '#0e4429', '#006d32', '#26a641'] }, show: false },
    calendar: { top: 20, left: 40, right: 20, range: [days[0], days[days.length - 1]], cellSize: [14, 14], splitLine: { lineStyle: { color: '#0d1117' } }, itemStyle: { borderColor: '#0d1117', borderWidth: 2, borderRadius: 2 }, dayLabel: { color: '#8b949e' }, monthLabel: { color: '#8b949e' }, yearLabel: { show: false } },
    series: [{ type: 'heatmap', coordinateSystem: 'calendar', data: data, emphasis: { itemStyle: { shadowBlur: 8, shadowColor: 'rgba(0,0,0,0.3)' } } }]
  })
  chart.resize()
}
</script>

<style scoped lang="scss">
.checkin-page {
  padding: 4px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.stats-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 24px;
  padding: 16px 32px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
}

.stat-value.text-green {
  color: #3fb950;
}

.stat-value.text-blue {
  color: #58a6ff;
}

.stat-label {
  font-size: 12px;
  color: var(--text-secondary);
}

.stat-divider {
  width: 1px;
  height: 36px;
  background: #30363d;
}

.main-row {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.calendar-card {
  flex: 1;
  min-width: 0;
  padding: 8px;

  :deep(.el-calendar) {
    --el-calendar-border: #30363d;
    --el-calendar-header-border-bottom: #30363d;
    --el-calendar-cell-width: 14.28%;
    background: transparent;
  }

  :deep(.el-calendar__header) {
    padding: 8px 16px;
  }

  :deep(.el-calendar__title) {
    color: var(--text-primary);
    font-size: 15px;
  }

  :deep(.el-calendar__body) {
    padding: 4px 8px 8px;
  }

  :deep(.el-calendar-table thead th) {
    color: var(--text-secondary);
    font-size: 12px;
    padding: 6px 0;
    border-bottom-color: #30363d;
  }

  :deep(.el-calendar-table td) {
    border-top-color: #30363d;
    border-right-color: #30363d;
  }
}

.date-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-height: 56px;
  padding: 6px 2px 4px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s ease;
  position: relative;

  &.cell-full {
    background: rgba(63, 185, 80, 0.08);
    border: 1px solid rgba(63, 185, 80, 0.35);
    box-shadow: 0 0 8px rgba(63, 185, 80, 0.12);
  }

  &.cell-partial {
    background: rgba(210, 153, 34, 0.08);
    border: 1px solid rgba(210, 153, 34, 0.35);
    box-shadow: 0 0 8px rgba(210, 153, 34, 0.10);
  }

  &.cell-incomplete {
    background: rgba(139, 148, 158, 0.04);
    border: 1px solid transparent;
  }

  &.cell-missed {
    background: rgba(248, 81, 73, 0.04);
    border: 1px solid transparent;

    &:hover {
      border-color: rgba(248, 81, 73, 0.3);
      background: rgba(248, 81, 73, 0.08);
    }
  }

  &.cell-today {
    background: rgba(88, 166, 255, 0.06);
    border: 1px solid rgba(88, 166, 255, 0.3);
  }

  &.cell-future {
    cursor: default;
    opacity: 0.45;
  }
}

.day-number {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.dot-row {
  display: flex;
  gap: 3px;
  margin-top: 4px;
}

.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;

  &.dot-full {
    background: #3fb950;
    box-shadow: 0 0 4px rgba(63, 185, 80, 0.6);
  }

  &.dot-partial {
    background: #d29922;
    box-shadow: 0 0 4px rgba(210, 153, 34, 0.5);
  }

  &.dot-incomplete {
    background: #8b949e;
  }
}

.today-card {
  width: 340px;
  flex-shrink: 0;
  padding: 24px;
}

.today-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.today-label {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.submit-btn {
  width: 100%;
  margin-top: 4px;
}

.today-record {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.record-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.record-key {
  font-size: 13px;
  color: var(--text-secondary);
}

.record-val {
  font-size: 14px;
  color: var(--text-primary);
}

.supplement-date {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  background: rgba(88, 166, 255, 0.06);
  border-radius: 8px;
  margin-bottom: 16px;
}

.supplement-label {
  font-size: 13px;
  color: var(--text-secondary);
}

.supplement-val {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

/* 成就徽章 */
.badges-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 16px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.badges-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.badge-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 10px 14px;
  border-radius: 8px;
  background: #161b22;
  border: 1px solid #30363d;
  min-width: 72px;
  opacity: 0.35;
  transition: all 0.3s;
}

.badge-item.unlocked {
  opacity: 1;
  border-color: #3fb950;
  background: rgba(63, 185, 80, 0.08);
}

.badge-icon { font-size: 22px; }
.badge-name { font-size: 11px; font-weight: 600; color: var(--text-primary); white-space: nowrap; }
.badge-desc { font-size: 10px; color: var(--text-secondary); }

/* 热力图 */
.heatmap-card {
  margin-bottom: 16px;
  padding: 20px 24px;
}

.heatmap-container {
  width: 100%;
  height: 170px;
  margin-top: 12px;
}
</style>
