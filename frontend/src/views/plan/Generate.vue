<template>
  <div class="generate-page">
    <div class="form-card glass-card">
      <h2 class="page-title">AI 智能计划生成</h2>
      <p class="page-desc">基于您的健康档案，由 DeepSeek 为您量身定制个性化运动/饮食计划</p>
      <el-alert title="每日最多生成3次计划" type="info" :closable="false" show-icon class="limit-tip" />

      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" class="generate-form">
        <el-form-item label="计划类型" prop="planType">
          <el-radio-group v-model="form.planType">
            <el-radio value="sport">运动计划</el-radio>
            <el-radio value="diet">饮食计划</el-radio>
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
            {{ generating ? 'AI 生成中...' : '开始生成' }}
          </el-button>
          <el-button :disabled="generating" @click="$router.push('/plan/list')">返回列表</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div v-if="generating" class="skeleton-overlay">
      <div class="skeleton-card glass-card">
        <div class="skeleton-header">
          <el-icon :size="24" color="#58a6ff" class="pulse-icon"><Cpu /></el-icon>
          <span class="skeleton-title">AI 正在为您生成个性化计划...</span>
        </div>
        <div class="skeleton-steps">
          <div class="skeleton-step" v-for="step in steps" :key="step.idx">
            <el-icon :color="step.done ? '#3fb950' : '#30363d'" :size="18">
              <CircleCheck v-if="step.done" /><Clock v-else />
            </el-icon>
            <span :class="['step-text', { done: step.done }]">{{ step.label }}</span>
          </div>
        </div>
        <div class="skeleton-progress">
          <div class="progress-bar" :style="{ width: progressPercent + '%' }"></div>
        </div>
        <p class="skeleton-hint">生成过程约需 10-30 秒，请耐心等待...</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Cpu, Clock, CircleCheck } from '@element-plus/icons-vue'
import { generatePlan } from '@/api/aiPlan'

const router = useRouter()
const formRef = ref(null)
const generating = ref(false)
const progressPercent = ref(0)

let progressTimer = null
let stepTimers = []

const form = reactive({
  planType: 'sport',
  durationDays: 7,
  intensity: '',
  tastePreference: ''
})

const rules = {
  planType: [{ required: true, message: '请选择计划类型', trigger: 'change' }],
  durationDays: [{ required: true, message: '请选择计划天数', trigger: 'change' }]
}

const steps = reactive([
  { idx: 1, label: '分析健康档案', done: false },
  { idx: 2, label: '匹配最优模型', done: false },
  { idx: 3, label: '生成计划内容', done: false },
  { idx: 4, label: '保存入库', done: false }
])

function startSkeletonAnimation() {
  progressPercent.value = 0
  steps.forEach(s => (s.done = false))

  let stepIdx = 0
  function nextStep() {
    if (stepIdx < steps.length) {
      steps[stepIdx].done = true
      stepIdx++
      const delay = 2000 + Math.random() * 3000
      const timer = setTimeout(nextStep, delay)
      stepTimers.push(timer)
    }
  }
  nextStep()

  const totalTime = 25000
  const interval = 300
  const increment = (100 / (totalTime / interval))
  progressTimer = setInterval(() => {
    progressPercent.value = Math.min(progressPercent.value + increment, 95)
  }, interval)
}

function clearSkeletonTimers() {
  if (progressTimer) {
    clearInterval(progressTimer)
    progressTimer = null
  }
  stepTimers.forEach(t => clearTimeout(t))
  stepTimers = []
  progressPercent.value = 100
  steps.forEach(s => (s.done = true))
}

async function handleGenerate() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return

    generating.value = true
    startSkeletonAnimation()

    try {
      const payload = {
        planType: form.planType,
        durationDays: form.durationDays,
        intensity: form.intensity || undefined,
        tastePreference: form.tastePreference || undefined
      }
      await generatePlan(payload)
      ElMessage.success('AI 计划生成成功')
      router.push('/plan/list')
    } finally {
      clearSkeletonTimers()
      generating.value = false
    }
  })
}

onUnmounted(() => {
  clearSkeletonTimers()
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

.skeleton-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(13, 17, 23, 0.85);
  backdrop-filter: blur(8px);
}

.skeleton-card {
  width: 480px;
  padding: 40px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
}

.skeleton-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.pulse-icon {
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 0.4; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.15); }
}

.skeleton-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.skeleton-steps {
  display: flex;
  flex-direction: column;
  gap: 14px;
  width: 100%;
  padding: 0 20px;
}

.skeleton-step {
  display: flex;
  align-items: center;
  gap: 12px;
}

.step-text {
  font-size: 14px;
  color: #484f58;
  transition: color 0.3s ease;

  &.done {
    color: var(--text-primary);
  }
}

.skeleton-progress {
  width: 100%;
  height: 4px;
  background: rgba(48, 54, 61, 0.6);
  border-radius: 2px;
  overflow: hidden;
}

.progress-bar {
  height: 100%;
  background: linear-gradient(90deg, #58a6ff, #3fb950);
  border-radius: 2px;
  transition: width 0.3s ease;
}

.skeleton-hint {
  font-size: 12px;
  color: #484f58;
}
</style>
