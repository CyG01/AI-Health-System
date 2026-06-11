<template>
  <div class="goal-page" v-loading="pageLoading">
    <div class="page-header">
      <h2>目标里程碑</h2>
      <el-button type="primary" @click="showDialog = true">
        <el-icon><Plus /></el-icon> 新建目标
      </el-button>
    </div>

    <!-- 目标总览 -->
    <el-row :gutter="20" class="overview-row" v-if="goals.length > 0">
      <el-col :span="6">
        <el-card class="overview-card" shadow="hover">
          <el-statistic title="总目标数" :value="goals.length" suffix="个" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="overview-card" shadow="hover">
          <el-statistic title="进行中" :value="activeGoals.length" suffix="个">
            <template #suffix><span class="sub-text"> / {{ goals.length }}</span></template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="overview-card" shadow="hover">
          <el-statistic title="已完成" :value="completedGoals.length" suffix="个" />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="overview-card" shadow="hover">
          <el-statistic title="平均完成度" :value="avgProgress" suffix="%" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 进行中目标 -->
    <div class="section" v-if="activeGoals.length > 0">
      <h3 class="section-title">进行中</h3>
      <el-row :gutter="20">
        <el-col :span="8" v-for="goal in activeGoals" :key="goal.id">
          <el-card class="goal-card" shadow="hover" :body-style="{ padding: '20px' }">
            <div class="goal-header">
              <el-tag :type="goalTypeColor(goal.goalType)" size="small" effect="plain">
                {{ goal.goalTypeLabel }}
              </el-tag>
              <el-dropdown trigger="click" @command="(cmd) => handleGoalAction(cmd, goal)">
                <el-button text :icon="MoreFilled" />
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="edit">编辑</el-dropdown-item>
                    <el-dropdown-item command="complete">标记完成</el-dropdown-item>
                    <el-dropdown-item command="abandon" divided>放弃目标</el-dropdown-item>
                    <el-dropdown-item command="delete" style="color: #ff4d4f">删除</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
            <h4 class="goal-name">{{ goal.goalName }}</h4>
            <div class="goal-progress">
              <el-progress
                :percentage="goal.progressPercent"
                :stroke-width="14"
                :color="progressColor(goal.progressPercent)"
                :striped="true"
                :striped-flow="true"
              />
            </div>
            <div class="goal-meta">
              <span>{{ goal.currentValue }} / {{ goal.targetValue }} {{ goal.unit }}</span>
              <span v-if="goal.remainingDays !== null" class="remaining">
                剩余 <b>{{ goal.remainingDays }}</b> 天
              </span>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 已完成目标 -->
    <div class="section" v-if="completedGoals.length > 0">
      <h3 class="section-title">已完成</h3>
      <el-row :gutter="20">
        <el-col :span="8" v-for="goal in completedGoals" :key="goal.id">
          <el-card class="goal-card completed" shadow="hover" :body-style="{ padding: '20px' }">
            <div class="goal-header">
              <el-tag :type="goalTypeColor(goal.goalType)" size="small" effect="plain">
                {{ goal.goalTypeLabel }}
              </el-tag>
              <el-icon color="#52c41a" :size="24"><CircleCheckFilled /></el-icon>
            </div>
            <h4 class="goal-name">{{ goal.goalName }}</h4>
            <div class="goal-progress">
              <el-progress :percentage="100" :stroke-width="14" color="#52c41a" />
            </div>
            <div class="goal-meta">
              <span>{{ goal.targetValue }} {{ goal.unit }}</span>
              <span class="completed-date">{{ goal.completedDate }} 达成</span>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 已放弃目标 -->
    <div class="section" v-if="abandonedGoals.length > 0">
      <h3 class="section-title">已放弃</h3>
      <el-row :gutter="20">
        <el-col :span="8" v-for="goal in abandonedGoals" :key="goal.id">
          <el-card class="goal-card abandoned" shadow="hover" :body-style="{ padding: '20px' }">
            <div class="goal-header">
              <el-tag type="info" size="small" effect="plain">{{ goal.goalTypeLabel }}</el-tag>
              <el-tag type="danger" size="small" effect="plain">已放弃</el-tag>
            </div>
            <h4 class="goal-name">{{ goal.goalName }}</h4>
            <div class="goal-progress">
              <el-progress :percentage="goal.progressPercent" :stroke-width="14" color="#c0c4cc" />
            </div>
            <div class="goal-meta">
              <span>{{ goal.currentValue }} / {{ goal.targetValue }} {{ goal.unit }}</span>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 空状态 -->
    <el-empty v-if="goals.length === 0 && !pageLoading" description="还没有设定目标，点击上方按钮创建第一个目标吧" />

    <!-- 创建/编辑弹窗 -->
    <el-dialog
      v-model="showDialog"
      :title="editingGoal ? '编辑目标' : '新建目标'"
      width="520px"
      destroy-on-close
    >
      <el-form :model="form" label-width="80px" :rules="rules" ref="formRef">
        <el-form-item label="目标类型" prop="goalType">
          <el-select v-model="form.goalType" style="width:100%">
            <el-option label="减重" value="weight_loss" />
            <el-option label="增重" value="weight_gain" />
            <el-option label="增肌" value="muscle_gain" />
            <el-option label="运动天数" value="exercise_days" />
            <el-option label="连续打卡" value="checkin_days" />
            <el-option label="饮水目标" value="water_target" />
            <el-option label="自定义" value="custom" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标名称" prop="goalName">
          <el-input v-model="form.goalName" placeholder="例如：减重5公斤" maxlength="50" />
        </el-form-item>
        <el-form-item label="目标值" prop="targetValue">
          <el-input-number v-model="form.targetValue" :min="0.1" :precision="1" style="width:150px" />
          <el-input v-model="form.unit" placeholder="单位: kg/天/ml..." style="width:120px;margin-left:8px" />
        </el-form-item>
        <el-form-item label="起始日期">
          <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" placeholder="今天" style="width:100%" />
        </el-form-item>
        <el-form-item label="目标日期">
          <el-date-picker v-model="form.targetDate" type="date" value-format="YYYY-MM-DD" placeholder="可选" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleCheckFilled, Plus, MoreFilled } from '@element-plus/icons-vue'
import { createGoal, updateGoal, deleteGoal, getGoalList, updateGoalStatus } from '@/api/goal'

const pageLoading = ref(false)
const submitting = ref(false)
const showDialog = ref(false)
const formRef = ref(null)
const goals = ref([])
const editingGoal = ref(null)

const form = reactive({
  goalType: '',
  goalName: '',
  targetValue: null,
  unit: '',
  startDate: null,
  targetDate: null
})

const rules = {
  goalType: [{ required: true, message: '请选择目标类型', trigger: 'change' }],
  goalName: [{ required: true, message: '请输入目标名称', trigger: 'blur' }],
  targetValue: [{ required: true, message: '请输入目标值', trigger: 'blur' }]
}

const activeGoals = computed(() => goals.value.filter(g => g.status === 0))
const completedGoals = computed(() => goals.value.filter(g => g.status === 1))
const abandonedGoals = computed(() => goals.value.filter(g => g.status === 2))

const avgProgress = computed(() => {
  const active = activeGoals.value
  if (active.length === 0) return 0
  return Math.round(active.reduce((s, g) => s + (g.progressPercent || 0), 0) / active.length)
})

function goalTypeColor(type) {
  const map = {
    weight_loss: 'danger', weight_gain: 'warning', muscle_gain: 'success',
    exercise_days: '', checkin_days: 'info', water_target: '', custom: ''
  }
  return map[type] || 'info'
}

function progressColor(pct) {
  if (pct >= 100) return '#52c41a'
  if (pct >= 50) return '#1890ff'
  if (pct >= 25) return '#fa8c16'
  return '#ff4d4f'
}

async function loadGoals() {
  pageLoading.value = true
  try {
    const res = await getGoalList()
    goals.value = res.data || []
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
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (editingGoal.value) {
      await updateGoal({ ...form, id: editingGoal.value.id })
      ElMessage.success('目标已更新')
    } else {
      await createGoal({ ...form })
      ElMessage.success('目标创建成功')
    }
    showDialog.value = false
    resetForm()
    await loadGoals()
  } finally {
    submitting.value = false
  }
}

function handleGoalAction(command, goal) {
  switch (command) {
    case 'edit':
      editingGoal.value = goal
      form.goalType = goal.goalType
      form.goalName = goal.goalName
      form.targetValue = goal.targetValue
      form.unit = goal.unit
      form.startDate = goal.startDate
      form.targetDate = goal.targetDate
      showDialog.value = true
      break
    case 'complete':
      ElMessageBox.confirm(`确认达成目标「${goal.goalName}」吗？`, '确认', { type: 'success' })
        .then(async () => {
          await updateGoalStatus(goal.id, 1)
          ElMessage.success('目标已达成！')
          await loadGoals()
        }).catch(() => {})
      break
    case 'abandon':
      ElMessageBox.confirm(`确认放弃目标「${goal.goalName}」吗？`, '确认', { type: 'warning' })
        .then(async () => {
          await updateGoalStatus(goal.id, 2)
          ElMessage.success('已放弃目标')
          await loadGoals()
        }).catch(() => {})
      break
    case 'delete':
      ElMessageBox.confirm(`确定删除目标「${goal.goalName}」吗？此操作不可恢复。`, '警告', { type: 'error' })
        .then(async () => {
          await deleteGoal(goal.id)
          ElMessage.success('已删除')
          await loadGoals()
        }).catch(() => {})
      break
  }
}

onMounted(() => {
  loadGoals()
})
</script>

<style scoped lang="scss">
.goal-page { padding: 0 4px; }

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  h2 { margin: 0; font-size: 22px; color: var(--text-primary); }
}

.overview-row { margin-bottom: 24px; }

.section { margin-bottom: 24px; }

.section-title {
  margin: 0 0 16px;
  font-size: 17px;
  font-weight: 600;
  color: var(--text-primary);
  padding-left: 12px;
  border-left: 3px solid var(--brand-primary);
}

.goal-card {
  margin-bottom: 16px;
  transition: transform 0.2s;
  &:hover { transform: translateY(-2px); }
  &.completed { border-left: 3px solid #52c41a; }
  &.abandoned { opacity: 0.6; }
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
  color: var(--text-primary);
}

.goal-progress { margin-bottom: 10px; }

.goal-meta {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: var(--text-secondary);
  .remaining { b { color: var(--brand-primary); } }
  .completed-date { color: #52c41a; }
}

.sub-text { font-size: 13px; color: var(--text-secondary); }
</style>