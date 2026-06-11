<template>
  <div class="generate-page">
    <MedicalDisclaimerBanner />
    <div v-if="!streaming" class="form-card glass-card">
      <h2 class="page-title">AI 智能计划生成</h2>
      <p class="page-desc">基于您的健康档案，由 DeepSeek 为您量身定制个性化健康计划（运动/饮食/综合/康复/冥想）</p>
      <el-alert title="每日最多生成3次计划" type="info" :closable="false" show-icon class="limit-tip" />

      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" class="generate-form">
        <el-form-item label="计划类型" prop="planType">
          <el-radio-group v-model="form.planType">
            <el-radio value="sport">运动计划</el-radio>
            <el-radio value="diet">饮食计划</el-radio>
            <el-radio value="comprehensive">综合计划</el-radio>
            <el-radio value="rehabilitation">康复计划</el-radio>
            <el-radio value="meditation">冥想放松</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="计划天数" prop="durationDays">
          <el-radio-group v-model="form.durationDays">
            <el-radio :value="7">7天</el-radio>
            <el-radio :value="30">30天</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="运动强度" prop="intensity">
          <el-select v-model="form.intensity" placeholder="请选择运动强度" clearable style="width: 240px">
            <el-option label="轻松（适合入门）" value="轻松" />
            <el-option label="适中（常规训练）" value="适中" />
            <el-option label="高强度（挑战极限）" value="高强度" />
          </el-select>
        </el-form-item>

        <el-form-item label="口味偏好" prop="tastePreference" v-if="form.planType === 'diet'">
          <el-select v-model="form.tastePreference" placeholder="请选择口味偏好" clearable style="width: 240px">
            <el-option label="清淡" value="清淡" />
            <el-option label="家常" value="家常" />
            <el-option label="低脂" value="低脂" />
            <el-option label="高蛋白" value="高蛋白" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="generating"
            :disabled="generating"
            @click="handleGenerate"
          >
            {{ generating ? '正在生成中...' : '开始生成' }}
          </el-button>
          <el-button :disabled="generating" @click="$router.push('/plan/list')">返回列表</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div v-if="streaming" class="streaming-container glass-card">
      <div class="streaming-header">
        <div class="streaming-header-left">
          <span class="terminal-dot" style="background:#3fb950"></span>
          <span class="terminal-dot" style="background:#d29922"></span>
          <span class="terminal-dot" style="background:#f85149"></span>
          <span class="terminal-label">{{ planTypeLabel }} · {{ form.durationDays }}天 · AI生成中</span>
        </div>
        <div class="streaming-header-right">
          <span v-if="streamStatus === 'streaming'" class="status-badge streaming">
            <span class="pulse-dot"></span>实时生成中
          </span>
          <span v-else-if="streamStatus === 'complete'" class="status-badge complete">
            <el-icon><CircleCheck /></el-icon>生成成功
          </span>
          <span v-else-if="streamStatus === 'error'" class="status-badge error">
            <el-icon><CircleClose /></el-icon>生成失败
          </span>
        </div>
      </div>
      <div class="streaming-body" ref="streamBodyRef">
        <pre class="streaming-text">{{ displayText }}<span v-if="streamStatus === 'streaming'" class="cursor-blink">|</span></pre>
      </div>
      <div class="streaming-footer">
        <template v-if="streamStatus === 'complete'">
          <el-button type="primary" size="small" @click="goToPlanList">
            生成成功！正在跳转... ({{ countdown }}秒)
          </el-button>
        </template>
        <template v-else-if="streamStatus === 'error'">
          <el-alert :title="errorMessage" type="error" :closable="false" show-icon class="error-alert" />
          <el-button type="primary" size="small" @click="resetForm">重新生成</el-button>
        </template>
        <span v-else class="bytes-info">{{ totalChars }} 字符已生成</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { CircleCheck, CircleClose } from '@element-plus/icons-vue'
import { generatePlanStream } from '@/api/aiPlan'
import MedicalDisclaimerBanner from '@/components/MedicalDisclaimerBanner.vue'

const router = useRouter()
const formRef = ref(null)
const streamBodyRef = ref(null)
const generating = ref(false)
const streaming = ref(false)
const displayText = ref('')
const streamStatus = ref('streaming')
const errorMessage = ref('')
const totalChars = ref(0)
const countdown = ref(0)

let typewriterQueue = []
let typewriterTimer = null
let aborted = false
let countdownTimer = null

const form = reactive({
  planType: 'sport',
  durationDays: 7,
  intensity: '',
  tastePreference: ''
})

const planTypeLabel = computed(() => {
  const map = { sport: '运动计划', diet: '饮食计划', comprehensive: '综合计划', rehabilitation: '康复计划', meditation: '冥想放松' }
  return map[form.planType] || '健康计划'
})

const rules = {
  planType: [{ required: true, message: '请选择计划类型', trigger: 'change' }],
  durationDays: [{ required: true, message: '请选择计划天数', trigger: 'change' }]
}

function autoScroll() {
  if (streamBodyRef.value) {
    streamBodyRef.value.scrollTop = streamBodyRef.value.scrollHeight
  }
}

function startTypewriter(fullText) {
  const chars = fullText.split('')
  typewriterQueue.push(...chars)
  if (!typewriterTimer) {
    runTypewriter()
  }
}

function runTypewriter() {
  typewriterTimer = setInterval(() => {
    if (aborted) return
    if (typewriterQueue.length === 0) {
      clearInterval(typewriterTimer)
      typewriterTimer = null
      return
    }
    const chunk = typewriterQueue.splice(0, 1)
    displayText.value += chunk.join('')
    totalChars.value = displayText.value.length
    autoScroll()
  }, 20)
}

function startCountdown(seconds) {
  countdown.value = seconds
  countdownTimer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(countdownTimer)
      countdownTimer = null
      router.push('/plan/list')
    }
  }, 1000)
}

function handleIncomingMessage(data) {
  if (aborted) return

  if (data === '[DONE]') {
    streamStatus.value = 'complete'
    if (typewriterTimer) {
      clearInterval(typewriterTimer)
      typewriterTimer = null
    }
    if (typewriterQueue.length > 0) {
      displayText.value += typewriterQueue.join('')
      typewriterQueue = []
      totalChars.value = displayText.value.length
      autoScroll()
    }
    startCountdown(2)
    return
  }

  if (data === '[ERROR]') {
    streamStatus.value = 'error'
    if (typewriterTimer) {
      clearInterval(typewriterTimer)
      typewriterTimer = null
    }
    if (typewriterQueue.length > 0) {
      displayText.value += typewriterQueue.join('')
      typewriterQueue = []
    }
    errorMessage.value = errorMessage.value || 'AI服务调用失败'
    generating.value = false
    return
  }

  totalChars.value = displayText.value.length + typewriterQueue.length + data.length
  startTypewriter(data)
}

async function handleGenerate() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return

    generating.value = true
    streaming.value = true
    displayText.value = ''
    streamStatus.value = 'streaming'
    errorMessage.value = ''
    totalChars.value = 0
    aborted = false

    try {
      const payload = {
        planType: form.planType,
        durationDays: form.durationDays,
        intensity: form.intensity || undefined,
        tastePreference: form.tastePreference || undefined
      }

      const emitter = await generatePlanStream(payload)
      emitter.onMessage = handleIncomingMessage
      emitter.onError = (err) => {
        if (aborted) return
        streamStatus.value = 'error'
        errorMessage.value = '网络连接中断，请重试'
        generating.value = false
        if (typewriterTimer) {
          clearInterval(typewriterTimer)
          typewriterTimer = null
        }
      }
      await emitter
    } catch (err) {
      if (aborted) return
      streamStatus.value = 'error'
      errorMessage.value = '网络连接中断，请重试'
    }
    generating.value = false
  })
}

function resetForm() {
  aborted = true
  if (typewriterTimer) {
    clearInterval(typewriterTimer)
    typewriterTimer = null
  }
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
  typewriterQueue = []
  streaming.value = false
  streaming.value = false
  displayText.value = ''
  generating.value = false
  errorMessage.value = ''
  countdown.value = 0
  streamStatus.value = 'streaming'
  totalChars.value = 0
}

function goToPlanList() {
  clearInterval(countdownTimer)
  countdownTimer = null
  router.push('/plan/list')
}

onUnmounted(() => {
  aborted = true
  if (typewriterTimer) {
    clearInterval(typewriterTimer)
    typewriterTimer = null
  }
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
})
</script>

<style scoped lang="scss">
.generate-page {
  padding: 4px;
  position: relative;
}

.form-card {
  padding: 32px;
  max-width: 720px;
}

.limit-tip {
  margin-top: 16px;
  margin-bottom: 4px;
  background: rgba(210, 153, 34, 0.08);
  border: 1px solid rgba(210, 153, 34, 0.2);
  border-radius: 8px;

  :deep(.el-alert__icon) {
    color: #d29922;
  }
  :deep(.el-alert__title) {
    color: #d29922;
    font-size: 13px;
  }
}

.generate-form {
  margin-top: 24px;
}

:deep(.el-radio-group) {
  .el-radio {
    margin-right: 24px;
  }
}

.streaming-container {
  max-width: 900px;
  border: 1px solid #58a6ff;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  height: calc(100vh - 140px);
}

.streaming-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: rgba(22, 27, 34, 0.95);
  border-bottom: 1px solid #30363d;
}

.streaming-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.terminal-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
}

.terminal-label {
  font-size: 13px;
  color: #8b949e;
  margin-left: 8px;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
}

.streaming-header-right {
  display: flex;
  align-items: center;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  padding: 4px 12px;
  border-radius: 12px;
  font-weight: 500;

  &.streaming {
    background: rgba(88, 166, 255, 0.12);
    color: #58a6ff;
    border: 1px solid rgba(88, 166, 255, 0.25);
  }

  &.complete {
    background: rgba(63, 185, 80, 0.12);
    color: #3fb950;
    border: 1px solid rgba(63, 185, 80, 0.25);
  }

  &.error {
    background: rgba(248, 81, 73, 0.12);
    color: #f85149;
    border: 1px solid rgba(248, 81, 73, 0.25);
  }
}

.pulse-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #58a6ff;
  animation: pulse-dot 1.2s ease-in-out infinite;
}

@keyframes pulse-dot {
  0%, 100% { opacity: 0.3; }
  50% { opacity: 1; }
}

.streaming-body {
  flex: 1;
  padding: 20px 24px;
  overflow-y: auto;
  background: rgba(13, 17, 23, 0.95);
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.8;
  color: #c9d1d9;
  white-space: pre-wrap;
  word-break: break-word;

  &::-webkit-scrollbar {
    width: 6px;
  }
  &::-webkit-scrollbar-track {
    background: transparent;
  }
  &::-webkit-scrollbar-thumb {
    background: #30363d;
    border-radius: 3px;
  }
}

.streaming-text {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
}

.cursor-blink {
  color: #58a6ff;
  animation: blink 0.8s step-end infinite;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

.streaming-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 12px 16px;
  background: rgba(22, 27, 34, 0.95);
  border-top: 1px solid #30363d;
  min-height: 48px;
}

.bytes-info {
  font-size: 12px;
  color: #484f58;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
}

.error-alert {
  max-width: 360px;
  background: rgba(248, 81, 73, 0.08);
  border: 1px solid rgba(248, 81, 73, 0.2);
  border-radius: 8px;

  :deep(.el-alert__icon) {
    color: #f85149;
  }
  :deep(.el-alert__title) {
    color: #f85149;
    font-size: 13px;
  }
}
</style>
