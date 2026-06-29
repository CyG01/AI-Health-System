<template>
  <div class="export-page">
    <div class="page-header">
      <h2 class="text-xl font-semibold">{{ $t('export.title') || '数据导出' }}</h2>
      <p class="text-sm text-secondary">{{ $t('export.desc') || '导出您的健康数据，支持CSV和Excel格式' }}</p>
    </div>

    <NGrid :x-gap="16" :y-gap="16" :cols="3" item-responsive responsive="screen">
      <!-- CSV 导出 -->
      <NGi span="3 m:1">
        <NCard class="h-full">
          <div class="export-card-content">
            <NIcon :size="48" color="#58a6ff"><DocumentTextOutline /></NIcon>
            <h3 class="text-lg font-semibold m-0">CSV {{ $t('export.format') || '格式' }}</h3>
            <p class="text-[13px] text-secondary leading-relaxed m-0">
              {{ $t('export.csvDesc') || '导出饮食、运动、体重、打卡数据为CSV格式，可用Excel、Google Sheets等打开' }}
            </p>
            <NButton type="primary" :loading="csvLoading" @click="exportCSV">
              <template #icon><NIcon><DownloadOutline /></NIcon></template>
              {{ $t('export.exportCsv') || '导出 CSV' }}
            </NButton>
          </div>
        </NCard>
      </NGi>

      <!-- Excel 导出 -->
      <NGi span="3 m:1">
        <NCard class="h-full">
          <div class="export-card-content">
            <NIcon :size="48" color="#3fb950"><GridOutline /></NIcon>
            <h3 class="text-lg font-semibold m-0">Excel {{ $t('export.format') || '格式' }}</h3>
            <p class="text-[13px] text-secondary leading-relaxed m-0">
              {{ $t('export.excelDesc') || '导出为Excel (.xlsx) 格式，支持多Sheet、样式美化，专业数据分析' }}
            </p>
            <NButton type="success" :loading="excelLoading" @click="exportExcel">
              <template #icon><NIcon><DownloadOutline /></NIcon></template>
              {{ $t('export.exportExcel') || '导出 Excel' }}
            </NButton>
          </div>
        </NCard>
      </NGi>

      <!-- 导出说明 -->
      <NGi span="3 m:1">
        <NCard class="h-full">
          <div class="export-card-content">
            <NIcon :size="48" color="#d29922"><InformationCircleOutline /></NIcon>
            <h3 class="text-lg font-semibold m-0">{{ $t('export.info') || '导出说明' }}</h3>
            <ul class="export-info">
              <li>{{ $t('export.info1') || '导出数据包含：饮食记录、运动记录、体重记录、每日打卡' }}</li>
              <li>{{ $t('export.info2') || '文件包含时间范围内的全部历史数据' }}</li>
              <li>{{ $t('export.info3') || '需要Pro及以上订阅等级' }}</li>
              <li>{{ $t('export.info4') || '数据仅用于个人存档和分析' }}</li>
            </ul>
          </div>
        </NCard>
      </NGi>
    </NGrid>

    <!-- 导出历史 -->
    <NCard :title="$t('export.history') || '导出历史'" class="mt-5">
      <NEmpty v-if="!exportHistory.length" :description="$t('export.noHistory') || '暂无导出记录'" />
      <NDataTable
        v-else
        :data="exportHistory"
        :columns="historyColumns"
        :bordered="false"
        striped
        size="small"
      />
    </NCard>
  </div>
</template>

<script setup lang="ts">
import { ref, h } from 'vue'
import {
  NCard, NGrid, NGi, NButton, NIcon, NDataTable, NEmpty, NTag,
  useMessage
} from 'naive-ui'
import type { DataTableColumns } from 'naive-ui'
import { DocumentTextOutline, GridOutline, InformationCircleOutline, DownloadOutline } from '@vicons/ionicons5'
import { fetchExportCSV, fetchExportExcel } from '@/service/api'

defineOptions({ name: 'DataExport' })
const message = useMessage()

interface ExportRecord {
  filename: string
  type: string
  time: string
}

const csvLoading = ref(false)
const excelLoading = ref(false)
const exportHistory = ref<ExportRecord[]>([])

const historyColumns: DataTableColumns<ExportRecord> = [
  { title: '文件名', key: 'filename', minWidth: 200 },
  {
    title: '类型', key: 'type', width: 80,
    render: (row) => h(NTag, {
      size: 'small',
      type: row.type === 'csv' ? 'success' : 'info'
    }, { default: () => row.type.toUpperCase() })
  },
  { title: '导出时间', key: 'time', width: 160 },
  {
    title: '状态', key: 'status', width: 80,
    render: () => h(NTag, { size: 'small', type: 'success' }, { default: () => '完成' })
  }
]

function addToHistory(filename: string, type: string) {
  exportHistory.value.unshift({
    filename,
    type,
    time: new Date().toLocaleString('zh-CN')
  })
}

function downloadBlob(data: BlobPart, filename: string) {
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
    const { data } = await fetchExportCSV()
    const filename = `health-data-${new Date().toISOString().slice(0, 10)}.csv`
    downloadBlob(data as BlobPart, filename)
    addToHistory(filename, 'csv')
    message.success('CSV导出成功')
  } catch {
    message.error('CSV导出失败，请确认订阅等级')
  }
  finally { csvLoading.value = false }
}

async function exportExcel() {
  excelLoading.value = true
  try {
    const { data } = await fetchExportExcel()
    const filename = `health-data-${new Date().toISOString().slice(0, 10)}.xlsx`
    downloadBlob(data as BlobPart, filename)
    addToHistory(filename, 'xlsx')
    message.success('Excel导出成功')
  } catch {
    message.error('Excel导出失败，请确认订阅等级')
  }
  finally { excelLoading.value = false }
}
</script>

<style scoped>
.export-page { padding: 0; }

.page-header { margin-bottom: 20px; }

.export-card-content {
  text-align: center;
  padding: 20px 10px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.export-info {
  text-align: left;
  margin: 0;
  padding-left: 20px;
  color: #8b949e;
  font-size: 13px;
  line-height: 1.8;
}

.text-secondary {
  color: #8b949e;
}
</style>
