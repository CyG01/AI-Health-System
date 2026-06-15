<template>
  <div class="goal-page">
    <div class="page-header">
      <h2 class="text-xl font-semibold">{{ $t('goal.milestones') || '目标里程碑' }}</h2>
      <NButton type="primary" @click="showDialog = true">
        <template #icon><NIcon><AddOutline /></NIcon></template>
        {{ $t('goal.create') || '新建目标' }}
      </NButton>
    </div>

    <!-- 目标总览 -->
    <NGrid v-if="goals.length > 0" :x-gap="16" :y-gap="16" :cols="4" class="mb-5" item-responsive responsive="screen">
      <NGi span="4 m:1">
        <NCard size="small" class="text-center">
          <NStatistic :label="$t('goal.total') || '总目标数'" :value="goals.length">
            <template #suffix>{{ $t('goal.unit') || '个' }}</template>
          </NStatistic>
        </NCard>
      </NGi>
      <NGi span="4 m:1">
        <NCard size="small" class="text-center">
          <NStatistic :label="$t('goal.active') || '进行中'" :value="activeGoals.length">
            <template #suffix> / {{ goals.length }}</template>
          </NStatistic>
        </NCard>
      </NGi>
      <NGi span="4 m:1">
        <NCard size="small" class="text-center">
          <NStatistic :label="$t('goal.completed') || '已完成'" :value="completedGoals.length">
            <template #suffix>{{ $t('goal.unit') || '个' }}</template>
          </NStatistic>
        </NCard>
      </NGi>
      <NGi span="4 m:1">
        <NCard size="small" class="text-center">
          <NStatistic :label="$t('goal.avgProgress') || '平均完成度'" :value="avgProgress">
            <template #suffix>%</template>
          </NStatistic>
        </NCard>
      </NGi>
    </NGrid>

    <!-- 进行中目标 -->
    <div v-if="activeGoals.length > 0" class="mb-6">
      <h3 class="section-title">{{ $t('goal.active') || '进行中' }}</h3>
      <NGrid :x-gap="16" :y-gap="16" :cols="3" item-responsive responsive="screen">
        <NGi v-for="goal in activeGoals" :key="goal.id" span="3 m:1">
          <NCard class="goal-card">
            <div class="goal-header">
              <NTag :type="goalTypeColor(goal.goalType)" size="small">{{ goal.goalTypeLabel }}</NTag>
              <NDropdown :options="goalActionOptions" @select="(cmd: string) => handleGoalAction(cmd, goal)" trigger="click">
                <NButton text>
                  <template #icon><NIcon><EllipsisVertical /></NIcon></template>
                </NButton>
              </NDropdown>
            </div>
            <h4 class="goal-name">{{ goal.goalName }}</h4>
            <NProgress
              type="line"
              :percentage="goal.progressPercent"
              :height="14"
              :color="progressColor(goal.progressPercent)"
              :show-indicator="false"
              :border-radius="4"
              class="mb-2"
            />
            <div class="goal-meta">
              <span class="text-[13px] text-secondary">{{ goal.currentValue }} / {{ goal.targetValue }} {{ goal.unit }}</span>
              <span v-if="goal.remainingDays !== null" class="text-[13px] text-secondary">
                {{ $t('goal.remaining') || '剩余' }} <b class="text-brand">{{ goal.remainingDays }}</b> {{ $t('goal.days') || '天' }}
              </span>
            </div>
          </NCard>
        </NGi>
      </NGrid>
    </div>

    <!-- 已完成目标 -->
    <div v-if="completedGoals.length > 0" class="mb-6">
      <h3 class="section-title">{{ $t('goal.completed') || '已完成' }}</h3>
      <NGrid :x-gap="16" :y-gap="16" :cols="3" item-responsive responsive="screen">
        <NGi v-for="goal in completedGoals" :key="goal.id" span="3 m:1">
          <NCard class="goal-card goal-card--completed">
            <div class="goal-header">
              <NTag :type="goalTypeColor(goal.goalType)" size="small">{{ goal.goalTypeLabel }}</NTag>
              <NIcon :size="24" color="#3fb950"><CheckmarkCircle /></NIcon>
            </div>
            <h4 class="goal-name">{{ goal.goalName }}</h4>
            <NProgress type="line" :percentage="100" :height="14" color="#3fb950" :show-indicator="false" :border-radius="4" class="mb-2" />
            <div class="goal-meta">
              <span class="text-[13px] text-secondary">{{ goal.targetValue }} {{ goal.unit }}</span>
              <span class="text-[13px] text-[#3fb950]">{{ goal.completedDate }} {{ $t('goal.achieved') || '达成' }}</span>
            </div>
          </NCard>
        </NGi>
      </NGrid>
    </div>

    <!-- 已放弃目标 -->
    <div v-if="abandonedGoals.length > 0" class="mb-6">
      <h3 class="section-title">{{ $t('goal.abandoned') || '已放弃' }}</h3>
      <NGrid :x-gap="16" :y-gap="16" :cols="3" item-responsive responsive="screen">
        <NGi v-for="goal in abandonedGoals" :key="goal.id" span="3 m:1">
          <NCard class="goal-card goal-card--abandoned">
            <div class="goal-header">
              <NTag type="info" size="small">{{ goal.goalTypeLabel }}</NTag>
              <NTag type="error" size="small">{{ $t('goal.abandoned') || '已放弃' }}</NTag>
            </div>
            <h4 class="goal-name">{{ goal.goalName }}</h4>
            <NProgress type="line" :percentage="goal.progressPercent" :height="14" color="#484f58" :show-indicator="false" :border-radius="4" class="mb-2" />
            <div class="goal-meta">
              <span class="text-[13px] text-secondary">{{ goal.currentValue }} / {{ goal.targetValue }} {{ goal.unit }}</span>
            </div>
          </NCard>
        </NGi>
      </NGrid>
    </div>

    <!-- 空状态 -->
    <NEmpty v-if="goals.length === 0 && !pageLoading" :description="$t('goal.empty') || '还没有设定目标，点击上方按钮创建第一个目标吧'" />

    <!-- 创建/编辑弹窗 -->
    <NModal
      v-model:show="showDialog"
      preset="card"
      :title="editingGoal ? ($t('goal.edit') || '编辑目标') : ($t('goal.create') || '新建目标')"
      style="width: 520px; max-width: 90vw"
      :mask-closable="true"
      @after-leave="resetForm"
    >
      <NForm ref="formRef" :model="form" :rules="formRules" label-placement="left" label-width="80px">
        <NFormItem :label="$t('goal.type') || '目标类型'" path="goalType">
          <NSelect v-model:value="form.goalType" :options="goalTypeOptions" class="w-full" />
        </NFormItem>
        <NFormItem :label="$t('goal.name') || '目标名称'" path="goalName">
          <NInput v-model:value="form.goalName" :placeholder="$t('goal.namePlaceholder') || '例如：减重5公斤'" :maxlength="50" />
        </NFormItem>
        <NFormItem :label="$t('goal.targetValue') || '目标值'" path="targetValue">
          <div class="flex items-center gap-2">
            <NInputNumber v-model:value="form.targetValue" :min="0.1" :precision="1" class="w-[150px]" />
            <NInput v-model:value="form.unit" :placeholder="$t('goal.unitPlaceholder') || '单位: kg/天/ml...'" class="w-[120px]" />
          </div>
        </NFormItem>
        <NFormItem :label="$t('goal.startDate') || '起始日期'">
          <NDatePicker v-model:formatted-value="form.startDate" type="date" value-format="yyyy-MM-dd" clearable class="w-full" />
        </NFormItem>
        <NFormItem :label="$t('goal.targetDate') || '目标日期'">
          <NDatePicker v-model:formatted-value="form.targetDate" type="date" value-format="yyyy-MM-dd" clearable class="w-full" />
        </NFormItem>
      </NForm>
      <template #footer>
        <div class="flex justify-end gap-2.5">
          <NButton @click="showDialog = false">{{ $t('common.cancel') || '取消' }}</NButton>
          <NButton type="primary" :loading="submitting" @click="handleSubmit">{{ $t('common.save') || '保存' }}</NButton>
        </div>
      </template>
    </NModal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import {
  NButton, NIcon, NCard, NTag, NProgress, NEmpty, NModal,
  NForm, NFormItem, NInput, NInputNumber, NSelect, NDatePicker,
  NGrid, NGi, NStatistic, NDropdown,
  useMessage, useDialog
} from 'naive-ui'
import type { FormInst, FormRules } from 'naive-ui'
import { AddOutline, EllipsisVertical, CheckmarkCircle } from '@vicons/ionicons5'
import { fetchCreateGoal, fetchUpdateGoal, fetchDeleteGoal, fetchGetGoalList, fetchUpdateGoalStatus } from '@/service/api'

defineOptions({ name: 'GoalMilestones' })
const message = useMessage()
const dialog = useDialog()

interface Goal {
  id: number | string
  goalType: string
  goalTypeLabel: string
  goalName: string
  targetValue: number
  currentValue: number
  unit: string
  progressPercent: number
  remainingDays: number | null
  status: number
  completedDate?: string
  startDate?: string
  targetDate?: string
}

const pageLoading = ref(false)
const submitting = ref(false)
const showDialog = ref(false)
const formRef = ref<FormInst | null>(null)
const goals = ref<Goal[]>([])
const editingGoal = ref<Goal | null>(null)

const form = reactive({
  goalType: '' as string,
  goalName: '',
  targetValue: null as number | null,
  unit: '',
  startDate: null as string | null,
  targetDate: null as string | null
})

const formRules: FormRules = {
  goalType: { type: 'string', required: true, message: '请选择目标类型', trigger: 'change' },
  goalName: { type: 'string', required: true, message: '请输入目标名称', trigger: 'blur' },
  targetValue: { type: 'number', required: true, message: '请输入目标值', trigger: 'blur' }
}

const goalTypeOptions = [
  { label: '减重', value: 'weight_loss' },
  { label: '增重', value: 'weight_gain' },
  { label: '增肌', value: 'muscle_gain' },
  { label: '运动天数', value: 'exercise_days' },
  { label: '连续打卡', value: 'checkin_days' },
  { label: '饮水目标', value: 'water_target' },
  { label: '自定义', value: 'custom' }
]

const goalActionOptions = [
  { label: '编辑', key: 'edit' },
  { label: '标记完成', key: 'complete' },
  { type: 'divider' as const, key: 'd1' },
  { label: '放弃目标', key: 'abandon' },
  { label: '删除', key: 'delete', props: { style: 'color: var(--color-danger, #f85149)' } }
]

const activeGoals = computed(() => goals.value.filter(g => g.status === 0))
const completedGoals = computed(() => goals.value.filter(g => g.status === 1))
const abandonedGoals = computed(() => goals.value.filter(g => g.status === 2))

const avgProgress = computed(() => {
  const active = activeGoals.value
  if (active.length === 0) return 0
  return Math.round(active.reduce((s, g) => s + (g.progressPercent || 0), 0) / active.length)
})

function goalTypeColor(type: string): 'default' | 'info' | 'success' | 'warning' | 'error' {
  const map: Record<string, 'default' | 'info' | 'success' | 'warning' | 'error'> = {
    weight_loss: 'error',
    weight_gain: 'warning',
    muscle_gain: 'success',
    exercise_days: 'default',
    checkin_days: 'info',
    water_target: 'default',
    custom: 'default'
  }
  return map[type] || 'info'
}

function progressColor(pct: number): string {
  if (pct >= 100) return '#3fb950'
  if (pct >= 50) return '#58a6ff'
  if (pct >= 25) return '#d29922'
  return '#f85149'
}

async function loadGoals() {
  pageLoading.value = true
  try {
    const { data } = await fetchGetGoalList()
    goals.value = (data as any) || []
  } finally {
    pageLoading.value = false
  }
}

function resetForm() {
  form.goalType = ''
  form.goalName = ''
  form.targetValue = null
  form.unit = ''
  form.startDate = null
  form.targetDate = null
  editingGoal.value = null
}

async function handleSubmit() {
  if (formRef.value) {
    try {
      await formRef.value.validate()
    } catch {
      return
    }
  }

  submitting.value = true
  try {
    if (editingGoal.value) {
      await fetchUpdateGoal({ ...form, id: editingGoal.value.id } as any)
      message.success('目标已更新')
    } else {
      await fetchCreateGoal({ ...form } as any)
      message.success('目标创建成功')
    }
    showDialog.value = false
    resetForm()
    await loadGoals()
  } finally {
    submitting.value = false
  }
}

function handleGoalAction(command: string, goal: Goal) {
  switch (command) {
    case 'edit':
      editingGoal.value = goal
      form.goalType = goal.goalType
      form.goalName = goal.goalName
      form.targetValue = goal.targetValue
      form.unit = goal.unit
      form.startDate = goal.startDate || null
      form.targetDate = goal.targetDate || null
      showDialog.value = true
      break
    case 'complete':
      dialog.success({
        title: '确认',
        content: `确认达成目标「${goal.goalName}」吗？`,
        positiveText: '确定',
        negativeText: '取消',
        onPositiveClick: async () => {
          await fetchUpdateGoalStatus(goal.id as number, '1')
          message.success('目标已达成！')
          await loadGoals()
        }
      })
      break
    case 'abandon':
      dialog.warning({
        title: '确认',
        content: `确认放弃目标「${goal.goalName}」吗？`,
        positiveText: '确定',
        negativeText: '取消',
        onPositiveClick: async () => {
          await fetchUpdateGoalStatus(goal.id as number, '2')
          message.success('已放弃目标')
          await loadGoals()
        }
      })
      break
    case 'delete':
      dialog.error({
        title: '警告',
        content: `确定删除目标「${goal.goalName}」吗？此操作不可恢复。`,
        positiveText: '确定删除',
        negativeText: '取消',
        onPositiveClick: async () => {
          await fetchDeleteGoal(goal.id as number)
          message.success('已删除')
          await loadGoals()
        }
      })
      break
  }
}

onMounted(() => {
  loadGoals()
})
</script>

<style scoped>
.goal-page { padding: 0 4px; }

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-title {
  margin: 0 0 16px;
  font-size: 17px;
  font-weight: 600;
  padding-left: 12px;
  border-left: 3px solid var(--brand-primary, #58a6ff);
}

.goal-card {
  transition: transform 0.2s;
  height: 100%;
}

.goal-card:hover {
  transform: translateY(-2px);
}

.goal-card--completed {
  border-left: 3px solid #3fb950;
}

.goal-card--abandoned {
  opacity: 0.6;
}

.goal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.goal-name {
  margin: 0 0 14px;
  font-size: 16px;
}

.goal-meta {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
}

.text-brand {
  color: var(--brand-primary, #58a6ff);
}

.text-secondary {
  color: #8b949e;
}
</style>
