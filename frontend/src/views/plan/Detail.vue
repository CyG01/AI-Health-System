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

      <el-empty v-else description="计划内容为空" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft, Check } from '@element-plus/icons-vue'
import { getPlanDetail } from '@/api/aiPlan'

const route = useRoute()
const pageLoading = ref(false)
const plan = ref(null)

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
</style>
