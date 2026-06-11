<template>
  <div class="sleep-page" v-loading="pageLoading">
    <div class="page-header">
      <h2>睡眠管理</h2>
    </div>

    <!-- 提交记录 -->
    <div class="submit-card glass-card">
      <h3 class="card-title">{{ todayRecord ? '更新今日睡眠' : '记录今日睡眠' }}</h3>
      <el-form :model="form" label-width="100px" @submit.prevent="handleSubmit">
        <el-row :gutter="20">
          <el-col :span="5">
            <el-form-item label="日期">
              <el-date-picker
                v-model="form.recordDate"
                type="date"
                placeholder="选择日期"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="5">
            <el-form-item label="入睡时间">
              <el-time-picker
                v-model="form.sleepTime"
                format="HH:mm"
                placeholder="入睡"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="5">
            <el-form-item label="起床时间">
              <el-time-picker
                v-model="form.wakeTime"
                format="HH:mm"
                placeholder="起床"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="5">
            <el-form-item label="睡眠质量">
              <el-rate v-model="form.quality" :max="5" :texts="qualityTexts" show-text />
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item label="">
              <el-button type="primary" native-type="submit" :loading="submitting">提交</el-button>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="form.dreamNotes" placeholder="梦境记录或其他备注..." maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
    </div>

    <!-- 统计概览 -->
    <el-row :gutter="20" class="stats-row" v-if="records.length > 0">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <el-statistic title="近7天平均时长" :value="avgDuration.toFixed(1)" suffix="小时" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <el-statistic title="近7天平均质量" :value="avgQuality.toFixed(1)" suffix="/5" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <el-statistic title="总记录天数" :value="records.length" suffix="天" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <el-statistic title="最好质量" :value="bestQuality" suffix="分" />
        </el-card>
      </el-col>
    </el-row>

    <!-- AI分析 -->
    <div class="analyze-card glass-card">
      <div class="card-header">
        <h3 class="card-title">AI睡眠分析</h3>
        <el-button type="primary" size="small" :loading="analyzing" @click="handleAnalyze">
          {{ aiAnalysis ? '重新分析' : '开始分析' }}
        </el-button>
      </div>
      <div v-if="aiAnalysis" class="ai-analysis-content">
        <el-icon :size="20" color="#58a6ff" style="margin-right: 8px"><MagicStick /></el-icon>
        <p v-html="formatAnalysis(aiAnalysis)"></p>
      </div>
      <el-empty v-else description="记录至少3天睡眠数据后可获得AI分析" :image-size="60" />
    </div>

    <!-- 历史记录 -->
    <div class="history-card glass-card">
      <h3 class="card-title">睡眠记录</h3>
      <el-table :data="records" stripe v-loading="pageLoading">
        <el-table-column prop="recordDate" label="日期" width="120" />
        <el-table-column label="入睡" width="100">
          <template #default="{ row }">{{ formatTime(row.sleepTime) }}</template>
        </el-table-column>
        <el-table-column label="起床" width="100">
          <template #default="{ row }">{{ formatTime(row.wakeTime) }}</template>
        </el-table-column>
        <el-table-column label="时长" width="100">
          <template #default="{ row }">{{ (row.durationMin / 60).toFixed(1) }}h</template>
        </el-table-column>
        <el-table-column label="质量" width="160">
          <template #default="{ row }">
            <el-rate :model-value="row.quality" disabled :max="5" size="small" />
          </template>
        </el-table-column>
        <el-table-column prop="dreamNotes" label="备注" min-width="150" show-overflow-tooltip />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { submitSleep, getTodaySleep, getSleepList, analyzeSleep } from '@/api/sleep'
import { sanitizeHtml } from '@/utils/sanitize'

const pageLoading = ref(false)
const submitting = ref(false)
const analyzing = ref(false)
const records = ref([])
const todayRecord = ref(null)
const aiAnalysis = ref('')

const form = reactive({
  recordDate: new Date().toISOString().slice(0, 10),
  sleepTime: null,
  wakeTime: null,
  quality: 3,
  dreamNotes: ''
})

const qualityTexts = ['很差', '较差', '一般', '较好', '很好']

const avgDuration = computed(() => {
  const recent = records.value.slice(0, 7)
  if (recent.length === 0) return 0
  return recent.reduce((s, r) => s + r.durationMin, 0) / recent.length / 60
})

const avgQuality = computed(() => {
  const recent = records.value.slice(0, 7)
  if (recent.length === 0) return 0
  return recent.reduce((s, r) => s + r.quality, 0) / recent.length
})

const bestQuality = computed(() => {
  if (records.value.length === 0) return 0
  return Math.max(...records.value.map(r => r.quality))
})

async function loadRecords() {
  pageLoading.value = true
  try {
    const res = await getSleepList(30)
    records.value = res.data || []
  } finally {
    pageLoading.value = false
  }
}

async function loadToday() {
  try {
    const res = await getTodaySleep()
    todayRecord.value = res.data
    if (todayRecord.value) {
      form.recordDate = todayRecord.value.recordDate
      form.sleepTime = todayRecord.value.sleepTime
      form.wakeTime = todayRecord.value.wakeTime
      form.quality = todayRecord.value.quality
      form.dreamNotes = todayRecord.value.dreamNotes || ''
    }
  } catch {
    // 无今日记录
  }
}

async function handleSubmit() {
  if (!form.sleepTime || !form.wakeTime) {
    ElMessage.warning('请选择入睡和起床时间')
    return
  }
  submitting.value = true
  try {
    await submitSleep({ ...form, quality: form.quality || 3 })
    ElMessage.success('已记录')
    await loadToday()
    await loadRecords()
  } finally {
    submitting.value = false
  }
}

async function handleAnalyze() {
  analyzing.value = true
  try {
    const res = await analyzeSleep()
    aiAnalysis.value = res.data?.analysis || ''
  } finally {
    analyzing.value = false
  }
}

function formatTime(timeStr) {
  if (!timeStr) return '-'
  return timeStr.substring(0, 5)
}

function formatAnalysis(text) {
  if (!text) return ''
  return sanitizeHtml(text.replace(/\n/g, '<br>'))
}

onMounted(() => {
  loadToday()
  loadRecords()
})
</script>

<style scoped>
.sleep-page {
  padding: 8px;
}

.page-header { margin-bottom: 16px; }

.submit-card, .analyze-card, .history-card {
  padding: 20px 24px;
  border-radius: 12px;
  margin-bottom: 20px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #e6edf3;
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stats-row { margin-bottom: 20px; }

.stat-card { background: #161b22; border-color: #30363d; }
.stat-card :deep(.el-statistic__head) { color: #8b949e; font-size: 13px; }
.stat-card :deep(.el-statistic__number) { color: #e6edf3; font-size: 22px; }

.analyze-card { margin-bottom: 20px; }

.ai-analysis-content {
  display: flex;
  align-items: flex-start;
  padding: 16px;
  background: rgba(88, 166, 255, 0.06);
  border-radius: 8px;
  border: 1px solid rgba(88, 166, 255, 0.15);
  color: #e6edf3;
  line-height: 1.8;
  font-size: 14px;
}

.history-card { margin-bottom: 0; }

:deep(.el-rate__icon) { margin-right: 2px; }
</style>