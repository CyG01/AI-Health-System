<template>
  <div class="privacy-page">
    <!-- Page Header -->
    <div class="flex items-center gap-3 mb-5">
      <div class="shield-icon">
        <svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
        </svg>
      </div>
      <div>
        <h2 class="text-xl font-semibold m-0">隐私控制中心</h2>
        <p class="text-sm m-0" style="color: var(--sport-text-secondary, #8b949e)">
          管理数据授权、AI 记忆、数据导出与销毁，全面掌控您的隐私安全
        </p>
      </div>
    </div>

    <NSpin :show="pageLoading">
      <div class="flex flex-col gap-5">
        <!-- Section 1: Data Authorization -->
        <NCard title="数据授权管理" size="small" class="section-card">
          <template #header-extra>
            <NTag :type="consentSaved ? 'success' : 'default'" size="small" round>
              {{ consentSaved ? '已保存' : '未保存' }}
            </NTag>
          </template>

          <NForm label-placement="left" label-width="180px" @submit.prevent="handleSaveConsent">
            <NFormItem label="数据用于模型训练">
              <div class="flex items-center gap-3">
                <NSwitch
                  v-model:value="consentForm.dataConsentForModel"
                  :checked-value="1"
                  :unchecked-value="0"
                  @update:value="consentSaved = false"
                />
                <span class="form-hint">允许将您的匿名化数据用于改进 AI 模型</span>
              </div>
            </NFormItem>

            <NFormItem label="数据用于个性化推荐">
              <div class="flex items-center gap-3">
                <NSwitch
                  v-model:value="consentForm.dataConsentForRecommend"
                  :checked-value="1"
                  :unchecked-value="0"
                  @update:value="consentSaved = false"
                />
                <span class="form-hint">允许基于您的数据提供个性化健康推荐</span>
              </div>
            </NFormItem>

            <NFormItem label=" ">
              <div class="flex items-center gap-2">
                <NTag
                  :type="consentForm.dataConsentForModel === 1 ? 'success' : 'warning'"
                  size="small"
                >
                  模型训练：{{ consentForm.dataConsentForModel === 1 ? '已授权' : '未授权' }}
                </NTag>
                <NTag
                  :type="consentForm.dataConsentForRecommend === 1 ? 'success' : 'warning'"
                  size="small"
                >
                  个性化推荐：{{ consentForm.dataConsentForRecommend === 1 ? '已授权' : '未授权' }}
                </NTag>
              </div>
            </NFormItem>

            <NFormItem label=" ">
              <div class="flex gap-2.5">
                <NButton type="primary" attr-type="submit" :loading="savingConsent" size="small">
                  保存授权
                </NButton>
                <NButton @click="loadConsent" :loading="loadingConsent" size="small">
                  重置
                </NButton>
              </div>
            </NFormItem>
          </NForm>
        </NCard>

        <!-- Section 2: AI Memory Sandbox -->
        <NCard title="AI 记忆沙盒" size="small" class="section-card">
          <template #header-extra>
            <NTag :type="memorySandbox.enabled ? 'info' : 'default'" size="small" round>
              {{ memorySandbox.enabled ? '无痕模式' : '正常模式' }}
            </NTag>
          </template>

          <NAlert
            v-if="memorySandbox.enabled"
            type="info"
            :bordered="false"
            class="mb-4"
            title="无痕模式已开启"
          >
            AI 将不会记忆您的对话内容，每次会话结束后上下文将被彻底清除。
          </NAlert>

          <NForm label-placement="left" label-width="180px">
            <NFormItem label="AI 记忆沙盒 (无痕模式)">
              <div class="flex items-center gap-3">
                <NSwitch
                  v-model:value="memorySandbox.enabled"
                  :loading="memorySandbox.toggling"
                  @update:value="handleToggleSandbox"
                />
                <span class="form-hint">
                  开启后 AI 不保留任何对话记忆，关闭后恢复正常记忆
                </span>
              </div>
            </NFormItem>
          </NForm>
        </NCard>

        <!-- Section 3: Data Export -->
        <NCard title="数据导出" size="small" class="section-card">
          <template #header-extra>
            <NTag v-if="exportState.taskId" :type="exportStatusTagType" size="small" round>
              {{ exportState.statusText }}
            </NTag>
          </template>

          <NAlert type="info" :bordered="false" class="mb-4" title="导出说明">
            您可以将个人数据导出为 JSON 格式存档。导出任务可能需要数分钟，完成后可下载文件。
          </NAlert>

          <div class="flex items-center gap-3 flex-wrap">
            <NButton
              type="primary"
              secondary
              :loading="exportState.requesting"
              :disabled="exportState.polling"
              @click="handleDataExport"
            >
              导出我的数据
            </NButton>

            <NButton
              v-if="exportState.taskId && exportState.status !== 'completed'"
              size="small"
              :loading="exportState.polling"
              @click="pollExportStatus"
            >
              刷新状态
            </NButton>

            <NButton
              v-if="exportState.downloadUrl"
              type="success"
              size="small"
              tag="a"
              :href="exportState.downloadUrl"
              target="_blank"
            >
              下载导出文件
            </NButton>
          </div>

          <NDescriptions
            v-if="exportState.taskId"
            label-placement="left"
            bordered
            :column="1"
            size="small"
            class="mt-4"
          >
            <NDescriptionsItem label="任务 ID">
              {{ exportState.taskId }}
            </NDescriptionsItem>
            <NDescriptionsItem label="当前状态">
              <NTag :type="exportStatusTagType" size="small">
                {{ exportState.statusText }}
              </NTag>
            </NDescriptionsItem>
          </NDescriptions>
        </NCard>

        <!-- Section 4: Data Purge -->
        <NCard title="数据销毁" size="small" class="section-card purge-card">
          <NAlert type="error" :bordered="false" class="mb-4" title="危险操作">
            数据销毁操作不可逆！一旦执行，您的所有健康数据、对话记录、运动日志等将被永久删除，无法恢复。请谨慎操作。
          </NAlert>

          <div class="flex items-center gap-3">
            <NButton
              type="error"
              :loading="purging"
              @click="showPurgeDialog = true"
            >
              一键焚毁我的所有数据
            </NButton>
          </div>
        </NCard>

        <!-- Section 5: Audit Log -->
        <NCard title="审计日志" size="small" class="section-card">
          <template #header-extra>
            <NButton text type="primary" size="small" :loading="auditLoading" @click="loadAuditLogs">
              刷新
            </NButton>
          </template>

          <NCollapse>
            <NCollapseItem title="查看近期隐私操作记录" name="audit">
              <NSpin :show="auditLoading">
                <NDataTable
                  :columns="auditColumns"
                  :data="auditLogs"
                  :bordered="false"
                  :single-line="false"
                  size="small"
                  :row-key="(row: AuditLogRow) => row.id"
                  max-height="400"
                  virtual-scroll
                />
                <NResult
                  v-if="!auditLoading && auditLogs.length === 0"
                  status="info"
                  size="small"
                  title="暂无审计日志"
                  description="您的隐私操作记录将在此展示"
                />
              </NSpin>
            </NCollapseItem>
          </NCollapse>
        </NCard>
      </div>
    </NSpin>

    <!-- Purge Confirmation Modal -->
    <NModal
      v-model:show="showPurgeDialog"
      preset="dialog"
      type="error"
      title="确认销毁所有数据"
      positive-text="确认销毁"
      negative-text="取消"
      :positive-button-props="{ loading: purging }"
      @positive-click="handleDataPurge"
      @negative-click="showPurgeDialog = false"
    >
      <div class="flex flex-col gap-3 py-2">
        <NAlert type="error" :bordered="false">
          此操作不可逆！以下数据将被永久删除：
        </NAlert>
        <NSpace vertical :size="4">
          <div class="flex items-center gap-2">
            <NTag type="error" size="small" round>健康数据</NTag>
            <span class="text-sm">血糖、体重、运动、饮食、睡眠等记录</span>
          </div>
          <div class="flex items-center gap-2">
            <NTag type="error" size="small" round>对话记录</NTag>
            <span class="text-sm">所有 AI 对话历史及记忆</span>
          </div>
          <div class="flex items-center gap-2">
            <NTag type="error" size="small" round>个人偏好</NTag>
            <span class="text-sm">授权设置、通知偏好等配置</span>
          </div>
          <div class="flex items-center gap-2">
            <NTag type="error" size="small" round>健康报告</NTag>
            <span class="text-sm">历史健康报告及分析数据</span>
          </div>
        </NSpace>
      </div>
    </NModal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, h } from 'vue'
import {
  NCard, NForm, NFormItem, NSwitch,
  NButton, NSpin, NTag, NDescriptions, NDescriptionsItem,
  NAlert, NSpace, NDataTable, NCollapse, NCollapseItem,
  NModal, NResult,
  useMessage, useDialog,
  type DataTableColumns
} from 'naive-ui'
import {
  fetchGetPrivacyConsent,
  fetchUpdatePrivacyConsent,
  fetchGetMemorySandbox,
  fetchToggleMemorySandbox,
  fetchDataPurge,
  fetchPrivacyAuditLogs,
  fetchDataExport,
  fetchDataExportStatus
} from '@/service/api'

defineOptions({ name: 'PrivacySettings' })

const message = useMessage()
const dialog = useDialog()

// ──────────────────────────────────────────
// Types
// ──────────────────────────────────────────

interface AuditLogRow {
  id: number
  actionType: string
  actionDescription: string
  result: string
  ipAddress: string
  userAgent: string
  createTime: string
}

interface ExportState {
  taskId: string
  status: string
  statusText: string
  downloadUrl: string
  requesting: boolean
  polling: boolean
}

// ──────────────────────────────────────────
// Page-level loading
// ──────────────────────────────────────────

const pageLoading = ref(false)

// ──────────────────────────────────────────
// Section 1: Data Authorization (Consent)
// ──────────────────────────────────────────

const loadingConsent = ref(false)
const savingConsent = ref(false)
const consentSaved = ref(false)
const consentForm = reactive({
  dataConsentForModel: 0,
  dataConsentForRecommend: 0
})

async function loadConsent() {
  loadingConsent.value = true
  try {
    const { data } = await fetchGetPrivacyConsent()
    if (data) {
      consentForm.dataConsentForModel = data.dataConsentForModel ?? 0
      consentForm.dataConsentForRecommend = data.dataConsentForRecommend ?? 0
      consentSaved.value = true
    }
  } catch {
    /* use defaults */
  } finally {
    loadingConsent.value = false
  }
}

async function handleSaveConsent() {
  savingConsent.value = true
  try {
    await fetchUpdatePrivacyConsent({
      dataConsentForModel: consentForm.dataConsentForModel,
      dataConsentForRecommend: consentForm.dataConsentForRecommend
    })
    consentSaved.value = true
    message.success('数据授权设置已保存')
  } catch {
    message.error('保存失败，请重试')
  } finally {
    savingConsent.value = false
  }
}

// ──────────────────────────────────────────
// Section 2: AI Memory Sandbox
// ──────────────────────────────────────────

const memorySandbox = reactive({
  enabled: false,
  toggling: false
})

async function loadMemorySandbox() {
  try {
    const { data } = await fetchGetMemorySandbox()
    if (data) {
      memorySandbox.enabled = data.enabled ?? false
    }
  } catch {
    /* use default */
  }
}

async function handleToggleSandbox(val: boolean) {
  memorySandbox.toggling = true
  try {
    await fetchToggleMemorySandbox(val)
    memorySandbox.enabled = val
    message.success(val ? '已开启无痕模式，AI 将不再记忆对话' : '已关闭无痕模式，AI 记忆已恢复')
  } catch {
    // Revert on failure
    memorySandbox.enabled = !val
    message.error('切换失败，请重试')
  } finally {
    memorySandbox.toggling = false
  }
}

// ──────────────────────────────────────────
// Section 3: Data Export
// ──────────────────────────────────────────

const exportState = reactive<ExportState>({
  taskId: '',
  status: '',
  statusText: '',
  downloadUrl: '',
  requesting: false,
  polling: false
})

const exportStatusTagType = computed(() => {
  switch (exportState.status) {
    case 'completed': return 'success'
    case 'processing': return 'info'
    case 'failed': return 'error'
    default: return 'default'
  }
})

async function handleDataExport() {
  dialog.warning({
    title: '确认导出数据',
    content: '将为您生成一份包含所有个人数据的 JSON 存档文件，此过程可能需要数分钟。',
    positiveText: '开始导出',
    negativeText: '取消',
    onPositiveClick: async () => {
      exportState.requesting = true
      try {
        const { data } = await fetchDataExport({ exportType: 'json', exportScope: 'all' })
        if (data) {
          exportState.taskId = data.taskId
          exportState.status = data.status
          exportState.statusText = mapExportStatus(data.status)
          message.success('导出任务已创建，正在处理中...')
          // Start polling
          startExportPolling()
        }
      } catch {
        message.error('创建导出任务失败')
      } finally {
        exportState.requesting = false
      }
    }
  })
}

let exportPollTimer: ReturnType<typeof setInterval> | null = null

function startExportPolling() {
  stopExportPolling()
  exportState.polling = true
  exportPollTimer = setInterval(async () => {
    await pollExportStatus()
  }, 5000)
}

function stopExportPolling() {
  if (exportPollTimer) {
    clearInterval(exportPollTimer)
    exportPollTimer = null
  }
  exportState.polling = false
}

async function pollExportStatus() {
  if (!exportState.taskId) return
  try {
    const { data } = await fetchDataExportStatus(exportState.taskId)
    if (data) {
      exportState.status = data.status
      exportState.statusText = mapExportStatus(data.status)
      if (data.downloadUrl) {
        exportState.downloadUrl = data.downloadUrl
      }
      if (data.status === 'completed' || data.status === 'failed') {
        stopExportPolling()
        if (data.status === 'completed') {
          message.success('数据导出完成，可以下载文件')
        } else {
          message.error('数据导出失败')
        }
      }
    }
  } catch {
    message.error('查询导出状态失败')
  }
}

function mapExportStatus(status: string): string {
  const map: Record<string, string> = {
    pending: '等待中',
    processing: '处理中',
    completed: '已完成',
    failed: '失败'
  }
  return map[status] || status
}

// ──────────────────────────────────────────
// Section 4: Data Purge
// ──────────────────────────────────────────

const showPurgeDialog = ref(false)
const purging = ref(false)

async function handleDataPurge() {
  purging.value = true
  try {
    const { data } = await fetchDataPurge({
      dataTypes: ['health_data', 'chat_history', 'preferences', 'reports'],
      reason: '用户主动请求销毁所有数据'
    })
    if (data) {
      message.success(`数据销毁请求已提交，预计 ${data.estimatedTime || '数分钟'} 内完成`)
      showPurgeDialog.value = false
    }
  } catch {
    message.error('数据销毁请求失败，请联系管理员')
  } finally {
    purging.value = false
  }
}

// ──────────────────────────────────────────
// Section 5: Audit Logs
// ──────────────────────────────────────────

const auditLoading = ref(false)
const auditLogs = ref<AuditLogRow[]>([])

const auditColumns: DataTableColumns<AuditLogRow> = [
  {
    title: '时间',
    key: 'createTime',
    width: 170,
    render(row) {
      return h('span', { class: 'text-xs' }, formatTime(row.createTime))
    }
  },
  {
    title: '操作类型',
    key: 'actionType',
    width: 120,
    render(row) {
      const typeMap: Record<string, 'info' | 'warning' | 'error' | 'success'> = {
        LOGIN: 'info',
        CONSENT_UPDATE: 'warning',
        DATA_EXPORT: 'success',
        DATA_PURGE: 'error',
        SANDBOX_TOGGLE: 'info'
      }
      return h(NTag, { size: 'small', type: typeMap[row.actionType] || 'default' }, { default: () => row.actionType })
    }
  },
  {
    title: '描述',
    key: 'actionDescription',
    ellipsis: { tooltip: true }
  },
  {
    title: '结果',
    key: 'result',
    width: 80,
    render(row) {
      const isSuccess = row.result?.toLowerCase() === 'success'
      return h(
        NTag,
        { size: 'small', type: isSuccess ? 'success' : 'error' },
        { default: () => (isSuccess ? '成功' : '失败') }
      )
    }
  },
  {
    title: 'IP 地址',
    key: 'ipAddress',
    width: 140
  }
]

function formatTime(dateStr: string): string {
  if (!dateStr) return '-'
  try {
    const d = new Date(dateStr)
    return d.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    })
  } catch {
    return dateStr
  }
}

async function loadAuditLogs() {
  auditLoading.value = true
  try {
    const { data } = await fetchPrivacyAuditLogs(50)
    if (data) {
      auditLogs.value = data as AuditLogRow[]
    }
  } catch {
    message.error('加载审计日志失败')
  } finally {
    auditLoading.value = false
  }
}

// ──────────────────────────────────────────
// Lifecycle
// ──────────────────────────────────────────

onMounted(async () => {
  pageLoading.value = true
  try {
    await Promise.all([
      loadConsent(),
      loadMemorySandbox(),
      loadAuditLogs()
    ])
  } finally {
    pageLoading.value = false
  }
})
</script>

<style scoped>
.privacy-page {
  padding: 0;
}

.shield-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: var(--chart-sky, #38bdf8);
  color: #fff;
  flex-shrink: 0;
}

.section-card {
  background: var(--sport-bg-surface, #fff);
}

.purge-card {
  border-color: var(--color-danger, #e53e3e) !important;
}

.form-hint {
  color: var(--sport-text-secondary, #8b949e);
  font-size: 13px;
}
</style>
