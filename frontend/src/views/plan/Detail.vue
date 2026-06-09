<template>
  <div class="detail-page" v-loading="pageLoading">
    <div v-if="plan" class="detail-container">
      <div class="detail-card glass-card">
        <div class="card-header">
          <div class="header-left">
            <el-button text :icon="ArrowLeft" @click="$router.push('/plan/list')">返回</el-button>
            <h2 class="page-title">{{ plan.planName }}</h2>
            <el-tag :type="plan.planType === 'sport' ? 'success' : 'primary'" size="small" effect="dark">
              {{ plan.planType === 'sport' ? '运动计划' : '饮食计划' }}
            </el-tag>
            <el-tag v-if="plan.status === 1" type="warning" size="small" effect="dark">当前生效</el-tag>
          </div>
          <div class="header-right">
            <el-button type="primary" @click="handleExportPdf">导出 PDF</el-button>
          </div>
        </div>

        <div class="plan-meta">
          <span>{{ plan.durationDays }}天计划</span>
          <span class="meta-divider">|</span>
          <span>开始日期：{{ plan.startDate }}</span>
        </div>
      </div>

      <div v-if="planDays.length > 0" class="days-grid">
        <div
          v-for="day in planDays"
          :key="day.d"
          class="day-card glass-card"
        >
          <div class="day-header">
            <span class="day-num">Day {{ day.d }}</span>
            <span class="day-date">{{ formatDayDate(day.d) }}</span>
          </div>
          <ul class="item-list">
            <li v-for="(item, idx) in day.items" :key="idx" class="item-row">
              <el-icon color="#58a6ff" :size="14"><Check /></el-icon>
              <span>{{ item }}</span>
            </li>
          </ul>
        </div>
      </div>

      <!-- AI动态计划调整 -->
      <div class="adjust-card glass-card">
        <div class="adjust-header">
          <h3><el-icon><MagicStick /></el-icon> AI动态计划调整</h3>
          <el-button
            type="primary"
            size="small"
            :loading="adjusting"
            @click="handleAdjustPlan"
          >
            分析打卡数据并调整计划
          </el-button>
        </div>
        <p class="adjust-tip">AI会分析你最近的打卡完成情况、体重变化和身体状况，智能调整后续计划</p>

        <div v-if="adjustResult" class="adjust-result">
          <div class="adjust-summary">
            <el-tag type="success" effect="dark">调整建议</el-tag>
            <span>{{ adjustResult.summary }}</span>
          </div>
          <div v-if="adjustResult.changes?.length" class="adjust-changes">
            <h4>调整项</h4>
            <ul>
              <li v-for="(change, i) in adjustResult.changes" :key="i">
                <el-tag size="small" :type="change.type === 'increase' ? 'success' : 'warning'">
                  {{ change.type === 'increase' ? '增加' : change.type === 'decrease' ? '减少' : '调整' }}
                </el-tag>
                <span>{{ change.description }}</span>
                <em>{{ change.reason }}</em>
              </li>
            </ul>
          </div>
        </div>
      </div>

      <el-empty v-else description="计划内容为空" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft, Check } from '@element-plus/icons-vue'
import { getPlanDetail } from '@/api/aiPlan'
import { adjustPlan } from '@/api/aiPlan'
import { ElMessage } from 'element-plus'

const route = useRoute()
const pageLoading = ref(false)
const plan = ref(null)
const adjusting = ref(false)
const adjustResult = ref(null)

const planDays = computed(() => {
  if (!plan.value?.aiContent) return []
  try {
    const parsed = JSON.parse(plan.value.aiContent)
    return parsed.days || []
  } catch {
    return []
  }
})

function formatDayDate(dayNum) {
  if (!plan.value?.startDate) return ''
  const start = new Date(plan.value.startDate)
  start.setDate(start.getDate() + dayNum - 1)
  const m = String(start.getMonth() + 1).padStart(2, '0')
  const d = String(start.getDate()).padStart(2, '0')
  return `${m}-${d}`
}

function handleExportPdf() {
  const title = plan.value?.planName || 'AI健康计划'
  let content = `<html><head><meta charset="utf-8"><title>${title}</title>
<style>
body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;background:#0d1117;color:#e6edf3;padding:32px;margin:0}
h1{font-size:22px;color:#58a6ff;margin-bottom:4px}
.meta{font-size:13px;color:#8b949e;margin-bottom:24px}
.day-card{background:#161b22;border-radius:8px;padding:20px 24px;margin-bottom:16px;box-shadow:0 4px 16px rgba(0,0,0,0.25)}
.day-header{font-size:16px;font-weight:600;color:#58a6ff;margin-bottom:12px;border-bottom:1px solid #30363d;padding-bottom:8px}
ul{list-style:none;padding:0;margin:0}
li{font-size:14px;padding:6px 0;color:#e6edf3;display:flex;align-items:flex-start;gap:8px}
li::before{content:'✓';color:#3fb950;flex-shrink:0}
</style></head><body>
<h1>${title}</h1>
<div class="meta">${plan.value?.planType === 'sport' ? '运动计划' : '饮食计划'} · ${plan.value?.durationDays}天 · 开始日期：${plan.value?.startDate}</div>`

  planDays.value.forEach(day => {
    content += `<div class="day-card"><div class="day-header">Day ${day.d} — ${formatDayDate(day.d)}</div><ul>`
    day.items.forEach(item => {
      content += `<li>${item}</li>`
    })
    content += '</ul></div>'
  })

  content += '</body></html>'

  const blob = new Blob([content], { type: 'text/html;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${title}.html`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

async function handleAdjustPlan() {
  if (!plan.value) return
  adjusting.value = true
  adjustResult.value = null
  try {
    const res = await adjustPlan(plan.value.id)
    adjustResult.value = res.data
    ElMessage.success('计划调整建议已生成')
  } catch {
    // handled by interceptor
  } finally {
    adjusting.value = false
  }
}

onMounted(async () => {
  pageLoading.value = true
  try {
    const res = await getPlanDetail(route.params.id)
    plan.value = res.data
  } finally {
    pageLoading.value = false
  }
})
</script>

<style scoped lang="scss">
.detail-page {
  padding: 4px;
}

.detail-container {
  max-width: 860px;
}

.detail-card {
  padding: 28px 32px;
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.plan-meta {
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 4px;
}

.meta-divider {
  margin: 0 6px;
  color: #30363d;
}

.days-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.day-card {
  padding: 20px 24px;
}

.day-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(48, 54, 61, 0.3);
}

.day-num {
  font-size: 16px;
  font-weight: 600;
  color: #58a6ff;
}

.day-date {
  font-size: 12px;
  color: var(--text-secondary);
}

.item-list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.item-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 14px;
  color: var(--text-primary);
  line-height: 1.5;
}

/* AI动态调整 */
.adjust-card {
  margin-top: 20px;
  padding: 20px 24px;
  border: 1px solid rgba(88, 166, 255, 0.2);
}

.adjust-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.adjust-header h3 {
  margin: 0;
  font-size: 15px;
  color: #58a6ff;
  display: flex;
  align-items: center;
  gap: 6px;
}

.adjust-tip {
  font-size: 13px;
  color: #8b949e;
  margin-bottom: 16px;
}

.adjust-result {
  padding: 16px;
  background: rgba(88, 166, 255, 0.06);
  border-radius: 8px;
  border: 1px solid rgba(88, 166, 255, 0.12);
}

.adjust-summary {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 16px;
}

.adjust-summary span {
  color: #c9d1d9;
  font-size: 14px;
  line-height: 1.6;
}

.adjust-changes h4 {
  font-size: 13px;
  color: #58a6ff;
  margin-bottom: 10px;
}

.adjust-changes ul {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.adjust-changes li {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 8px 10px;
  background: #0d1117;
  border-radius: 6px;
  border: 1px solid #21262d;
  font-size: 13px;
  color: #c9d1d9;
  line-height: 1.5;
}

.adjust-changes li em {
  color: #8b949e;
  font-size: 12px;
  font-style: normal;
  margin-left: auto;
  flex-shrink: 0;
}
</style>
