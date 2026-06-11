<template>
  <div class="export-page">
    <div class="page-header">
      <h2>数据导出</h2>
      <p class="page-desc">导出您的健康数据，支持CSV和Excel格式</p>
    </div>

    <el-row :gutter="20">
      <el-col :span="8">
        <el-card class="export-card" shadow="hover">
          <div class="export-card-content">
            <el-icon :size="48" color="#58a6ff"><Document /></el-icon>
            <h3>CSV 格式</h3>
            <p>导出饮食、运动、体重、打卡数据为CSV格式，可用Excel、Google Sheets等打开</p>
            <el-button type="primary" :loading="csvLoading" @click="exportCSV" :icon="Download">
              导出 CSV
            </el-button>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="export-card" shadow="hover">
          <div class="export-card-content">
            <el-icon :size="48" color="#67c23a"><Grid /></el-icon>
            <h3>Excel 格式</h3>
            <p>导出为Excel (.xlsx) 格式，支持多Sheet、样式美化，专业数据分析</p>
            <el-button type="success" :loading="excelLoading" @click="exportExcel" :icon="Download">
              导出 Excel
            </el-button>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="export-card" shadow="hover">
          <div class="export-card-content">
            <el-icon :size="48" color="#e6a23c"><InfoFilled /></el-icon>
            <h3>导出说明</h3>
            <ul class="export-info">
              <li>导出数据包含：饮食记录、运动记录、体重记录、每日打卡</li>
              <li>文件包含时间范围内的全部历史数据</li>
              <li>需要Pro及以上订阅等级</li>
              <li>数据仅用于个人存档和分析</li>
            </ul>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 导出历史 -->
    <div class="history-card glass-card" v-if="false">
      <h3 class="card-title">导出历史</h3>
      <el-empty description="暂无导出记录" :image-size="80" />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Document, Grid, InfoFilled } from '@element-plus/icons-vue'
import { exportCSV as exportCSVApi, exportExcel as exportExcelApi } from '@/api/export'

const csvLoading = ref(false)
const excelLoading = ref(false)

function downloadBlob(data, filename) {
  const url = window.URL.createObjectURL(new Blob([data]))
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

async function exportCSV() {
  csvLoading.value = true
  try {
    const res = await exportCSVApi()
    downloadBlob(res, `health-data-${new Date().toISOString().slice(0, 10)}.csv`)
    ElMessage.success('CSV导出成功')
  } catch { ElMessage.error('CSV导出失败，请确认订阅等级') }
  finally { csvLoading.value = false }
}

async function exportExcel() {
  excelLoading.value = true
  try {
    const res = await exportExcelApi()
    downloadBlob(res, `health-data-${new Date().toISOString().slice(0, 10)}.xlsx`)
    ElMessage.success('Excel导出成功')
  } catch { ElMessage.error('Excel导出失败，请确认订阅等级') }
  finally { excelLoading.value = false }
}
</script>

<style scoped>
.export-page { padding: 0; }
.page-header { margin-bottom: 20px; }
.page-header h2 { margin: 0 0 4px; font-size: 20px; font-weight: 600; }
.page-desc { margin: 0; color: #8b949e; font-size: 14px; }
.export-card { height: 100%; }
.export-card-content { text-align: center; padding: 20px 10px; display: flex; flex-direction: column; align-items: center; gap: 12px; }
.export-card-content h3 { margin: 0; font-size: 18px; }
.export-card-content p { margin: 0; color: #8b949e; font-size: 13px; line-height: 1.6; }
.export-info { text-align: left; margin: 0; padding-left: 20px; color: #8b949e; font-size: 13px; line-height: 1.8; }
.history-card { padding: 20px; margin-top: 20px; }
.card-title { margin: 0 0 16px; font-size: 16px; font-weight: 600; }
</style>