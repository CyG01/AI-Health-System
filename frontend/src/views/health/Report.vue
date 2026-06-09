<template>
  <div class="health-report-page" v-loading="pageLoading">
    <div class="page-header">
      <h2>AI健康报告</h2>
      <div class="header-actions">
        <el-button type="primary" :loading="generating" @click="handleGenerate('weekly')">
          生成周报
        </el-button>
        <el-button :loading="generating" @click="handleGenerate('monthly')">
          生成月报
        </el-button>
      </div>
    </div>

    <div v-if="reports.length > 0" class="report-list">
      <div
        v-for="report in reports"
        :key="report.id"
        class="report-card glass-card"
        :class="{ unread: report.isRead === 0 }"
        @click="viewReport(report)"
      >
        <div class="report-header">
          <div class="report-type">
            <el-tag
              :type="report.reportType === 'weekly' ? 'primary' : 'success'"
              size="small"
              effect="dark"
            >
              {{ report.reportType === 'weekly' ? '周报' : '月报' }}
            </el-tag>
            <span class="report-period">{{ report.reportPeriod }}</span>
          </div>
          <div class="report-meta">
            <span class="report-time">{{ formatTime(report.createTime) }}</span>
            <span v-if="report.isRead === 0" class="unread-dot"></span>
          </div>
        </div>
        <div class="report-preview" v-if="report.aiContent">
          {{ getPreview(report.aiContent) }}
        </div>
      </div>
    </div>

    <el-empty v-else description="暂无健康报告" :image-size="100">
      <el-button type="primary" @click="handleGenerate('weekly')">生成第一份报告</el-button>
    </el-empty>

    <!-- 报告详情弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="'AI健康' + (detailReport?.reportType === 'weekly' ? '周报' : '月报')"
      width="680px"
      destroy-on-close
      class="report-dialog"
    >
      <div v-if="reportData" class="report-content">
        <!-- 综合评分 -->
        <div class="report-score" v-if="reportData.score">
          <div class="score-ring">
            <span class="score-num">{{ reportData.score }}</span>
            <span class="score-label">综合评分</span>
          </div>
        </div>

        <!-- 总体概述 -->
        <div class="report-section" v-if="reportData.summary">
          <h4>总体概述</h4>
          <p>{{ reportData.summary }}</p>
        </div>

        <!-- 亮点 -->
        <div class="report-section" v-if="reportData.achievements?.length">
          <h4>亮点成就</h4>
          <ul>
            <li v-for="(a, i) in reportData.achievements" :key="i">{{ a }}</li>
          </ul>
        </div>

        <!-- 关注点 -->
        <div class="report-section" v-if="reportData.concerns?.length">
          <h4>需要关注</h4>
          <ul class="concerns">
            <li v-for="(c, i) in reportData.concerns" :key="i">{{ c }}</li>
          </ul>
        </div>

        <!-- 建议 -->
        <div class="report-section" v-if="reportData.suggestions?.length">
          <h4>改善建议</h4>
          <ul class="suggestions">
            <li v-for="(s, i) in reportData.suggestions" :key="i">{{ s }}</li>
          </ul>
        </div>
      </div>
      <div v-else class="report-raw" v-html="rawContent"></div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { generateReport, getReportList } from '@/api/healthReport'

const pageLoading = ref(false)
const generating = ref(false)
const reports = ref([])
const dialogVisible = ref(false)
const detailReport = ref(null)

const reportData = computed(() => {
  if (!detailReport.value?.aiContent) return null
  try {
    return JSON.parse(detailReport.value.aiContent)
  } catch {
    return null
  }
})

const rawContent = computed(() => {
  if (!detailReport.value?.aiContent) return ''
  try {
    JSON.parse(detailReport.value.aiContent)
    return ''
  } catch {
    return detailReport.value.aiContent.replace(/\n/g, '<br>')
  }
})

async function loadReports() {
  pageLoading.value = true
  try {
    const res = await getReportList(1, 20)
    reports.value = res.data || []
  } finally {
    pageLoading.value = false
  }
}

async function handleGenerate(type) {
  generating.value = true
  try {
    const res = await generateReport(type)
    ElMessage.success('报告生成成功')
    reports.value.unshift(res.data)
  } finally {
    generating.value = false
  }
}

function viewReport(report) {
  detailReport.value = report
  dialogVisible.value = true
}

function getPreview(content) {
  if (!content) return ''
  try {
    const data = JSON.parse(content)
    return data.summary || content.substring(0, 100)
  } catch {
    return content.substring(0, 100)
  }
}

function formatTime(time) {
  if (!time) return ''
  return time.substring(0, 10)
}

onMounted(loadReports)
</script>

<style scoped>
.health-report-page { padding: 8px; }

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header-actions { display: flex; gap: 10px; }

.report-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.report-card {
  padding: 18px 22px;
  border-radius: 10px;
  cursor: pointer;
  transition: border-color 0.2s;
  border: 1px solid #30363d;
}

.report-card:hover { border-color: #58a6ff; }

.report-card.unread { border-color: rgba(88, 166, 255, 0.4); }

.report-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.report-type { display: flex; align-items: center; gap: 10px; }

.report-period {
  color: #e6edf3;
  font-weight: 600;
  font-size: 15px;
}

.report-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #8b949e;
  font-size: 13px;
}

.unread-dot {
  width: 8px;
  height: 8px;
  background: #58a6ff;
  border-radius: 50%;
}

.report-preview {
  color: #8b949e;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 报告弹窗样式 */
.report-content {
  color: #e6edf3;
}

.report-score {
  text-align: center;
  margin-bottom: 24px;
}

.score-ring {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 90px;
  height: 90px;
  border-radius: 50%;
  border: 3px solid;
  border-image: linear-gradient(135deg, #3fb950, #58a6ff) 1;
}

.score-num {
  font-size: 32px;
  font-weight: 700;
  background: linear-gradient(135deg, #3fb950, #58a6ff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.score-label { font-size: 11px; color: #8b949e; margin-top: 2px; }

.report-section {
  margin-bottom: 20px;
}

.report-section h4 {
  font-size: 15px;
  color: #58a6ff;
  margin-bottom: 8px;
  padding-bottom: 6px;
  border-bottom: 1px solid #21262d;
}

.report-section p, .report-section li {
  font-size: 14px;
  line-height: 1.8;
  color: #c9d1d9;
}

.report-section ul { padding-left: 18px; }

.concerns li { color: #fa8c16; }
.suggestions li { color: #3fb950; }

.report-raw {
  color: #c9d1d9;
  line-height: 1.8;
  font-size: 14px;
}
</style>