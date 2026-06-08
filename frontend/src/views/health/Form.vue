<template>
  <div class="health-form-page" v-loading="pageLoading">
    <div class="form-card glass-card">
      <h2 class="page-title">{{ isEdit ? '修改健康档案' : '创建健康档案' }}</h2>
      <p class="page-desc">填写您的身体指标与健康状况，我们为您计算 BMI 与基础代谢率</p>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" class="health-form">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="身高(cm)" prop="height">
              <el-input-number v-model="form.height" :min="100" :max="250" :precision="1" :step="0.5" placeholder="请输入身高" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="体重(kg)" prop="weight">
              <el-input-number v-model="form.weight" :min="30" :max="300" :precision="1" :step="0.5" placeholder="请输入体重" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="健康目标" prop="goal">
          <el-input v-model="form.goal" type="textarea" :rows="2" placeholder="例如：减重5kg、每周运动3次" maxlength="200" show-word-limit />
        </el-form-item>

        <el-form-item label="既往病史" prop="diseaseHistory">
          <el-input v-model="form.diseaseHistory" type="textarea" :rows="2" placeholder="例如：高血压、糖尿病等" maxlength="500" show-word-limit />
        </el-form-item>

        <el-form-item label="过敏史" prop="allergyHistory">
          <el-input v-model="form.allergyHistory" type="textarea" :rows="2" placeholder="例如：青霉素过敏、花粉过敏等" maxlength="500" show-word-limit />
        </el-form-item>

        <el-form-item label="运动习惯" prop="exerciseHabit">
          <el-input v-model="form.exerciseHabit" type="textarea" :rows="2" placeholder="例如：每周慢跑3次、每天步行30分钟" maxlength="500" show-word-limit />
        </el-form-item>

        <el-form-item label="饮食习惯" prop="dietHabit">
          <el-input v-model="form.dietHabit" type="textarea" :rows="2" placeholder="例如：偏好清淡、不挑食、每天三餐规律" maxlength="500" show-word-limit />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="saveLoading" :disabled="saveLoading" @click="handleSave">
            {{ isEdit ? '更新档案' : '创建档案' }}
          </el-button>
          <el-button :disabled="saveLoading" @click="handleCancel">取消</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createHealth, updateHealth, getLatestHealth } from '@/api/health'

const router = useRouter()
const formRef = ref(null)
const pageLoading = ref(false)
const saveLoading = ref(false)
const isEdit = ref(false)

const form = reactive({
  height: null,
  weight: null,
  goal: '',
  diseaseHistory: '',
  allergyHistory: '',
  exerciseHabit: '',
  dietHabit: ''
})

const rules = {
  height: [
    { required: true, message: '请输入身高', trigger: 'blur' },
    { type: 'number', min: 100, max: 250, message: '身高必须在100-250cm之间', trigger: 'blur' }
  ],
  weight: [
    { required: true, message: '请输入体重', trigger: 'blur' },
    { type: 'number', min: 30, max: 300, message: '体重必须在30-300kg之间', trigger: 'blur' }
  ],
  goal: [
    { required: true, message: '请输入健康目标', trigger: 'blur' },
    { max: 200, message: '健康目标不能超过200个字符', trigger: 'blur' }
  ],
  diseaseHistory: [{ max: 500, message: '既往病史不能超过500个字符', trigger: 'blur' }],
  allergyHistory: [{ max: 500, message: '过敏史不能超过500个字符', trigger: 'blur' }],
  exerciseHabit: [{ max: 500, message: '运动习惯不能超过500个字符', trigger: 'blur' }],
  dietHabit: [{ max: 500, message: '饮食习惯不能超过500个字符', trigger: 'blur' }]
}

async function tryLoadLatest() {
  try {
    const res = await getLatestHealth()
    const data = res.data
    form.height = data.height
    form.weight = data.weight
    form.goal = data.goal || ''
    form.diseaseHistory = data.diseaseHistory || ''
    form.allergyHistory = data.allergyHistory || ''
    form.exerciseHabit = data.exerciseHabit || ''
    form.dietHabit = data.dietHabit || ''
    isEdit.value = true
  } catch {
    isEdit.value = false
  }
}

onMounted(async () => {
  pageLoading.value = true
  try {
    await tryLoadLatest()
  } finally {
    pageLoading.value = false
  }
})

async function handleSave() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    saveLoading.value = true
    try {
      const payload = {
        height: form.height,
        weight: form.weight,
        goal: form.goal,
        diseaseHistory: form.diseaseHistory,
        allergyHistory: form.allergyHistory,
        exerciseHabit: form.exerciseHabit,
        dietHabit: form.dietHabit
      }
      if (isEdit.value) {
        await updateHealth(payload)
        ElMessage.success('健康档案已更新')
      } else {
        await createHealth(payload)
        ElMessage.success('健康档案已创建')
      }
      router.push('/health/view')
    } finally {
      saveLoading.value = false
    }
  })
}

function handleCancel() {
  router.back()
}
</script>

<style scoped lang="scss">
.health-form-page {
  padding: 4px;
}

.form-card {
  padding: 32px;
  max-width: 860px;
}

.health-form {
  margin-top: 24px;

  :deep(.el-input-number) {
    width: 100%;
  }
}

:deep(.el-divider) {
  border-color: transparent;
  margin: 28px 0;
}
</style>
