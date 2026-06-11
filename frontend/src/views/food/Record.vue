<template>
  <div class="food-record-page">
    <div class="page-header">
      <h2>饮食记录</h2>
    </div>

    <!-- AI智能食物识别 -->
    <el-card class="ai-recognition-card" shadow="hover">
      <template #header>
        <span><el-icon><MagicStick /></el-icon> AI智能食物识别</span>
      </template>
      <div class="recognition-area">
        <el-upload
          class="recognition-upload"
          :auto-upload="false"
          :show-file-list="false"
          :on-change="handleImageUpload"
          accept="image/*"
          drag
        >
          <div v-if="!recognizing && !recognitionResult" class="upload-placeholder">
            <el-icon :size="40" color="#58a6ff"><Camera /></el-icon>
            <p>拍照或上传食物图片</p>
            <em>AI自动识别食物品种并估算热量</em>
          </div>
          <div v-else-if="recognizing" class="recognizing-status">
            <el-icon :size="32" class="is-loading" color="#58a6ff"><Loading /></el-icon>
            <p>AI正在识别中...</p>
          </div>
        </el-upload>

        <!-- 识别结果 -->
        <div v-if="recognitionResult" class="recognition-result">
          <div class="result-header">
            <h3>识别结果</h3>
            <el-button text size="small" @click="recognitionResult = null; uploadedImage = ''">清除</el-button>
          </div>
          <div class="result-foods">
            <div class="food-item">
              <span class="food-name">{{ recognitionResult.foodName }}</span>
              <el-tag size="small" type="success">~{{ recognitionResult.caloriePer100g }}kcal/100g</el-tag>
              <span class="food-confidence">置信度 {{ recognitionResult.confidence }}%</span>
              <el-button size="small" type="primary" @click="quickRecord(recognitionResult)">快速记录</el-button>
            </div>
          </div>
          <p v-if="recognitionResult.note" class="result-note">{{ recognitionResult.note }}</p>
        </div>
      </div>
    </el-card>

    <!-- 文字快捷录入 + 常用食物 -->
    <el-card class="quick-input-card" shadow="hover">
      <template #header><span>快捷录入</span></template>
      <div class="quick-input-area">
        <div class="text-input-row">
          <el-input
            v-model="quickText"
            placeholder="输入食物名称快速搜索，如：米饭、鸡蛋、牛奶..."
            clearable
            @keyup.enter="handleQuickTextSearch"
            style="flex:1"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-button type="primary" @click="handleQuickTextSearch" :loading="quickTextLoading" style="margin-left:8px">
            搜索
          </el-button>
        </div>
        <div v-if="frequentItems.length > 0" class="frequent-items">
          <span class="frequent-label">常用：</span>
          <el-button
            v-for="item in frequentItems"
            :key="item.id"
            size="small"
            type="default"
            @click="selectFrequentItem(item)"
            class="frequent-btn"
          >
            {{ item.name }}
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 提交饮食记录 -->
    <el-card class="submit-card" shadow="hover">
      <template #header><span>记录饮食</span></template>
      <el-form :model="form" label-width="80px" @submit.prevent="handleSubmit">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="食物">
              <el-select
                v-model="form.foodItemId"
                filterable
                placeholder="搜索食物名称"
                :loading="itemsLoading"
                style="width: 100%"
              >
                <el-option
                  v-for="item in foodItems"
                  :key="item.id"
                  :label="`${item.name} (${item.caloriesPerUnit}kcal/${item.unit})`"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item label="数量">
              <el-input-number v-model="form.amount" :min="0.1" :precision="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item label="单位">
              <el-input v-model="form.unit" placeholder="份/碗/个" />
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item label="餐次">
              <el-select v-model="form.mealType" style="width: 100%">
                <el-option label="早餐" value="breakfast" />
                <el-option label="午餐" value="lunch" />
                <el-option label="晚餐" value="dinner" />
                <el-option label="加餐" value="snack" />
              </el-select>
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
        <el-statistic title="今日总摄入" :value="todayTotal" suffix="kcal" />
      </el-col>
      <el-col :span="6">
        <el-statistic title="今日记录条数" :value="records.length" suffix="条" />
      </el-col>
    </el-row>

    <!-- 记录列表 -->
    <el-card class="list-card" shadow="hover">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>饮食记录</span>
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
        <el-table-column prop="foodItemName" label="食物" min-width="120" />
        <el-table-column label="餐次" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.mealType === 'breakfast'">早餐</el-tag>
            <el-tag v-else-if="row.mealType === 'lunch'" type="success">午餐</el-tag>
            <el-tag v-else-if="row.mealType === 'dinner'" type="warning">晚餐</el-tag>
            <el-tag v-else type="info">加餐</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="份量" width="100">
          <template #default="{ row }">{{ row.amount }} {{ row.unit }}</template>
        </el-table-column>
        <el-table-column label="热量" width="100">
          <template #default="{ row }"><b>{{ row.calories }}</b> kcal</template>
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
import { getFoodItems, getFrequentItems, parseFoodText, submitFoodRecord, getFoodRecordsPage } from '@/api/food'
import { recognizeFood } from '@/api/food'

const foodItems = ref([])
const frequentItems = ref([])
const itemsLoading = ref(false)
const records = ref([])
const recordsLoading = ref(false)
const submitting = ref(false)
const recognizing = ref(false)
const recognitionResult = ref(null)
const uploadedImage = ref('')
const page = ref(1)
const total = ref(0)
const quickText = ref('')
const quickTextLoading = ref(false)

const form = ref({
  foodItemId: null,
  amount: 1,
  unit: '份',
  mealType: 'lunch'
})

const todayTotal = computed(() =>
  records.value.reduce((sum, r) => sum + (r.calories || 0), 0)
)

function formatTime(t) {
  if (!t) return '-'
  return t.substring(11, 16)
}

async function handleImageUpload(file) {
  const isImage = file.raw.type.startsWith('image/')
  if (!isImage) {
    ElMessage.warning('请上传图片文件')
    return
  }
  if (file.raw.size > 10 * 1024 * 1024) {
    ElMessage.warning('图片不能超过10MB')
    return
  }

  recognizing.value = true
  recognitionResult.value = null

  // 使用 FormData 上传原始文件
  const formData = new FormData()
  formData.append('image', file.raw)

  try {
    const res = await recognizeFood(formData)
    // Result<FoodRecognizeVO> 解析: res.data 是后端返回的 Result 对象
    recognitionResult.value = res.data || res
  } catch {
    // handled by interceptor
  } finally {
    recognizing.value = false
  }
}

/** 文字快捷搜索食物 */
async function handleQuickTextSearch() {
  const text = quickText.value?.trim()
  if (!text) return
  quickTextLoading.value = true
  try {
    const res = await parseFoodText({ text })
    if (res.data) {
      const food = res.data
      form.value.foodItemId = food.id
      form.value.amount = 1
      form.value.unit = '份'
      quickText.value = ''
      ElMessage.success(`已匹配：${food.name}`)
    }
  } catch {
    ElMessage.info('未匹配到对应食物，请在下方下拉列表中选择')
  } finally {
    quickTextLoading.value = false
  }
}

/** 选择常用食物 */
function selectFrequentItem(item) {
  form.value.foodItemId = item.id
  form.value.amount = 1
  form.value.unit = '份'
  ElMessage.success(`已选择：${item.name}`)
}

function quickRecord(food) {
  // food 是 FoodRecognizeVO: { foodName, caloriePer100g, confidence, recommendedGrams, category, ... }
  const foodName = food.foodName || ''
  // 尝试匹配已有食物项
  const matched = foodItems.value.find(
    f => f.name.toLowerCase().includes(foodName.toLowerCase()) ||
         foodName.toLowerCase().includes(f.name.toLowerCase())
  )
  if (matched) {
    form.value.foodItemId = matched.id
    form.value.amount = food.recommendedGrams || 1
    form.value.unit = matched.unit || '份'
    ElMessage.success(`已匹配到 ${matched.name}`)
  } else {
    form.value.foodItemId = null
    form.value.amount = food.recommendedGrams || 1
    form.value.unit = '份'
    ElMessage.info(`未在食物库找到 ${foodName}，请手动选择`)
  }
  // 滚动到表单
  window.scrollTo({ top: 200, behavior: 'smooth' })
}

async function loadItems() {
  itemsLoading.value = true
  try {
    const res = await getFoodItems()
    foodItems.value = res.data || []
  } finally {
    itemsLoading.value = false
  }
}

async function loadRecords() {
  recordsLoading.value = true
  try {
    const res = await getFoodRecordsPage({ page: page.value, size: 10 })
    if (res.data) {
      records.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } finally {
    recordsLoading.value = false
  }
}

async function handleSubmit() {
  if (!form.value.foodItemId) {
    ElMessage.warning('请选择食物')
    return
  }
  submitting.value = true
  try {
    const res = await submitFoodRecord(form.value)
    if (res.code === 200) {
      ElMessage.success('记录成功')
      form.value.amount = 1
      form.value.foodItemId = null
      loadRecords()
    }
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadItems()
  loadRecords()
  loadFrequentItems()
})

/** 加载常用食物 */
async function loadFrequentItems() {
  try {
    const res = await getFrequentItems({ limit: 8 })
    if (res.data) {
      frequentItems.value = res.data
    }
  } catch {
    // 静默处理
  }
}
</script>

<style scoped>
.food-record-page { padding: 20px 0; }
.page-header { margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 20px; font-weight: 600; }
.submit-card { margin-bottom: 20px; }
.stats-row { margin-bottom: 20px; }
.list-card { margin-bottom: 20px; }

/* AI食物识别 */
.ai-recognition-card {
  margin-bottom: 20px;
  :deep(.el-card__header) {
    border-color: rgba(88, 166, 255, 0.2);
    color: #e6edf3;
    display: flex;
    align-items: center;
    gap: 6px;
  }
}

.recognition-area { padding: 8px 0; }

.recognition-upload {
  width: 100%;
  :deep(.el-upload-dragger) {
    background: #0d1117;
    border-color: #30363d;
    border-style: dashed;
  }
  :deep(.el-upload-dragger:hover) {
    border-color: #58a6ff;
  }
}

.upload-placeholder, .recognizing-status {
  padding: 40px 20px;
  text-align: center;
}

.upload-placeholder p {
  color: #e6edf3;
  margin: 10px 0 4px;
  font-size: 15px;
}

.upload-placeholder em {
  color: #8b949e;
  font-size: 12px;
}

.recognizing-status p {
  color: #58a6ff;
  margin-top: 10px;
}

.recognition-result {
  margin-top: 16px;
  padding: 16px;
  background: rgba(88, 166, 255, 0.06);
  border-radius: 8px;
  border: 1px solid rgba(88, 166, 255, 0.15);
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.result-header h3 {
  margin: 0;
  font-size: 15px;
  color: #58a6ff;
}

.result-foods {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.food-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: #0d1117;
  border-radius: 8px;
  border: 1px solid #21262d;
}

.food-name {
  color: #e6edf3;
  font-weight: 500;
  flex: 1;
}

.food-confidence {
  color: #8b949e;
  font-size: 12px;
}

.result-note {
  color: #8b949e;
  font-size: 13px;
  margin: 10px 0 0;
  line-height: 1.6;
}

/* 快捷录入 */
.quick-input-card {
  margin-bottom: 20px;
  :deep(.el-card__header) {
    border-color: rgba(82, 196, 26, 0.2);
    color: #e6edf3;
  }
}

.quick-input-area {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.text-input-row {
  display: flex;
  align-items: center;
}

.frequent-items {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.frequent-label {
  color: #8b949e;
  font-size: 13px;
  white-space: nowrap;
}

.frequent-btn {
  border-color: #30363d;
  background: #0d1117;
  color: #c9d1d9;
}

.frequent-btn:hover {
  border-color: #58a6ff;
  color: #58a6ff;
}
</style>