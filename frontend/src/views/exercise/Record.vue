<template>
  <div class="exercise-record-page">
    <div class="page-header">
      <h2>运动记录</h2>
    </div>

    <!-- 提交运动记录 -->
    <el-card class="submit-card" shadow="hover">
      <template #header><span>记录运动</span></template>
      <el-form :model="form" label-width="80px" @submit.prevent="handleSubmit">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="运动项目">
              <el-select
                v-model="form.exerciseItemId"
                filterable
                placeholder="搜索运动项目"
                :loading="itemsLoading"
                @change="onItemChange"
                style="width: 100%"
              >
                <el-option
                  v-for="item in exerciseItems"
                  :key="item.id"
                  :label="`${item.name} (${item.caloriesPerUnit}kcal/${item.unit})`"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item label="时长(分)">
              <el-input-number v-model="form.duration" :min="1" :max="480" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item label="数量">
              <el-input-number v-model="form.amount" :min="0.1" :precision="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item label="单位">
              <el-input v-model="form.unit" placeholder="次/组/分钟" />
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item>
              <el-button type="primary" native-type="submit" :loading="submitting">提交记录</el-button>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <!-- 今日统计 -->
    <el-row :gutter="20" class="stats-row" v-if="todayTotal > 0">
      <el-col :span="6">
        <el-statistic title="今日总消耗" :value="todayTotal" suffix="kcal" />
      </el-col>
      <el-col :span="6">
        <el-statistic title="今日运动次数" :value="records.length" suffix="次" />
      </el-col>
    </el-row>

    <!-- AI运动指导 -->
    <el-card v-if="selectedItem" class="guidance-card" shadow="hover">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span><el-icon><MagicStick /></el-icon> AI运动指导 - {{ selectedItem.name }}</span>
          <el-button size="small" type="primary" :loading="guidanceLoading" @click="handleGetGuidance">
            {{ guidance ? '刷新指导' : '获取AI指导' }}
          </el-button>
        </div>
      </template>
      <div v-if="guidance" class="guidance-content">
        <div class="guidance-section" v-if="guidance.basicInfo">
          <h4>基本信息</h4>
          <el-row :gutter="16">
            <el-col :span="6"><span class="label">类型</span>：{{ guidance.basicInfo.type }}</el-col>
            <el-col :span="6"><span class="label">目标肌群</span>：{{ guidance.basicInfo.targetMuscle }}</el-col>
            <el-col :span="6"><span class="label">难度</span>：<el-tag size="small">{{ guidance.basicInfo.difficulty }}</el-tag></el-col>
            <el-col :span="6"><span class="label">热量消耗</span>：~{{ selectedItem.caloriesPerUnit }}kcal/{{ selectedItem.unit }}</el-col>
          </el-row>
        </div>
        <div class="guidance-section" v-if="guidance.breathing">
          <h4>呼吸节奏</h4>
          <p>{{ guidance.breathing }}</p>
        </div>
        <div class="guidance-section" v-if="guidance.steps?.length">
          <h4>动作要领</h4>
          <ul class="step-list">
            <li v-for="(step, i) in guidance.steps" :key="i">
              <span class="step-num">{{ i + 1 }}</span> {{ step }}
            </li>
          </ul>
        </div>
        <div class="guidance-section" v-if="guidance.commonMistakes?.length">
          <h4>常见错误</h4>
          <ul class="mistake-list">
            <li v-for="(m, i) in guidance.commonMistakes" :key="i">
              <el-tag type="danger" size="small" effect="dark">错误{{ i + 1 }}</el-tag> {{ m }}
            </li>
          </ul>
        </div>
        <div class="guidance-section" v-if="guidance.tips">
          <h4>小贴士</h4>
          <p>{{ guidance.tips }}</p>
        </div>
      </div>
      <el-empty v-else description="点击按钮获取AI运动指导" :image-size="50" />
    </el-card>

    <!-- 记录列表 -->
    <el-card class="list-card" shadow="hover">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>运动记录</span>
          <el-pagination
            v-model:current-page="page"
            small
            layout="prev, pager, next"
            :total="total"
            :page-size="10"
            @current-change="loadRecords"
          />
        </div>
      </template>
      <el-table :data="records" stripe v-loading="recordsLoading">
        <el-table-column prop="exerciseItemName" label="运动项目" min-width="120" />
        <el-table-column label="运动类型" width="100">
          <template #default="{ row }">
            <el-tag>{{ row.exerciseType || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="duration" label="时长(分钟)" width="100" />
        <el-table-column label="运动量" width="100">
          <template #default="{ row }">{{ row.amount }} {{ row.unit }}</template>
        </el-table-column>
        <el-table-column label="消耗热量" width="100">
          <template #default="{ row }"><b>{{ row.caloriesBurned }}</b> kcal</template>
        </el-table-column>
        <el-table-column label="时间" width="100">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getExerciseItems, submitExerciseRecord, getExerciseRecordsPage } from '@/api/exercise'
import { getExerciseGuidance } from '@/api/exercise'

const exerciseItems = ref([])
const itemsLoading = ref(false)
const records = ref([])
const recordsLoading = ref(false)
const submitting = ref(false)
const guidanceLoading = ref(false)
const guidance = ref(null)
const selectedItem = ref(null)
const page = ref(1)
const total = ref(0)

const form = ref({
  exerciseItemId: null,
  duration: 30,
  amount: 1,
  unit: '次'
})

const todayTotal = computed(() =>
  records.value.reduce((sum, r) => sum + (r.caloriesBurned || 0), 0)
)

function formatTime(t) {
  if (!t) return '-'
  return t.substring(11, 16)
}

async function loadItems() {
  itemsLoading.value = true
  try {
    const res = await getExerciseItems()
    exerciseItems.value = res.data || []
  } finally {
    itemsLoading.value = false
  }
}

function onItemChange(val) {
  selectedItem.value = exerciseItems.value.find(i => i.id === val) || null
  guidance.value = null
}

async function handleGetGuidance() {
  if (!selectedItem.value) return
  guidanceLoading.value = true
  try {
    const res = await getExerciseGuidance(selectedItem.value.id)
    if (res.data?.data) {
      guidance.value = res.data.data
    } else {
      guidance.value = res.data
    }
  } finally {
    guidanceLoading.value = false
  }
}

async function loadRecords() {
  recordsLoading.value = true
  try {
    const res = await getExerciseRecordsPage({ page: page.value, size: 10 })
    if (res.data) {
      records.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } finally {
    recordsLoading.value = false
  }
}

async function handleSubmit() {
  if (!form.value.exerciseItemId) {
    ElMessage.warning('请选择运动项目')
    return
  }
  submitting.value = true
  try {
    const res = await submitExerciseRecord(form.value)
    if (res.code === 200) {
      ElMessage.success('记录成功')
      form.value.duration = 30
      form.value.amount = 1
      form.value.exerciseItemId = null
      loadRecords()
    }
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadItems()
  loadRecords()
})
</script>

<style scoped>
.exercise-record-page { padding: 20px 0; }
.page-header { margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 20px; font-weight: 600; }
.submit-card { margin-bottom: 20px; }
.stats-row { margin-bottom: 20px; }
.list-card { margin-bottom: 20px; }

.guidance-card {
  margin-bottom: 20px;
  :deep(.el-card__header) { border-color: rgba(88, 166, 255, 0.2); }
}

.guidance-content { padding: 8px 0; }

.guidance-section {
  margin-bottom: 16px;
}

.guidance-section h4 {
  font-size: 14px;
  color: #58a6ff;
  margin-bottom: 8px;
  padding-bottom: 4px;
  border-bottom: 1px solid #21262d;
}

.guidance-section p {
  color: #c9d1d9;
  font-size: 13px;
  line-height: 1.8;
  margin: 0;
}

.guidance-section .label { color: #8b949e; }

.step-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.step-list li {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  color: #c9d1d9;
  font-size: 13px;
  line-height: 1.6;
}

.step-num {
  width: 22px;
  height: 22px;
  background: linear-gradient(135deg, #58a6ff, #7c3aed);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  flex-shrink: 0;
}

.mistake-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.mistake-list li {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  color: #c9d1d9;
  font-size: 13px;
  line-height: 1.6;
}
</style>