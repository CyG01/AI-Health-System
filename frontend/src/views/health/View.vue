<template>
  <div class="health-view-page" v-loading="pageLoading">
    <div v-if="hasRecord" class="health-view-container">
      <div class="profile-card glass-card">
        <div class="card-header">
          <h2 class="page-title">健康档案</h2>
          <el-button type="primary" @click="$router.push('/health/form')">编辑档案</el-button>
        </div>

        <div class="metrics-grid">
          <div class="metric-item">
            <span class="metric-label">身高</span>
            <span class="metric-value">{{ record.height }} <small>cm</small></span>
          </div>
          <div class="metric-item">
            <span class="metric-label">体重</span>
            <span class="metric-value">{{ record.weight }} <small>kg</small></span>
          </div>
          <div class="metric-item">
            <span class="metric-label">BMI</span>
            <span class="metric-value" :style="{ color: bmiColor }">{{ record.bmi }}</span>
          </div>
          <div class="metric-item">
            <span class="metric-label">基础代谢率(BMR)</span>
            <span class="metric-value">{{ record.bmr }} <small>kcal/天</small></span>
          </div>
          <div class="metric-item">
            <span class="metric-label">每日所需热量</span>
            <span class="metric-value">{{ record.dailyCalorie }} <small>kcal</small></span>
          </div>
        </div>

        <div class="detail-section" v-if="record.goal">
          <span class="detail-label">健康目标</span>
          <span class="detail-text">{{ record.goal }}</span>
        </div>
        <div class="detail-section" v-if="record.diseaseHistory">
          <span class="detail-label">既往病史</span>
          <span class="detail-text">{{ record.diseaseHistory }}</span>
        </div>
        <div class="detail-section" v-if="record.allergyHistory">
          <span class="detail-label">过敏史</span>
          <span class="detail-text">{{ record.allergyHistory }}</span>
        </div>
        <div class="detail-section" v-if="record.exerciseHabit">
          <span class="detail-label">运动习惯</span>
          <span class="detail-text">{{ record.exerciseHabit }}</span>
        </div>
        <div class="detail-section" v-if="record.dietHabit">
          <span class="detail-label">饮食习惯</span>
          <span class="detail-text">{{ record.dietHabit }}</span>
        </div>
      </div>

      <div class="bmi-card glass-card">
        <h2 class="page-title">BMI 仪表盘</h2>
        <div ref="bmiChartRef" class="bmi-chart"></div>
      </div>

      <div v-if="assessment" class="assessment-card glass-card">
        <h2 class="page-title">健康风险评估</h2>
        <div class="bmi-level-badge" :style="{ background: bmiLevelBg }">
          {{ assessment.bmiLevel }}
        </div>
        <ul class="risk-list">
          <li v-for="(risk, idx) in assessment.risks" :key="idx" class="risk-item">
            <el-icon color="#58a6ff"><WarningFilled /></el-icon>
            <span>{{ risk }}</span>
          </li>
        </ul>
      </div>
    </div>

    <div v-else class="empty-card glass-card">
      <el-empty description="暂无健康档案">
        <el-button type="primary" @click="$router.push('/health/create')">立即创建</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getLatestHealth, getHealthAssessment } from '@/api/health'
import echarts from '@/utils/echarts'
import { WarningFilled } from '@element-plus/icons-vue'

const router = useRouter()
const pageLoading = ref(false)
const record = ref({})
const assessment = ref(null)
const hasRecord = ref(false)
const bmiChartRef = ref(null)
let bmiChartInstance = null

const bmiColor = computed(() => {
  const bmi = record.value.bmi
  if (!bmi) return '#8b949e'
  if (bmi < 18.5) return '#d29922'
  if (bmi < 24) return '#3fb950'
  if (bmi < 28) return '#d29922'
  return '#f85149'
})

const bmiLevelBg = computed(() => {
  const level = assessment.value?.bmiLevel
  if (level === '正常') return '#3fb950'
  if (level === '偏瘦' || level === '偏胖') return '#d29922'
  if (level === '肥胖') return '#f85149'
  return '#30363d'
})

function renderBmiGauge() {
  if (!bmiChartRef.value) return
  if (!bmiChartInstance) {
    bmiChartInstance = echarts.init(bmiChartRef.value)
  }

  const bmi = record.value.bmi || 0
  const option = {
    series: [
      {
        type: 'gauge',
        startAngle: 210,
        endAngle: -30,
        min: 10,
        max: 40,
        center: ['50%', '55%'],
        radius: '85%',
        axisLine: {
          lineStyle: {
            width: 20,
            color: [
              [0.25, '#3fb950'],
              [0.5, '#d29922'],
              [0.75, '#f85149'],
              [1, '#f85149']
            ]
          }
        },
        axisTick: {
          show: false
        },
        splitLine: {
          show: false
        },
        axisLabel: {
          show: false
        },
        pointer: {
          length: '70%',
          width: 6,
          itemStyle: {
            color: '#58a6ff'
          }
        },
        detail: {
          valueAnimation: true,
          formatter: '{value}',
          color: '#e6edf3',
          fontSize: 36,
          fontWeight: 700,
          offsetCenter: [0, '75%']
        },
        title: {
          color: '#8b949e',
          fontSize: 12,
          offsetCenter: [0, '95%']
        },
        data: [
          {
            value: bmi,
            name: 'BMI 指数'
          }
        ]
      }
    ]
  }

  bmiChartInstance.setOption(option)
}

function loadData() {
  pageLoading.value = true
  Promise.all([
    getLatestHealth().then(res => {
      record.value = res.data
      hasRecord.value = true
    }),
    getHealthAssessment().then(res => {
      assessment.value = res.data
    })
  ])
  .catch(() => {
    hasRecord.value = false
  })
  .finally(() => {
    pageLoading.value = false
  })
}

watch(hasRecord, async (val) => {
  if (val) {
    await nextTick()
    renderBmiGauge()
  }
})

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.health-view-page {
  padding: 4px;
}

.health-view-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-width: 860px;
}

.profile-card,
.assessment-card {
  padding: 28px 32px;
}

.bmi-card {
  padding: 28px 32px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.metric-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px 16px;
  background: rgba(88, 166, 255, 0.06);
  border-radius: 8px;
}

.metric-label {
  font-size: 12px;
  color: var(--text-secondary);
}

.metric-value {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);

  small {
    font-size: 12px;
    font-weight: 400;
    color: var(--text-secondary);
  }
}

.detail-section {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px 0;
  border-bottom: 1px solid rgba(48, 54, 61, 0.25);
}

.detail-label {
  font-size: 12px;
  color: var(--text-secondary);
}

.detail-text {
  font-size: 14px;
  color: var(--text-primary);
}

.bmi-chart {
  width: 100%;
  height: 300px;
}

.bmi-level-badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 20px;
  border-radius: 20px;
  font-size: 16px;
  font-weight: 600;
  color: #fff;
  margin-top: 8px;
  margin-bottom: 16px;
}

.risk-list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.risk-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  font-size: 14px;
  color: var(--text-primary);
  line-height: 1.6;

  .el-icon {
    margin-top: 2px;
    flex-shrink: 0;
  }
}

.empty-card {
  padding: 64px 32px;
  display: flex;
  justify-content: center;
}
</style>
